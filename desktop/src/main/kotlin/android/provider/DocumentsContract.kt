package android.provider

import android.net.Uri

/**
 * Desktop shim for android.provider.DocumentsContract: pure URI parsing, same rules as AOSP
 * (`.../document/<id>`, `.../tree/<id>[/document/<id>]`). Non-document URIs (e.g. desktop
 * `file:` trees) throw [IllegalArgumentException] exactly as on Android.
 */
object DocumentsContract {
    private const val PATH_DOCUMENT = "document"
    private const val PATH_TREE = "tree"

    @JvmStatic
    fun getDocumentId(documentUri: Uri): String {
        val paths = documentUri.pathSegments
        if (paths.size >= 2 && PATH_DOCUMENT == paths[0]) return paths[1]
        if (paths.size >= 4 && PATH_TREE == paths[0] && PATH_DOCUMENT == paths[2]) return paths[3]
        throw IllegalArgumentException("Invalid URI: $documentUri")
    }

    @JvmStatic
    fun getTreeDocumentId(documentUri: Uri): String {
        val paths = documentUri.pathSegments
        if (paths.size >= 2 && PATH_TREE == paths[0]) return paths[1]
        throw IllegalArgumentException("Invalid URI: $documentUri")
    }
}
