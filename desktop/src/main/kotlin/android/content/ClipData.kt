package android.content

/** Desktop shim for android.content.ClipData (plain-text clips only). */
class ClipData private constructor(val label: CharSequence?, val text: CharSequence?) {
    fun getItemCount(): Int = 1
    fun getItemAt(index: Int): Item = Item(text)

    class Item(val text: CharSequence?)

    companion object {
        @JvmStatic
        fun newPlainText(label: CharSequence?, text: CharSequence?): ClipData = ClipData(label, text)
    }
}
