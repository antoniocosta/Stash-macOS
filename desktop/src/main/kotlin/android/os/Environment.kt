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
    const val DIRECTORY_DOWNLOADS = "Download"
    const val DIRECTORY_MUSIC = "Music"
    const val DIRECTORY_PICTURES = "Pictures"
    const val DIRECTORY_DOCUMENTS = "Documents"

    /** The macOS counterpart of a shared-storage directory (~/Downloads for "Download", else ~/<name>). */
    @JvmStatic
    fun getExternalStoragePublicDirectory(type: String): File =
        File(System.getProperty("user.home"), if (type == DIRECTORY_DOWNLOADS) "Downloads" else type)

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
