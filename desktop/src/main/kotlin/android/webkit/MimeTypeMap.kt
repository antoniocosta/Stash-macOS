package android.webkit

import java.net.URLConnection

/**
 * Desktop android.webkit.MimeTypeMap: common audio/image/text types first
 * (what upstream imports), then the JDK's content-type table.
 */
class MimeTypeMap private constructor() {

    fun getExtensionFromMimeType(mimeType: String?): String? {
        val m = mimeType?.lowercase() ?: return null
        return byMime[m]
    }

    fun getMimeTypeFromExtension(extension: String?): String? {
        val e = extension?.lowercase()?.removePrefix(".") ?: return null
        return byExt[e] ?: URLConnection.getFileNameMap().getContentTypeFor("x.$e")
    }

    fun hasExtension(extension: String?): Boolean = getMimeTypeFromExtension(extension) != null
    fun hasMimeType(mimeType: String?): Boolean = getExtensionFromMimeType(mimeType) != null

    companion object {
        private val table = listOf(
            "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "aac" to "audio/aac", "flac" to "audio/flac",
            "ogg" to "audio/ogg", "opus" to "audio/opus", "wav" to "audio/x-wav", "weba" to "audio/webm",
            "webm" to "video/webm", "mka" to "audio/x-matroska", "alac" to "audio/alac", "wma" to "audio/x-ms-wma",
            "jpg" to "image/jpeg", "png" to "image/png", "webp" to "image/webp", "gif" to "image/gif",
            "txt" to "text/plain", "lrc" to "application/lrc", "json" to "application/json",
            "zip" to "application/zip", "mp4" to "video/mp4",
        )
        private val byExt = table.toMap()
        private val byMime = table.reversed().associate { (e, m) -> m to e } +
            mapOf("audio/x-flac" to "flac", "audio/wav" to "wav", "audio/mp3" to "mp3", "audio/x-m4a" to "m4a")

        private val instance = MimeTypeMap()
        @JvmStatic fun getSingleton(): MimeTypeMap = instance
        @JvmStatic fun getFileExtensionFromUrl(url: String?): String =
            url?.substringBefore('#')?.substringBefore('?')?.substringAfterLast('/')
                ?.substringAfterLast('.', "")?.takeIf { it.matches(Regex("[a-zA-Z_0-9.\\-()%]+")) } ?: ""
    }
}
