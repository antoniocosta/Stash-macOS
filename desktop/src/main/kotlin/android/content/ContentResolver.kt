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

    /**
     * Queries the desktop's providers: a `file:` document (its OpenableColumns / Document columns)
     * or a `file:` children URI from DocumentsContract (one row per entry of the folder). Anything
     * else has no provider, which Android answers with null.
     */
    open fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): android.database.Cursor? {
        val file = localFileOf(uri) ?: return null
        val columns = projection ?: arrayOf(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
            android.provider.DocumentsContract.Document.COLUMN_SIZE,
            android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val entries = if (uri.getQueryParameter(android.provider.DocumentsContract.CHILDREN_QUERY) != null) {
            if (!file.isDirectory) return null
            file.listFiles().orEmpty().sortedBy { it.name }
        } else {
            if (!file.exists()) return null
            listOf(file)
        }
        return android.database.MatrixCursor(columns, entries.map { f -> Array(columns.size) { documentColumn(f, columns[it]) } })
    }

    private fun documentColumn(f: File, column: String): Any? = when (column) {
        android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID -> f.absolutePath
        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME -> f.name
        android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE ->
            if (f.isDirectory) android.provider.DocumentsContract.Document.MIME_TYPE_DIR
            else android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "application/octet-stream"
        android.provider.DocumentsContract.Document.COLUMN_SIZE -> if (f.isDirectory) null else f.length()
        android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED -> f.lastModified()
        android.provider.DocumentsContract.Document.COLUMN_FLAGS ->
            android.provider.DocumentsContract.Document.FLAG_SUPPORTS_WRITE or android.provider.DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                android.provider.DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        "_data" -> f.absolutePath
        else -> null
    }

    /** MediaStore.Downloads inserts land in ~/Downloads (a fresh name if one is taken), returned as a `file:` URI. */
    open fun insert(url: Uri, values: ContentValues?): Uri? {
        if (url.toString().removePrefix("content://") != android.provider.MediaStore.DOWNLOADS_AUTHORITY_PATH) return null
        val name = values?.getAsString(android.provider.MediaStore.MediaColumns.DISPLAY_NAME) ?: return null
        val relative = values.getAsString(android.provider.MediaStore.MediaColumns.RELATIVE_PATH).orEmpty()
            .removePrefix(android.os.Environment.DIRECTORY_DOWNLOADS).trim('/')
        val dir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), relative).apply { mkdirs() }
        var target = File(dir, name)
        var n = 1
        while (target.exists()) target = File(dir, "${name.substringBeforeLast('.')} (${n++})" + name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" })
        target.createNewFile()
        return Uri.fromFile(target)
    }

    /** Pending flags and the like: nothing to update on a plain file. */
    open fun update(uri: Uri, values: ContentValues?, where: String?, selectionArgs: Array<String>?): Int =
        if (localFileOf(uri)?.exists() == true) 1 else 0

    open fun delete(url: Uri, where: String?, selectionArgs: Array<String>?): Int =
        if (localFileOf(url)?.delete() == true) 1 else 0

    open fun openFileDescriptor(uri: Uri, mode: String): android.os.ParcelFileDescriptor? {
        val file = localFileOf(uri) ?: throw FileNotFoundException("No content provider: $uri")
        if (mode.startsWith("r") && !file.exists()) throw FileNotFoundException(file.path)
        return android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.parseMode(mode))
    }

    open fun releasePersistableUriPermission(uri: Uri, modeFlags: Int) {
        synchronized(LOCK) {
            val remaining = readGrants().filterNot { it.uri == uri }
            grantsFile().writeText(remaining.joinToString("") { "${it.modeFlags}\t${it.persistedTime}\t${it.uri}\n" })
        }
    }

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
