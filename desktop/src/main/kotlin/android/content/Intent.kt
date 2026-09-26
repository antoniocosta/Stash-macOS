package android.content

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable

/**
 * Desktop shim for android.content.Intent: a plain value object. What an Intent
 * *does* on desktop (open a URL, share, relaunch, broadcast) is decided by [DesktopIntents].
 */
class Intent() : Parcelable {
    var action: String? = null
    var data: Uri? = null
    var type: String? = null
    var flags: Int = 0
    val `package`: String? get() = pkg
    private var pkg: String? = null
    val component: ComponentName? get() = cmp
    private var cmp: ComponentName? = null
    var extras: Bundle? = null
        private set
    val categories: MutableSet<String>? get() = _categories
    private var _categories: MutableSet<String>? = null

    constructor(action: String?) : this() { this.action = action }
    constructor(action: String?, uri: Uri?) : this(action) { data = uri }
    constructor(packageContext: Context, cls: Class<*>) : this() { cmp = ComponentName(packageContext, cls) }
    constructor(o: Intent) : this() {
        action = o.action; data = o.data; type = o.type; flags = o.flags; pkg = o.pkg
        cmp = o.cmp; extras = o.extras?.let { Bundle(it) }; _categories = o._categories?.toMutableSet()
    }

    fun addFlags(flags: Int): Intent = apply { this.flags = this.flags or flags }
    fun addCategory(category: String): Intent = apply { (_categories ?: mutableSetOf<String>().also { _categories = it }) += category }
    fun hasCategory(category: String?): Boolean = _categories?.contains(category) == true
    fun setPackage(packageName: String?): Intent = apply { pkg = packageName }
    fun setComponent(component: ComponentName?): Intent = apply { cmp = component }
    fun setClassName(packageName: String, className: String): Intent =
        apply { cmp = ComponentName(packageName, className) }
    fun setClass(packageContext: Context, cls: Class<*>): Intent = apply { cmp = ComponentName(packageContext, cls) }

    private fun bundle(): Bundle = extras ?: Bundle().also { extras = it }
    fun putExtra(name: String, value: String?): Intent = apply { bundle().putString(name, value) }
    fun putExtra(name: String, value: CharSequence?): Intent = apply { bundle().putCharSequence(name, value) }
    fun putExtra(name: String, value: Int): Intent = apply { bundle().putInt(name, value) }
    fun putExtra(name: String, value: Long): Intent = apply { bundle().putLong(name, value) }
    fun putExtra(name: String, value: Boolean): Intent = apply { bundle().putBoolean(name, value) }
    fun putExtra(name: String, value: Parcelable?): Intent = apply { bundle().putParcelable(name, value) }
    fun putExtras(extras: Bundle): Intent = apply { bundle().putAll(extras) }
    fun hasExtra(name: String): Boolean = extras?.containsKey(name) == true
    fun getStringExtra(name: String): String? = extras?.getString(name)
    fun getCharSequenceExtra(name: String): CharSequence? = extras?.getCharSequence(name)
    fun getIntExtra(name: String, defaultValue: Int): Int = extras?.getInt(name, defaultValue) ?: defaultValue
    fun getLongExtra(name: String, defaultValue: Long): Long = extras?.getLong(name, defaultValue) ?: defaultValue
    fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean =
        extras?.getBoolean(name, defaultValue) ?: defaultValue
    fun <T : Parcelable> getParcelableExtra(name: String): T? = extras?.getParcelable(name)
    fun removeExtra(name: String) { extras?.remove(name) }

    override fun toString(): String =
        "Intent { act=$action dat=$data typ=$type flg=0x${flags.toString(16)} pkg=$pkg cmp=$cmp cat=$_categories extras=$extras }"

    companion object {
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE"
        const val ACTION_CHOOSER = "android.intent.action.CHOOSER"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        const val ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED"

        const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
        const val CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE"
        const val CATEGORY_DEFAULT = "android.intent.category.DEFAULT"

        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_INTENT = "android.intent.extra.INTENT"
        const val EXTRA_TITLE = "android.intent.extra.TITLE"

        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001
        const val FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002
        const val FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040
        const val FLAG_ACTIVITY_SINGLE_TOP = 0x20000000
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000

        @JvmStatic
        fun createChooser(target: Intent, title: CharSequence?): Intent =
            Intent(ACTION_CHOOSER).putExtra(EXTRA_INTENT, target).apply { if (title != null) putExtra(EXTRA_TITLE, title) }
    }
}
