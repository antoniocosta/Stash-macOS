package androidx.core.content

import android.content.Context

/** Desktop shim for androidx.core.content.ContextCompat; permission checks delegate to Context (always granted on desktop). */
object ContextCompat {
    @JvmStatic
    fun checkSelfPermission(context: Context, permission: String): Int = context.checkSelfPermission(permission)
}
