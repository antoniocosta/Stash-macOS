package androidx.media3.datasource

import android.util.Log
import androidx.media3.common.C
import java.util.regex.Pattern

/** Desktop port of media3 1.9.2 `HttpUtil`: utility methods for HTTP. */
object HttpUtil {
    private const val TAG = "HttpUtil"
    private val CONTENT_RANGE_WITH_START_AND_END = Pattern.compile("bytes (\\d+)-(\\d+)/(?:\\d+|\\*)")
    private val CONTENT_RANGE_WITH_SIZE = Pattern.compile("bytes (?:(?:\\d+-\\d+)|\\*)/(\\d+)")

    /** Builds a `Range` header value, or null if the whole resource is requested. */
    @JvmStatic
    fun buildRangeRequestHeader(position: Long, length: Long): String? {
        if (position == 0L && length == LENGTH_UNSET_LONG) return null
        val sb = StringBuilder("bytes=").append(position).append('-')
        if (length != LENGTH_UNSET_LONG) sb.append(position + length - 1)
        return sb.toString()
    }

    /** The document size from a `Content-Range` header, or [C.LENGTH_UNSET]. */
    @JvmStatic
    fun getDocumentSize(contentRangeHeader: String?): Long {
        if (contentRangeHeader.isNullOrEmpty()) return LENGTH_UNSET_LONG
        val matcher = CONTENT_RANGE_WITH_SIZE.matcher(contentRangeHeader)
        return if (matcher.matches()) matcher.group(1)!!.toLong() else LENGTH_UNSET_LONG
    }

    /** The content length from `Content-Length` / `Content-Range` headers, or [C.LENGTH_UNSET]. */
    @JvmStatic
    fun getContentLength(contentLengthHeader: String?, contentRangeHeader: String?): Long {
        var contentLength = LENGTH_UNSET_LONG
        if (!contentLengthHeader.isNullOrEmpty()) {
            try {
                contentLength = contentLengthHeader.toLong()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Unexpected Content-Length [$contentLengthHeader]")
            }
        }
        if (!contentRangeHeader.isNullOrEmpty()) {
            val matcher = CONTENT_RANGE_WITH_START_AND_END.matcher(contentRangeHeader)
            if (matcher.matches()) {
                try {
                    val contentLengthFromRange = matcher.group(2)!!.toLong() - matcher.group(1)!!.toLong() + 1
                    if (contentLength < 0) {
                        contentLength = contentLengthFromRange
                    } else if (contentLength != contentLengthFromRange) {
                        Log.w(TAG, "Inconsistent headers [$contentLengthHeader] [$contentRangeHeader]")
                        contentLength = maxOf(contentLength, contentLengthFromRange)
                    }
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Unexpected Content-Range [$contentRangeHeader]")
                }
            }
        }
        return contentLength
    }
}
