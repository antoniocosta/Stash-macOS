package android.content.pm

import android.content.Context
import android.content.Intent

/**
 * Desktop shim for android.content.pm.PackageManager. Knows exactly one package (this app,
 * version mirrored from app/build.gradle.kts); URL VIEW intents resolve to macOS
 * LaunchServices (`open`), which is what [android.content.Context.startActivity] uses.
 */
open class PackageManager internal constructor() {

    @Throws(NameNotFoundException::class)
    open fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
        if (packageName != Context.PACKAGE_NAME) throw NameNotFoundException(packageName)
        return PackageInfo().apply {
            this.packageName = packageName
            versionName = VERSION_NAME
            @Suppress("DEPRECATION")
            versionCode = VERSION_CODE
            longVersionCode = VERSION_CODE.toLong()
        }
    }

    open fun getLaunchIntentForPackage(packageName: String): Intent? =
        if (packageName != Context.PACKAGE_NAME) null
        else Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)

    open fun queryIntentActivities(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        val scheme = intent.data?.scheme?.lowercase()
        return if (intent.action == Intent.ACTION_VIEW && (scheme == "http" || scheme == "https")) {
            mutableListOf(ResolveInfo(ActivityInfo(LAUNCH_SERVICES, "open")))
        } else {
            mutableListOf()
        }
    }

    open fun checkPermission(permName: String, packageName: String): Int = PERMISSION_GRANTED

    class NameNotFoundException(name: String? = null) : Exception(name)

    companion object {
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
        const val MATCH_DEFAULT_ONLY = 0x00010000
        const val MATCH_ALL = 0x00020000
        const val GET_META_DATA = 0x00000080

        /** From the generated com.stash.app.BuildConfig (read from upstream app/build.gradle.kts). */
        internal const val VERSION_NAME = com.stash.app.BuildConfig.VERSION_NAME
        internal const val VERSION_CODE = com.stash.app.BuildConfig.VERSION_CODE
        internal const val LAUNCH_SERVICES = "com.apple.LaunchServices"

        internal val INSTANCE = PackageManager()
    }
}
