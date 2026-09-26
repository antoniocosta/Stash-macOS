package android.content.pm

/** Desktop shim for android.content.pm.PackageInfo (filled by [PackageManager.getPackageInfo]). */
class PackageInfo {
    @JvmField var packageName: String? = null
    @JvmField var versionName: String? = null
    @Deprecated("Use longVersionCode") @JvmField var versionCode: Int = 0
    var longVersionCode: Long = 0
    @JvmField var firstInstallTime: Long = 0
    @JvmField var lastUpdateTime: Long = 0
}
