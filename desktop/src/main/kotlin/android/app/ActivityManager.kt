package android.app

/**
 * Desktop shim for android.app.ActivityManager. macOS keeps no per-app exit-reason
 * ring buffer, so [getHistoricalProcessExitReasons] honestly returns an empty list.
 */
class ActivityManager private constructor() {
    fun getHistoricalProcessExitReasons(packageName: String?, pid: Int, maxNum: Int): MutableList<ApplicationExitInfo> =
        mutableListOf()

    /** macOS never restricts a running app's background work. */
    val isBackgroundRestricted: Boolean get() = false

    companion object {
        internal val INSTANCE = ActivityManager()
    }
}
