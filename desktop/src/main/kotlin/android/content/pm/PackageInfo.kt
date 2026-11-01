package android.content.pm

/** Desktop shim for android.content.pm.PackageInfo (filled by [PackageManager.getPackageInfo]). */
class PackageInfo {
    @JvmField var packageName: String? = null
    @JvmField var versionName: String? = null
    @Deprecated("Use longVersionCode") @JvmField var versionCode: Int = 0
    var longVersionCode: Long = 0
    @JvmField var firstInstallTime: Long = 0
    @JvmField var lastUpdateTime: Long = 0
    @JvmField var signingInfo: SigningInfo? = null
    @Deprecated("Use signingInfo") @JvmField var signatures: Array<Signature>? = null
}

/** Desktop shim for android.content.pm.Signature (a certificate's bytes). */
class Signature(private val bytes: ByteArray) {
    fun toByteArray(): ByteArray = bytes.copyOf()
}

/** Desktop shim for android.content.pm.SigningInfo. */
class SigningInfo(
    val apkContentsSigners: Array<Signature> = emptyArray(),
    val signingCertificateHistory: Array<Signature> = emptyArray(),
) {
    fun hasMultipleSigners(): Boolean = apkContentsSigners.size > 1
}
