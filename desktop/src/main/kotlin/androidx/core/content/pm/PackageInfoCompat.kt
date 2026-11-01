package androidx.core.content.pm

import android.content.pm.PackageInfo

object PackageInfoCompat {
    @JvmStatic fun getLongVersionCode(info: PackageInfo): Long = info.longVersionCode
}
