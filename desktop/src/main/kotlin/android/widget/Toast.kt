package android.widget

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop shim for android.widget.Toast: [show] publishes the message to [DesktopToasts], which the
 * desktop window renders as a transient bottom overlay for the Android duration (2s short / 3.5s long).
 */
class Toast private constructor(private val text: CharSequence, private val duration: Int) {
    fun show() {
        Log.i("Toast", text.toString())
        DesktopToasts.post(text.toString(), if (duration == LENGTH_LONG) 3500L else 2000L)
    }

    fun cancel() = DesktopToasts.clear()

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        @JvmStatic
        fun makeText(context: Context?, text: CharSequence?, duration: Int): Toast = Toast(text ?: "", duration)

        @JvmStatic
        fun makeText(context: Context, resId: Int, duration: Int): Toast = Toast(context.getString(resId), duration)
    }
}

/** The currently visible toast, observed by the desktop window. */
object DesktopToasts {
    data class Message(val text: String, val durationMs: Long, val id: Long)

    private val state = MutableStateFlow<Message?>(null)
    val current: StateFlow<Message?> = state.asStateFlow()
    private var seq = 0L

    @Synchronized
    fun post(text: String, durationMs: Long) { state.value = Message(text, durationMs, ++seq) }

    fun clear() { state.value = null }

    /** Dismiss [message] if it is still the visible one. */
    fun dismiss(message: Message) { state.compareAndSet(message, null) }
}
