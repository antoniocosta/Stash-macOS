package android.app

/** Desktop shim for android.app.ApplicationExitInfo (reason constants; never instantiated on desktop). */
class ApplicationExitInfo private constructor(
    val timestamp: Long,
    val reason: Int,
    val importance: Int,
    val pss: Long,
    val rss: Long,
    val description: String?,
) {
    companion object {
        const val REASON_UNKNOWN = 0
        const val REASON_EXIT_SELF = 1
        const val REASON_SIGNALED = 2
        const val REASON_LOW_MEMORY = 3
        const val REASON_CRASH = 4
        const val REASON_CRASH_NATIVE = 5
        const val REASON_ANR = 6
        const val REASON_INITIALIZATION_FAILURE = 7
        const val REASON_PERMISSION_CHANGE = 8
        const val REASON_EXCESSIVE_RESOURCE_USAGE = 9
        const val REASON_USER_REQUESTED = 10
        const val REASON_USER_STOPPED = 11
        const val REASON_DEPENDENCY_DIED = 12
        const val REASON_OTHER = 13
    }
}
