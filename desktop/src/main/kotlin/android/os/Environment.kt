package android.os

import java.io.File

/**
 * Desktop shim for android.os.Environment. A path under /Volumes/<name> is MOUNTED while
 * that volume directory exists (REMOVED otherwise); other paths are on the always-mounted
 * boot volume.
 */
object Environment {
    const val MEDIA_UNKNOWN = "unknown"
    const val MEDIA_REMOVED = "removed"
    const val MEDIA_MOUNTED = "mounted"

    @JvmStatic
    fun getExternalStorageState(path: File): String {
        val parts = path.absoluteFile.toPath().normalize().iterator().asSequence().map { it.toString() }.toList()
        return if (parts.size >= 2 && parts[0] == "Volumes") {
            if (File("/Volumes/${parts[1]}").isDirectory) MEDIA_MOUNTED else MEDIA_REMOVED
        } else {
            MEDIA_MOUNTED
        }
    }
}
