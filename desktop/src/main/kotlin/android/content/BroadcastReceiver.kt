package android.content

/**
 * Desktop shim for android.content.BroadcastReceiver. Receivers registered via
 * [Context.registerReceiver] get in-process [Context.sendBroadcast]s; there are no
 * system broadcasts (e.g. BOOT_COMPLETED never fires on desktop).
 */
abstract class BroadcastReceiver {
    abstract fun onReceive(context: Context, intent: Intent)

    /** Async completion handle; desktop receivers already run off the caller's thread. */
    fun goAsync(): PendingResult = PendingResult()

    open class PendingResult internal constructor() {
        fun finish() {}
    }
}
