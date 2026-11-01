package dagger.hilt.android

import android.content.Context

/**
 * Desktop shim for Hilt's EntryPointAccessors. The desktop Dagger component implements every
 * upstream @EntryPoint interface (DesktopAppComponent); Main.kt registers it in [DesktopEntryPoints].
 */
object EntryPointAccessors {
    @JvmStatic
    fun <T> fromApplication(context: Context, entryPoint: Class<T>): T = entryPoint.cast(DesktopEntryPoints.component())

    @JvmStatic
    fun <T> fromActivity(activity: android.app.Activity, entryPoint: Class<T>): T = entryPoint.cast(DesktopEntryPoints.component())
}

object DesktopEntryPoints {
    @Volatile var component: Any? = null

    fun component(): Any = component ?: error("Desktop Dagger component not initialised")
}
