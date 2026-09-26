package android.view

/** Desktop shim for android.view.Window: stores the system-bar colours; macOS has no system bars to paint. */
open class Window {
    var statusBarColor: Int = 0
    var navigationBarColor: Int = 0
}
