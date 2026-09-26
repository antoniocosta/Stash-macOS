package androidx.core.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Context

/** Desktop shim for androidx.core.app.NotificationCompat: builds the plain [Notification] value that the logging NotificationManager prints. */
class NotificationCompat private constructor() {

    class Builder(@Suppress("UNUSED_PARAMETER") context: Context, private val channelId: String) {
        private var title: CharSequence? = null
        private var text: CharSequence? = null
        private var smallIcon = 0
        private var flags = 0
        private var contentIntent: PendingIntent? = null
        private var style: Style? = null
        private var progressMax = 0
        private var progress = 0
        private var indeterminate = false
        private val actions = mutableListOf<Notification.Action>()

        private fun flag(mask: Int, on: Boolean) = apply { flags = if (on) flags or mask else flags and mask.inv() }

        fun setContentTitle(title: CharSequence?): Builder = apply { this.title = title }
        fun setContentText(text: CharSequence?): Builder = apply { this.text = text }
        fun setSmallIcon(icon: Int): Builder = apply { smallIcon = icon }
        fun setAutoCancel(autoCancel: Boolean): Builder = flag(Notification.FLAG_AUTO_CANCEL, autoCancel)
        fun setOngoing(ongoing: Boolean): Builder = flag(Notification.FLAG_ONGOING_EVENT, ongoing)
        fun setOnlyAlertOnce(onlyAlertOnce: Boolean): Builder = flag(Notification.FLAG_ONLY_ALERT_ONCE, onlyAlertOnce)
        fun setSilent(silent: Boolean): Builder = this
        fun setPriority(pri: Int): Builder = this
        fun setCategory(category: String?): Builder = this
        fun setForegroundServiceBehavior(behavior: Int): Builder = this
        fun setContentIntent(intent: PendingIntent?): Builder = apply { contentIntent = intent }
        fun setStyle(style: Style?): Builder = apply { this.style = style }
        fun setProgress(max: Int, progress: Int, indeterminate: Boolean): Builder = apply {
            progressMax = max; this.progress = progress; this.indeterminate = indeterminate
        }
        fun addAction(icon: Int, title: CharSequence?, intent: PendingIntent?): Builder =
            apply { actions += Notification.Action(icon, title, intent) }

        fun build(): Notification = Notification().also { n ->
            n.channelId = channelId
            n.icon = smallIcon
            n.flags = flags
            n.contentIntent = contentIntent
            n.actions = actions.toTypedArray().takeIf { it.isNotEmpty() }
            n.extras.putCharSequence(Notification.EXTRA_TITLE, title)
            n.extras.putCharSequence(Notification.EXTRA_TEXT, text)
            if (progressMax > 0 || indeterminate) {
                n.extras.putInt(Notification.EXTRA_PROGRESS, progress)
                n.extras.putInt(Notification.EXTRA_PROGRESS_MAX, progressMax)
                n.extras.putBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, indeterminate)
            }
            (style as? BigTextStyle)?.text?.let { n.extras.putCharSequence(Notification.EXTRA_BIG_TEXT, it) }
        }
    }

    abstract class Style

    class BigTextStyle() : Style() {
        internal var text: CharSequence? = null
        fun bigText(cs: CharSequence?): BigTextStyle = apply { text = cs }
    }

    companion object {
        const val FOREGROUND_SERVICE_DEFAULT = 0
        const val FOREGROUND_SERVICE_IMMEDIATE = 1
        const val FOREGROUND_SERVICE_DEFERRED = 2

        const val PRIORITY_MIN = -2
        const val PRIORITY_LOW = -1
        const val PRIORITY_DEFAULT = 0
        const val PRIORITY_HIGH = 1
        const val PRIORITY_MAX = 2
    }
}
