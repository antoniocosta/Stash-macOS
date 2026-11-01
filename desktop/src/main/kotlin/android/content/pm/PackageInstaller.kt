package android.content.pm

import android.content.IntentSender
import java.io.IOException
import java.io.OutputStream

/**
 * Desktop shim for android.content.pm.PackageInstaller. macOS can't install an Android package, so
 * a session can't be created (an IOException, which upstream's updater reports as an install failure).
 */
class PackageInstaller private constructor() {
    class SessionParams(val mode: Int) {
        fun setAppPackageName(appPackageName: String?) {}
        fun setSize(sizeBytes: Long) {}
        fun setRequireUserAction(requireUserAction: Int) {}

        companion object {
            const val MODE_FULL_INSTALL = 1
            const val MODE_INHERIT_EXISTING = 2
            const val USER_ACTION_UNSPECIFIED = 0
            const val USER_ACTION_REQUIRED = 1
            const val USER_ACTION_NOT_REQUIRED = 2
        }
    }

    class Session internal constructor() : AutoCloseable {
        fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream = throw IOException("Package installs are not supported on macOS")
        fun fsync(out: OutputStream) {}
        fun commit(statusReceiver: IntentSender) {}
        fun abandon() {}
        override fun close() {}
    }

    fun createSession(params: SessionParams): Int = throw IOException("Package installs are not supported on macOS")
    fun openSession(sessionId: Int): Session = throw IOException("Package installs are not supported on macOS")
    fun abandonSession(sessionId: Int) {}

    companion object {
        internal val INSTANCE = PackageInstaller()
        const val EXTRA_STATUS = "android.content.pm.extra.STATUS"
        const val EXTRA_STATUS_MESSAGE = "android.content.pm.extra.STATUS_MESSAGE"
        const val EXTRA_SESSION_ID = "android.content.pm.extra.SESSION_ID"
        const val STATUS_PENDING_USER_ACTION = -1
        const val STATUS_SUCCESS = 0
        const val STATUS_FAILURE = 1
        const val STATUS_FAILURE_BLOCKED = 2
        const val STATUS_FAILURE_ABORTED = 3
        const val STATUS_FAILURE_INVALID = 4
        const val STATUS_FAILURE_CONFLICT = 5
        const val STATUS_FAILURE_STORAGE = 6
        const val STATUS_FAILURE_INCOMPATIBLE = 7
    }
}
