package android.content

import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE

/**
 * Desktop shim for android.content.ContentResolver. There are no content providers on desktop:
 * `file:` URIs (and scheme-less absolute paths) are served from the filesystem with Android's
 * file-scheme semantics; every other scheme fails like an unresolvable provider
 * ([FileNotFoundException] / null type). Persistable grants are recorded in
 * `no_backup/persisted_uri_permissions` so [persistedUriPermissions] reflects what was taken.
 */
open class ContentResolver internal constructor(private val context: Context) {

    open fun openInputStream(uri: Uri): InputStream? {
        val file = localFileOf(uri) ?: throw FileNotFoundException("No content provider: $uri")
        return file.inputStream()
    }

    open fun openOutputStream(uri: Uri): OutputStream? = openOutputStream(uri, "w")

    /** Modes follow ParcelFileDescriptor.parseMode: w/wt/rwt truncate, wa appends, rw keeps content. */
    open fun openOutputStream(uri: Uri, mode: String): OutputStream? {
        val file = localFileOf(uri) ?: throw FileNotFoundException("No content provider: $uri")
        val options = when (mode) {
            "w", "wt", "rwt" -> arrayOf(WRITE, CREATE, TRUNCATE_EXISTING)
            "wa" -> arrayOf(WRITE, CREATE, APPEND)
            "rw" -> arrayOf(WRITE, CREATE)
            else -> throw IllegalArgumentException("Invalid mode: $mode")
        }
        return try {
            Files.newOutputStream(file.toPath(), *options)
        } catch (e: java.nio.file.NoSuchFileException) {
            throw FileNotFoundException(e.message)
        }
    }

    /** Like Android, only content providers report a type; there are none on desktop. */
    open fun getType(url: Uri): String? = null

    open fun takePersistableUriPermission(uri: Uri, modeFlags: Int) {
        synchronized(LOCK) {
            val all = readGrants().associateBy { it.uri }.toMutableMap()
            val merged = (all[uri]?.modeFlags ?: 0) or (modeFlags and (FLAG_READ or FLAG_WRITE))
            all[uri] = UriPermission(uri, merged, System.currentTimeMillis())
            grantsFile().writeText(all.values.joinToString("") { "${it.modeFlags}\t${it.persistedTime}\t${it.uri}\n" })
        }
    }

    open val persistedUriPermissions: List<UriPermission>
        get() = synchronized(LOCK) { readGrants() }

    private fun grantsFile() = File(context.noBackupFilesDir, "persisted_uri_permissions")

    private fun readGrants(): List<UriPermission> {
        val f = grantsFile()
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull { line ->
            val parts = line.split('\t', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            UriPermission(Uri.parse(parts[2]), parts[0].toIntOrNull() ?: return@mapNotNull null, parts[1].toLongOrNull() ?: 0L)
        }
    }

    companion object {
        const val SCHEME_CONTENT = "content"
        const val SCHEME_FILE = "file"
        const val SCHEME_ANDROID_RESOURCE = "android.resource"

        private const val FLAG_READ = 0x1 // Intent.FLAG_GRANT_READ_URI_PERMISSION
        private const val FLAG_WRITE = 0x2 // Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        private val LOCK = Any()

        /** The local file a URI denotes on desktop: `file:` URIs and scheme-less absolute paths. */
        internal fun localFileOf(uri: Uri): File? {
            val path = uri.path ?: return null
            return when (uri.scheme) {
                SCHEME_FILE -> File(path)
                null -> File(path).takeIf { it.isAbsolute }
                else -> null
            }
        }
    }
}
