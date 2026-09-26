package android.view

import android.content.Context

open class ViewGroup(context: Context) : View(context) {
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
