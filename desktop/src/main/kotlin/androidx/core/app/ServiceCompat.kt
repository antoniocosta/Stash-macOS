package androidx.core.app

import android.app.Service

/**
 * Desktop shim for `androidx.core.app.ServiceCompat`.
 */
object ServiceCompat {
    const val STOP_FOREGROUND_REMOVE = 1
    const val STOP_FOREGROUND_DETACH = 2

    @JvmStatic
    fun stopForeground(service: Service, flags: Int) {
        // No-op on desktop.
    }
}
