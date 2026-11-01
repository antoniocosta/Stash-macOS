package android.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.File

/** Desktop shim for android.content.ContextWrapper: forwards to [baseContext]. */
open class ContextWrapper(base: Context?) : Context() {
    private var base: Context? = base

    open val baseContext: Context get() = base ?: DesktopContext

    /** Android's protected `attachBaseContext`: an Activity's base is attached once, before onCreate. */
    protected open fun attachBaseContext(base: Context) {
        check(this.base == null || this.base === base) { "Base context already set" }
        this.base = base
    }

    internal fun replaceBase(newBase: Context) {
        base = newBase
    }

    internal override val overrideConfiguration: android.content.res.Configuration? get() = baseContext.overrideConfiguration

    override val applicationContext: Context get() = baseContext.applicationContext
    override val packageName: String get() = baseContext.packageName
    override val filesDir: File get() = baseContext.filesDir
    override val noBackupFilesDir: File get() = baseContext.noBackupFilesDir
    override val cacheDir: File get() = baseContext.cacheDir
    override val packageManager: PackageManager get() = baseContext.packageManager
    override val applicationInfo: ApplicationInfo get() = baseContext.applicationInfo
}

/** Desktop shim for android.content.MutableContextWrapper. */
open class MutableContextWrapper(base: Context?) : ContextWrapper(base) {
    override var baseContext: Context
        get() = super.baseContext
        set(value) = replaceBase(value)
}
