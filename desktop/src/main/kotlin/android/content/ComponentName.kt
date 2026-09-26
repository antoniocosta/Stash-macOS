package android.content

/** Desktop shim for android.content.ComponentName (package + class name value object). */
data class ComponentName(val packageName: String, val className: String) {
    constructor(pkg: Context, cls: Class<*>) : this(pkg.packageName, cls.name)

    fun flattenToString(): String = "$packageName/$className"
}
