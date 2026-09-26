package androidx.documentfile.provider

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * Desktop shim for androidx DocumentFile, backed by java.io.File. `file:` URIs (and scheme-less
 * absolute paths) are served; any other scheme has no provider on desktop, so the factories
 * return null. createFile/createDirectory follow ExternalStorageProvider naming (the SAF
 * provider upstream targets): extension kept when it matches the MIME, otherwise the MIME's
 * extension is appended, and collisions become "name (1).ext".
 */
abstract class DocumentFile internal constructor(val parentFile: DocumentFile?) {

    abstract val uri: Uri
    abstract val name: String?
    abstract val isDirectory: Boolean
    abstract val isFile: Boolean
    abstract fun exists(): Boolean
    abstract fun length(): Long
    abstract fun lastModified(): Long
    abstract fun canRead(): Boolean
    abstract fun canWrite(): Boolean
    abstract fun delete(): Boolean
    abstract fun listFiles(): Array<DocumentFile>
    abstract fun createFile(mimeType: String, displayName: String): DocumentFile?
    abstract fun createDirectory(displayName: String): DocumentFile?

    open fun findFile(displayName: String): DocumentFile? = listFiles().firstOrNull { it.name == displayName }

    companion object {
        @JvmStatic fun fromFile(file: File): DocumentFile = FileDocumentFile(null, file)

        @JvmStatic fun fromSingleUri(context: Context, singleUri: Uri): DocumentFile? =
            ContentResolver.localFileOf(singleUri)?.let { FileDocumentFile(null, it) }

        @JvmStatic fun fromTreeUri(context: Context, treeUri: Uri): DocumentFile? =
            ContentResolver.localFileOf(treeUri)?.let { FileDocumentFile(null, it) }
    }
}

private class FileDocumentFile(parent: DocumentFile?, private val file: File) : DocumentFile(parent) {
    override val uri: Uri get() = Uri.fromFile(file)
    override val name: String get() = file.name
    override val isDirectory: Boolean get() = file.isDirectory
    override val isFile: Boolean get() = file.isFile
    override fun exists() = file.exists()
    override fun length() = file.length()
    override fun lastModified() = file.lastModified()
    override fun canRead() = file.canRead()
    override fun canWrite() = file.canWrite()
    override fun delete(): Boolean = if (file.isDirectory) file.deleteRecursively() else file.delete()

    override fun listFiles(): Array<DocumentFile> =
        file.listFiles().orEmpty().map<File, DocumentFile> { FileDocumentFile(this, it) }.toTypedArray()

    override fun createFile(mimeType: String, displayName: String): DocumentFile? {
        val (base, ext) = MimeNames.split(mimeType, displayName)
        return create(base, ext) { it.createNewFile() }
    }

    override fun createDirectory(displayName: String): DocumentFile? = create(displayName, "") { it.mkdir() }

    private fun create(base: String, ext: String, make: (File) -> Boolean): DocumentFile? {
        val suffix = if (ext.isEmpty()) "" else ".$ext"
        var target = File(file, base + suffix)
        var n = 0
        while (target.exists()) {
            if (++n >= 32) return null
            target = File(file, "$base ($n)$suffix")
        }
        return try {
            if (make(target)) FileDocumentFile(this, target) else null
        } catch (e: IOException) {
            null
        }
    }
}

/** The subset of Android's MimeMap needed for provider-style naming (FileUtils.splitFileName). */
private object MimeNames {
    private const val UNKNOWN = "application/octet-stream"
    private val extToMime = mapOf(
        "m4a" to "audio/mp4", "mp4" to "video/mp4", "aac" to "audio/aac", "mp3" to "audio/mpeg",
        "flac" to "audio/flac", "ogg" to "audio/ogg", "oga" to "audio/ogg", "opus" to "audio/ogg",
        "wav" to "audio/x-wav", "webm" to "video/webm", "txt" to "text/plain", "json" to "application/json",
        "zip" to "application/zip", "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
    )
    private val mimeToExt = mapOf(
        "audio/mp4" to "m4a", "video/mp4" to "mp4", "audio/aac" to "aac", "audio/mpeg" to "mp3",
        "audio/flac" to "flac", "audio/ogg" to "ogg", "audio/x-wav" to "wav", "audio/wav" to "wav",
        "video/webm" to "webm", "text/plain" to "txt", "application/json" to "json",
        "application/zip" to "zip", "image/jpeg" to "jpg", "image/png" to "png",
    )

    fun split(mimeType: String, displayName: String): Pair<String, String> {
        val dot = displayName.lastIndexOf('.')
        val name = if (dot >= 0) displayName.substring(0, dot) else displayName
        val ext = if (dot >= 0) displayName.substring(dot + 1) else null
        val mimeFromExt = ext?.let { extToMime[it.lowercase()] } ?: UNKNOWN
        val extFromMime = if (mimeType == UNKNOWN) null else mimeToExt[mimeType]
        return if (mimeType == mimeFromExt || ext == extFromMime) name to ext.orEmpty()
        else displayName to extFromMime.orEmpty()
    }
}
