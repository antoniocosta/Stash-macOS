package androidx.core.view

import android.view.View
import android.view.Window

/** Desktop shim for androidx.core.view.WindowCompat: returns an insets controller for the (bar-less) macOS window. */
object WindowCompat {
    @JvmStatic
    fun getInsetsController(window: Window, view: View): WindowInsetsControllerCompat = WindowInsetsControllerCompat(window, view)

    @JvmStatic
    fun setDecorFitsSystemWindows(window: Window, decorFitsSystemWindows: Boolean) {}
}

/** Stores the requested bar-icon appearance; macOS has no status/navigation bars to restyle. */
class WindowInsetsControllerCompat(window: Window, view: View) {
    var isAppearanceLightStatusBars: Boolean = false
    var isAppearanceLightNavigationBars: Boolean = false
}
