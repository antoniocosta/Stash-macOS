package android.app

import android.os.Bundle

/**
 * Desktop shim for android.app.Notification: the built value object. Title/text live in
 * [extras] under Android's own keys; [NotificationManager] logs them instead of showing UI.
 */
class Notification() {
    @JvmField var extras: Bundle = Bundle()
    @JvmField var flags: Int = 0
    @JvmField var icon: Int = 0
    @JvmField var contentIntent: PendingIntent? = null
    @JvmField var actions: Array<Action>? = null
    @JvmField var `when`: Long = System.currentTimeMillis()
    var channelId: String? = null
        internal set

    class Action(@JvmField val icon: Int, @JvmField val title: CharSequence?, @JvmField val actionIntent: PendingIntent?)

    companion object {
        const val FLAG_ONGOING_EVENT = 0x00000002
        const val FLAG_ONLY_ALERT_ONCE = 0x00000008
        const val FLAG_AUTO_CANCEL = 0x00000010
        const val FLAG_FOREGROUND_SERVICE = 0x00000040

        const val EXTRA_TITLE = "android.title"
        const val EXTRA_TEXT = "android.text"
        const val EXTRA_BIG_TEXT = "android.bigText"
        const val EXTRA_PROGRESS = "android.progress"
        const val EXTRA_PROGRESS_MAX = "android.progressMax"
        const val EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate"
    }
}
