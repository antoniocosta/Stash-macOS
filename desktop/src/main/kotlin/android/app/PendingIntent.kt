package android.app

import android.content.Context
import android.content.Intent

/**
 * Desktop shim for android.app.PendingIntent: a deferred Intent. Nothing on desktop taps
 * notifications, but [send] performs the Intent for real (activity or in-process broadcast).
 */
class PendingIntent private constructor(
    private val context: Context,
    val requestCode: Int,
    private val intent: Intent,
    val flags: Int,
    private val broadcast: Boolean,
) {
    fun send() {
        if (broadcast) context.sendBroadcast(Intent(intent)) else context.startActivity(Intent(intent))
    }

    fun cancel() {}

    val creatorPackage: String get() = context.packageName

    companion object {
        const val FLAG_ONE_SHOT = 1 shl 30
        const val FLAG_NO_CREATE = 1 shl 29
        const val FLAG_CANCEL_CURRENT = 1 shl 28
        const val FLAG_UPDATE_CURRENT = 1 shl 27
        const val FLAG_IMMUTABLE = 1 shl 26
        const val FLAG_MUTABLE = 1 shl 25

        @JvmStatic
        fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent =
            PendingIntent(context, requestCode, Intent(intent), flags, broadcast = false)

        @JvmStatic
        fun getBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent =
            PendingIntent(context, requestCode, Intent(intent), flags, broadcast = true)
    }
}
