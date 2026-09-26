package android.app

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop shim for android.app.NotificationManager. No macOS notifications are posted:
 * notify()/cancel() are logged via android.util.Log (tag "Notification"). Ongoing /
 * progress updates log at DEBUG to avoid flooding; everything else at INFO.
 */
class NotificationManager private constructor() {
    private val channels = ConcurrentHashMap<String, NotificationChannel>()
    private val active = ConcurrentHashMap<String, Notification>()

    fun createNotificationChannel(channel: NotificationChannel) { channels.putIfAbsent(channel.id, channel) }
    fun createNotificationChannels(channels: List<NotificationChannel>) = channels.forEach(::createNotificationChannel)
    fun getNotificationChannel(channelId: String): NotificationChannel? = channels[channelId]
    fun deleteNotificationChannel(channelId: String) { channels.remove(channelId) }
    fun areNotificationsEnabled(): Boolean = true

    fun notify(id: Int, notification: Notification) = notify(null, id, notification)

    fun notify(tag: String?, id: Int, notification: Notification) {
        active[key(tag, id)] = notification
        val e = notification.extras
        val body = e.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(Notification.EXTRA_TEXT)
        val progress = when {
            e.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE) -> " [in progress]"
            e.getInt(Notification.EXTRA_PROGRESS_MAX) > 0 ->
                " [${e.getInt(Notification.EXTRA_PROGRESS)}/${e.getInt(Notification.EXTRA_PROGRESS_MAX)}]"
            else -> ""
        }
        val msg = "#$id (${notification.channelId}) ${e.getCharSequence(Notification.EXTRA_TITLE)}: $body$progress"
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) Log.d(TAG, msg) else Log.i(TAG, msg)
    }

    fun cancel(id: Int) = cancel(null, id)

    fun cancel(tag: String?, id: Int) {
        if (active.remove(key(tag, id)) != null) Log.d(TAG, "#$id cancelled")
    }

    fun cancelAll() = active.clear()

    private fun key(tag: String?, id: Int) = "$tag:$id"

    companion object {
        private const val TAG = "Notification"
        const val IMPORTANCE_UNSPECIFIED = -1000
        const val IMPORTANCE_NONE = 0
        const val IMPORTANCE_MIN = 1
        const val IMPORTANCE_LOW = 2
        const val IMPORTANCE_DEFAULT = 3
        const val IMPORTANCE_HIGH = 4

        internal val INSTANCE = NotificationManager()
    }
}
