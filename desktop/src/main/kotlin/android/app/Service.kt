package android.app

import android.content.Context
import android.content.Intent

/**
 * Desktop shim for `android.app.Service`.
 */
abstract class Service : Context() {
    open fun onCreate() {}
    open fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    open fun onDestroy() {}
    open fun onTaskRemoved(rootIntent: Intent?) {}

    fun startForeground(id: Int, notification: Notification) {
        NotificationManager.INSTANCE.notify(id, notification)
    }

    fun startForeground(id: Int, notification: Notification, foregroundServiceType: Int) {
        NotificationManager.INSTANCE.notify(id, notification)
    }

    open fun stopSelf() {
        onDestroy()
    }

    companion object {
        const val START_STICKY = 1
        const val START_NOT_STICKY = 2
    }
}
