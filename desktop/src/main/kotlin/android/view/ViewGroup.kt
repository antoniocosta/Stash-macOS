package android.view

import android.content.Context

open class ViewGroup(context: Context) : View(context), ViewParent {
    private val children = ArrayList<View>()

    open fun addView(child: View) {
        (child.parent as? ViewGroup)?.removeView(child)
        children += child
        child.parent = this
    }

    open fun removeView(view: View) {
        if (children.remove(view)) view.parent = null
    }

    open class LayoutParams(
        var width: Int,
        var height: Int,
    ) {
        companion object {
            const val MATCH_PARENT = -1
            const val WRAP_CONTENT = -2
        }
    }
}
