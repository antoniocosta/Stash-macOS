package android.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.File

/**
 * Desktop shim for android.content.Context. Only the members upstream actually
 * touches are provided; paths map to standard macOS locations:
 *   filesDir / noBackupFilesDir / databases -> ~/Library/Application Support/Stash
 *   cacheDir                                -> ~/Library/Caches/Stash
 * System services come from [DesktopSystemServices]; activities/broadcasts are
 * dispatched in-process by [DesktopIntents].
 */
open class Context protected constructor() : coil3.PlatformContext() {

    open val applicationContext: Context get() = this

    open val packageName: String get() = PACKAGE_NAME

    open val filesDir: File get() = dir(appSupport, "files")
    open val noBackupFilesDir: File get() = dir(appSupport, "no_backup")
    open val cacheDir: File get() = dir(cacheRoot)

    open fun getDatabasePath(name: String): File = File(dir(appSupport, "databases"), name)

    open fun getDir(name: String, mode: Int): File = dir(appSupport, "app_$name")

    /** Stateless file-backed resolver (grants persist on disk), so a fresh instance per call is equivalent. */
    open val contentResolver: ContentResolver get() = ContentResolver(this)

    // ── System / notification scope ────────────────────────────────────────

    open val packageManager: PackageManager get() = PackageManager.INSTANCE

    open val applicationInfo: ApplicationInfo get() = ApplicationInfo.forApp(this)

    open fun getSystemService(name: String): Any? = DesktopSystemServices.get(name)

    @Suppress("UNCHECKED_CAST")
    fun <T> getSystemService(serviceClass: Class<T>): T = DesktopSystemServices.get(serviceClass) as T

    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        DesktopSharedPreferences.open(File(dir(appSupport, "shared_prefs"), "$name.xml"))

    open fun startActivity(intent: Intent) = DesktopIntents.startActivity(this, intent)

    /** Desktop services are in-process objects (see DesktopMediaServiceRegistry); nothing to start by Intent. */
    open fun startService(service: Intent): ComponentName? = service.component

    open fun startForegroundService(service: Intent): ComponentName? = service.component

    open fun checkUriPermission(uri: android.net.Uri, pid: Int, uid: Int, modeFlags: Int): Int = android.content.pm.PackageManager.PERMISSION_GRANTED

    open fun getMainExecutor(): java.util.concurrent.Executor = java.util.concurrent.Executor { javax.swing.SwingUtilities.invokeLater(it) }

    open fun sendBroadcast(intent: Intent) = DesktopIntents.sendBroadcast(this, intent)

    open fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? =
        DesktopIntents.registerReceiver(receiver, filter)

    open fun unregisterReceiver(receiver: BroadcastReceiver) = DesktopIntents.unregisterReceiver(receiver)

    /** Desktop grants every runtime permission (there is no permission model). */
    open fun checkSelfPermission(permission: String): Int = PackageManager.PERMISSION_GRANTED

    // ── Resources (generated R ids, see com.stash.desktop.res.DesktopResources) ──

    /** Set by [createConfigurationContext] / `applyOverrideConfiguration`: a delta over the system configuration. */
    internal open val overrideConfiguration: android.content.res.Configuration? get() = null

    open val resources: android.content.res.Resources
        get() = android.content.res.Resources {
            com.stash.desktop.res.DesktopDisplay.configuration().apply { overrideConfiguration?.let(::updateFrom) }
        }

    /** A Context whose [resources] see [overrideConfiguration] laid over this one's. */
    open fun createConfigurationContext(overrideConfiguration: android.content.res.Configuration): Context {
        val combined = android.content.res.Configuration().apply {
            this@Context.overrideConfiguration?.let(::updateFrom)
            updateFrom(overrideConfiguration)
        }
        return object : ContextWrapper(this@Context) {
            override val overrideConfiguration get() = combined
        }
    }

    fun getString(resId: Int): String = resources.getString(resId)

    fun getString(resId: Int, vararg formatArgs: Any?): String = resources.getString(resId, *formatArgs)

    fun getText(resId: Int): CharSequence = resources.getText(resId)

    companion object {
        const val MODE_PRIVATE = 0
        const val PACKAGE_NAME = "com.stash.app"

        const val ACTIVITY_SERVICE = "activity"
        const val AUDIO_SERVICE = "audio"
        const val BATTERY_SERVICE = "batterymanager"
        const val CLIPBOARD_SERVICE = "clipboard"
        const val CONNECTIVITY_SERVICE = "connectivity"
        const val NOTIFICATION_SERVICE = "notification"
        const val POWER_SERVICE = "power"
        const val WIFI_SERVICE = "wifi"
        const val USAGE_STATS_SERVICE = "usagestats"
        const val ACCESSIBILITY_SERVICE = "accessibility"
        const val WINDOW_SERVICE = "window"

        private val home = File(System.getProperty("user.home"))
        private val appSupport = File(home, "Library/Application Support/Stash")
        private val cacheRoot = File(home, "Library/Caches/Stash")

        private fun dir(parent: File, child: String? = null): File =
            (if (child == null) parent else File(parent, child)).apply { mkdirs() }
    }
}

/** The single application Context of the desktop process. */
object DesktopContext : Context()
