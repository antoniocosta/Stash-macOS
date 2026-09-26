package android.content

import android.net.Uri

/** Desktop shim for android.content.UriPermission: a grant recorded by [ContentResolver.takePersistableUriPermission]. */
class UriPermission internal constructor(
    val uri: Uri,
    internal val modeFlags: Int,
    val persistedTime: Long,
) {
    val isReadPermission: Boolean get() = modeFlags and 0x1 != 0
    val isWritePermission: Boolean get() = modeFlags and 0x2 != 0

    override fun toString(): String = "UriPermission {uri=$uri, modeFlags=$modeFlags, persistedTime=$persistedTime}"
}
