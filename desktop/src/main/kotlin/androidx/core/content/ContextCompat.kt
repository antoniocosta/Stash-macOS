package androidx.core.content

import android.content.Context

/** Desktop shim for androidx.core.content.ContextCompat; permission checks delegate to Context (always granted on desktop). */
object ContextCompat {
    /** The main thread's executor: the Swing EDT, the desktop's main looper. */
    @JvmStatic
    fun getMainExecutor(context: Context): java.util.concurrent.Executor =
        java.util.concurrent.Executor { javax.swing.SwingUtilities.invokeLater(it) }

    @JvmStatic
    fun checkSelfPermission(context: Context, permission: String): Int = context.checkSelfPermission(permission)
}
