package android.os

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/**
 * Desktop shim for android.os.Looper. The desktop's main thread is the Swing event dispatch thread
 * (where Compose Desktop and `Dispatchers.Main` run), so [getMainLooper] is the EDT and
 * [myLooper] is it when called there, null elsewhere (a plain thread has no Looper on Android either).
 */
class Looper private constructor() {
    val thread: Thread? get() = null

    fun isCurrentThread(): Boolean = SwingUtilities.isEventDispatchThread()

    internal fun post(r: Runnable) = SwingUtilities.invokeLater(r)

    companion object {
        private val MAIN = Looper()

        @JvmStatic fun getMainLooper(): Looper = MAIN

        @JvmStatic fun myLooper(): Looper? = if (SwingUtilities.isEventDispatchThread()) MAIN else null
    }
}

/**
 * Desktop shim for android.os.Handler on [Looper]: posts run on the EDT; delayed posts are timed on
 * a shared scheduler and then hopped onto the EDT, and [removeCallbacks] cancels both stages.
 */
open class Handler @JvmOverloads constructor(
    val looper: Looper = Looper.myLooper() ?: throw RuntimeException("Can't create handler inside thread that has not called Looper.prepare()"),
    private val callback: Callback? = null,
) {
    fun interface Callback {
        fun handleMessage(msg: Message): Boolean
    }

    private val pending = HashMap<Runnable, MutableList<ScheduledFuture<*>>>()
    private val queued = HashMap<Runnable, Int>()

    fun post(r: Runnable): Boolean = postDelayed(r, 0L)

    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        if (delayMillis <= 0L) {
            synchronized(this) { queued.merge(r, 1, Int::plus) }
            looper.post { if (consumeQueued(r)) r.run() }
            return true
        }
        lateinit var future: ScheduledFuture<*>
        future = scheduler.schedule({
            synchronized(this) { pending[r]?.remove(future) }
            post(r)
        }, delayMillis, TimeUnit.MILLISECONDS)
        synchronized(this) { pending.getOrPut(r) { mutableListOf() }.add(future) }
        return true
    }

    fun postAtFrontOfQueue(r: Runnable): Boolean = post(r)

    fun removeCallbacks(r: Runnable) {
        synchronized(this) {
            pending.remove(r)?.forEach { it.cancel(false) }
            queued.remove(r)
        }
    }

    fun removeCallbacksAndMessages(token: Any?) {
        synchronized(this) {
            pending.values.flatten().forEach { it.cancel(false) }
            pending.clear()
            queued.clear()
        }
    }

    fun hasCallbacks(r: Runnable): Boolean = synchronized(this) { pending[r]?.isNotEmpty() == true || (queued[r] ?: 0) > 0 }

    fun sendEmptyMessage(what: Int): Boolean = post { dispatchMessage(Message().also { it.what = what }) }

    open fun handleMessage(msg: Message) {}

    fun dispatchMessage(msg: Message) {
        if (callback?.handleMessage(msg) != true) handleMessage(msg)
    }

    private fun consumeQueued(r: Runnable): Boolean = synchronized(this) {
        val n = queued[r] ?: return false
        if (n <= 1) queued.remove(r) else queued[r] = n - 1
        true
    }

    companion object {
        private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "android.os.Handler-timer").apply { isDaemon = true }
        }

        @JvmStatic fun createAsync(looper: Looper): Handler = Handler(looper)
    }
}

/** Desktop shim for android.os.Message (the fields Handler callers use). */
class Message {
    @JvmField var what: Int = 0
    @JvmField var arg1: Int = 0
    @JvmField var arg2: Int = 0
    @JvmField var obj: Any? = null
}
