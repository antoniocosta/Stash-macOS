package android.view

/** Desktop shim for android.view.Window: stores the system-bar colours; macOS has no system bars to paint. */
open class Window {
    var statusBarColor: Int = 0
    var navigationBarColor: Int = 0
    var isNavigationBarContrastEnforced: Boolean = false
    var isStatusBarContrastEnforced: Boolean = false
    var flags: Int = 0
        private set

    fun addFlags(flags: Int) { this.flags = this.flags or flags }
    fun clearFlags(flags: Int) { this.flags = this.flags and flags.inv() }
}

/** Desktop shim for android.view.WindowManager (the LayoutParams flags upstream sets). */
interface WindowManager {
    open class LayoutParams {
        companion object {
            const val FLAG_KEEP_SCREEN_ON = 0x00000080
            const val FLAG_SECURE = 0x00002000
            const val FLAG_LAYOUT_NO_LIMITS = 0x00000200
            const val FLAG_DIM_BEHIND = 0x00000002
            const val FLAG_NOT_TOUCHABLE = 0x00000010
        }
    }
}
