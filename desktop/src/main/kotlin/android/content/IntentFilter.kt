package android.content

/** Desktop shim for android.content.IntentFilter: action/category matching for in-process broadcasts. */
class IntentFilter() {
    private val actions = mutableListOf<String>()
    private val categories = mutableListOf<String>()

    constructor(action: String) : this() { addAction(action) }

    fun addAction(action: String) { actions += action }
    fun addCategory(category: String) { categories += category }
    fun countActions(): Int = actions.size
    fun getAction(index: Int): String = actions[index]
    fun hasAction(action: String?): Boolean = action in actions
    fun matchAction(action: String?): Boolean = hasAction(action)
}
