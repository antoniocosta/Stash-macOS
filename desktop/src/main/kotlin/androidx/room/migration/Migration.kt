package androidx.room.migration

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * THE ONE SHADOWED LIBRARY CLASS (approved; see desktop/PORTING.md).
 *
 * room-runtime-jvm's Migration only has migrate(SQLiteConnection); upstream's
 * 60+ migrations override the Android signature migrate(SupportSQLiteDatabase).
 * This class is a binary-compatible SUPERSET of the JVM class (same
 * constructor, same startVersion/endVersion getters, same
 * migrate(SQLiteConnection)) plus the Android overload. The original
 * Migration.class is stripped from the room-runtime-jvm jar by an artifact
 * transform in desktop/build.gradle.kts so exactly one definition exists.
 *
 * Mirrors Android Room 2.7's own bridge: the connection-based entry point
 * delegates to the SupportSQLiteDatabase overload.
 */
abstract class Migration(
    val startVersion: Int,
    val endVersion: Int,
) {
    open fun migrate(db: SupportSQLiteDatabase) {
        throw NotImplementedError(
            "Migration($startVersion -> $endVersion) must override migrate(SQLiteConnection) " +
                "or migrate(SupportSQLiteDatabase)",
        )
    }

    open fun migrate(connection: SQLiteConnection) {
        migrate(SQLiteDatabase(connection, path = null, ownsConnection = false))
    }
}
