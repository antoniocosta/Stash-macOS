package android.provider

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Desktop shim for android.provider.DocumentsContract. Content-provider URIs parse with AOSP's rules
 * (`.../document/<id>`, `.../tree/<id>[/document/<id>]`). The desktop's own document trees are the
 * `file:` folders the macOS folder picker returns: there a document id is the absolute path, a
 * document URI is the file's `file:` URI, and a children URI is the folder's `file:` URI with
 * `?children` — which [ContentResolver.query] lists from the filesystem.
 */
object DocumentsContract {
    private const val PATH_DOCUMENT = "document"
    private const val PATH_TREE = "tree"
    internal const val CHILDREN_QUERY = "children"

    const val EXTRA_LOADING = "loading"
    const val EXTRA_INFO = "info"
    const val EXTRA_ERROR = "error"
    const val EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI"

    object Document {
        const val COLUMN_DOCUMENT_ID = "document_id"
        const val COLUMN_MIME_TYPE = "mime_type"
        const val COLUMN_DISPLAY_NAME = "_display_name"
        const val COLUMN_SUMMARY = "summary"
        const val COLUMN_LAST_MODIFIED = "last_modified"
        const val COLUMN_ICON = "icon"
        const val COLUMN_FLAGS = "flags"
        const val COLUMN_SIZE = "_size"
        const val MIME_TYPE_DIR = "vnd.android.document/directory"
        const val FLAG_SUPPORTS_WRITE = 0x2
        const val FLAG_SUPPORTS_DELETE = 0x4
        const val FLAG_DIR_SUPPORTS_CREATE = 0x8
        const val FLAG_SUPPORTS_RENAME = 0x40
    }

    private fun isFile(uri: Uri) = uri.scheme == ContentResolver.SCHEME_FILE

    @JvmStatic
    fun isDocumentUri(context: Context?, uri: Uri?): Boolean {
        uri ?: return false
        if (isFile(uri)) return uri.getQueryParameter(CHILDREN_QUERY) == null
        val paths = uri.pathSegments
        return (paths.size == 2 && paths[0] == PATH_DOCUMENT) || (paths.size == 4 && paths[0] == PATH_TREE && paths[2] == PATH_DOCUMENT)
    }

    @JvmStatic
    fun isTreeUri(uri: Uri): Boolean = isFile(uri) || uri.pathSegments.let { it.size >= 2 && it[0] == PATH_TREE }

    @JvmStatic
    fun getDocumentId(documentUri: Uri): String {
        if (isFile(documentUri)) return documentUri.path ?: throw IllegalArgumentException("Invalid URI: $documentUri")
        val paths = documentUri.pathSegments
        if (paths.size >= 2 && PATH_DOCUMENT == paths[0]) return paths[1]
        if (paths.size >= 4 && PATH_TREE == paths[0] && PATH_DOCUMENT == paths[2]) return paths[3]
        throw IllegalArgumentException("Invalid URI: $documentUri")
    }

    @JvmStatic
    fun getTreeDocumentId(documentUri: Uri): String {
        if (isFile(documentUri)) return documentUri.path ?: throw IllegalArgumentException("Invalid URI: $documentUri")
        val paths = documentUri.pathSegments
        if (paths.size >= 2 && PATH_TREE == paths[0]) return paths[1]
        throw IllegalArgumentException("Invalid URI: $documentUri")
    }

    @JvmStatic
    fun buildDocumentUriUsingTree(treeUri: Uri, documentId: String): Uri =
        if (isFile(treeUri)) Uri.fromFile(File(documentId))
        else treeUri.buildUpon().path(null).appendPath(PATH_TREE).appendPath(getTreeDocumentId(treeUri))
            .appendPath(PATH_DOCUMENT).appendPath(documentId).build()

    @JvmStatic
    fun buildChildDocumentsUriUsingTree(treeUri: Uri, parentDocumentId: String): Uri =
        if (isFile(treeUri)) Uri.fromFile(File(parentDocumentId)).buildUpon().appendQueryParameter(CHILDREN_QUERY, "1").build()
        else treeUri.buildUpon().path(null).appendPath(PATH_TREE).appendPath(getTreeDocumentId(treeUri))
            .appendPath(PATH_DOCUMENT).appendPath(parentDocumentId).appendPath("children").build()

    @JvmStatic
    fun deleteDocument(content: ContentResolver, documentUri: Uri): Boolean {
        val file = ContentResolver.localFileOf(documentUri) ?: throw java.io.FileNotFoundException("No document provider: $documentUri")
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    @JvmStatic
    fun renameDocument(content: ContentResolver, documentUri: Uri, displayName: String): Uri? {
        val file = ContentResolver.localFileOf(documentUri) ?: throw java.io.FileNotFoundException("No document provider: $documentUri")
        val target = File(file.parentFile, displayName)
        return if (file.renameTo(target)) Uri.fromFile(target) else null
    }

    @JvmStatic
    fun createDocument(content: ContentResolver, parentDocumentUri: Uri, mimeType: String, displayName: String): Uri? {
        val dir = ContentResolver.localFileOf(parentDocumentUri) ?: return null
        val target = File(dir, displayName)
        if (mimeType == Document.MIME_TYPE_DIR) target.mkdirs() else target.createNewFile()
        return Uri.fromFile(target)
    }
}

/** Desktop shim for android.provider.OpenableColumns. */
object OpenableColumns {
    const val DISPLAY_NAME = "_display_name"
    const val SIZE = "_size"
}

/** Desktop shim for android.provider.MediaStore: the Downloads collection is ~/Downloads. */
object MediaStore {
    const val VOLUME_EXTERNAL = "external"
    const val VOLUME_EXTERNAL_PRIMARY = "external_primary"
    internal const val DOWNLOADS_AUTHORITY_PATH = "media/external/downloads"

    interface MediaColumns {
        companion object {
            const val DISPLAY_NAME = "_display_name"
            const val MIME_TYPE = "mime_type"
            const val RELATIVE_PATH = "relative_path"
            const val IS_PENDING = "is_pending"
            const val SIZE = "_size"
            const val DATE_ADDED = "date_added"
            const val DATE_MODIFIED = "date_modified"
        }
    }

    object Downloads {
        @JvmField val EXTERNAL_CONTENT_URI: Uri = Uri.parse("content://media/external/downloads")
        const val DISPLAY_NAME = MediaColumns.DISPLAY_NAME
        const val MIME_TYPE = MediaColumns.MIME_TYPE
        const val RELATIVE_PATH = MediaColumns.RELATIVE_PATH
        const val IS_PENDING = MediaColumns.IS_PENDING

        @JvmStatic fun getContentUri(volumeName: String): Uri = EXTERNAL_CONTENT_URI
    }
}
