package android.view

import android.content.Context

/** Desktop shim for android.view.View: the Compose window's host view, as exposed through LocalView. */
open class View(val context: Context) {
    open val isInEditMode: Boolean get() = false
    var layoutParams: ViewGroup.LayoutParams? = null
    var keepScreenOn: Boolean = false
}
