package androidx.core.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context

/** Desktop shim for androidx.core.app.NotificationManagerCompat; delegates to the logging [NotificationManager]. */
class NotificationManagerCompat private constructor(private val nm: NotificationManager) {

    fun areNotificationsEnabled(): Boolean = nm.areNotificationsEnabled()
    fun notify(id: Int, notification: Notification) = nm.notify(id, notification)
    fun notify(tag: String?, id: Int, notification: Notification) = nm.notify(tag, id, notification)
    fun cancel(id: Int) = nm.cancel(id)
    fun cancel(tag: String?, id: Int) = nm.cancel(tag, id)
    fun cancelAll() = nm.cancelAll()

    companion object {
        const val IMPORTANCE_NONE = NotificationManager.IMPORTANCE_NONE
        const val IMPORTANCE_LOW = NotificationManager.IMPORTANCE_LOW
        const val IMPORTANCE_DEFAULT = NotificationManager.IMPORTANCE_DEFAULT
        const val IMPORTANCE_HIGH = NotificationManager.IMPORTANCE_HIGH

        @JvmStatic
        fun from(context: Context): NotificationManagerCompat =
            NotificationManagerCompat(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }
}
