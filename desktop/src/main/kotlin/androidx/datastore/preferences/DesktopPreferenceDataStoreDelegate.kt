package androidx.datastore.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/*
 * Desktop port of the Android-only `preferencesDataStore` property delegate and
 * `preferencesDataStoreFile` (androidx.datastore.preferences, Android artifact).
 * Same signatures, same file location relative to filesDir, same singleton
 * semantics, implemented on the multiplatform PreferenceDataStoreFactory.
 */

fun preferencesDataStore(
    name: String,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { listOf() },
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): ReadOnlyProperty<Context, DataStore<Preferences>> =
    PreferenceDataStoreSingletonDelegate(name, corruptionHandler, produceMigrations, scope)

fun Context.preferencesDataStoreFile(name: String): File =
    File(applicationContext.filesDir, "datastore/$name.preferences_pb")

internal class PreferenceDataStoreSingletonDelegate(
    private val name: String,
    private val corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    private val produceMigrations: (Context) -> List<DataMigration<Preferences>>,
    private val scope: CoroutineScope,
) : ReadOnlyProperty<Context, DataStore<Preferences>> {

    private val lock = Any()

    @Volatile
    private var instance: DataStore<Preferences>? = null

    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<Preferences> =
        instance ?: synchronized(lock) {
            instance ?: run {
                val app = thisRef.applicationContext
                PreferenceDataStoreFactory.create(
                    corruptionHandler = corruptionHandler,
                    migrations = produceMigrations(app),
                    scope = scope,
                ) { app.preferencesDataStoreFile(name) }
            }.also { instance = it }
        }
}
