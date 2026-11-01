package android.view

import android.content.Context

/** Desktop shim for android.view.View: the Compose window's host view, as exposed through LocalView. */
open class View(val context: Context) {
    open val isInEditMode: Boolean get() = false
    var layoutParams: ViewGroup.LayoutParams? = null
    var keepScreenOn: Boolean = false
    var visibility: Int = VISIBLE
    var parent: ViewParent? = null
        internal set

    open fun setBackgroundColor(color: Int) {}

    open fun announceForAccessibility(text: CharSequence?) {}

    open fun post(action: Runnable): Boolean {
        javax.swing.SwingUtilities.invokeLater(action)
        return true
    }

    companion object {
        const val VISIBLE = 0
        const val INVISIBLE = 4
        const val GONE = 8
    }
}

/** Desktop shim for android.view.ViewParent. */
interface ViewParent
