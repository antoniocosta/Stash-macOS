package android.util

/** Desktop shim for android.util.Base64 (same flags and semantics). */
object Base64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8
    const val NO_CLOSE = 16

    @JvmStatic fun decode(str: String, flags: Int): ByteArray = decode(str.toByteArray(Charsets.US_ASCII), flags)

    @JvmStatic fun decode(input: ByteArray, flags: Int): ByteArray = decode(input, 0, input.size, flags)

    @JvmStatic fun decode(input: ByteArray, offset: Int, len: Int, flags: Int): ByteArray {
        // Android's decoder ignores whitespace and accepts missing padding.
        val cleaned = String(input, offset, len, Charsets.US_ASCII).filterNot { it.isWhitespace() }
        return try {
            (if (flags and URL_SAFE != 0) java.util.Base64.getUrlDecoder() else java.util.Base64.getDecoder())
                .decode(cleaned)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("bad base-64", e)
        }
    }

    @JvmStatic fun encodeToString(input: ByteArray, flags: Int): String =
        String(encode(input, flags), Charsets.US_ASCII)

    @JvmStatic fun encodeToString(input: ByteArray, offset: Int, len: Int, flags: Int): String =
        String(encode(input, offset, len, flags), Charsets.US_ASCII)

    @JvmStatic fun encode(input: ByteArray, flags: Int): ByteArray = encode(input, 0, input.size, flags)

    @JvmStatic fun encode(input: ByteArray, offset: Int, len: Int, flags: Int): ByteArray {
        var enc = if (flags and URL_SAFE != 0) java.util.Base64.getUrlEncoder() else java.util.Base64.getEncoder()
        if (flags and NO_PADDING != 0) enc = enc.withoutPadding()
        val raw = enc.encodeToString(input.copyOfRange(offset, offset + len))
        if (flags and NO_WRAP != 0) return raw.toByteArray(Charsets.US_ASCII)
        // Android wraps at 76 chars and terminates every line (including the last).
        val eol = if (flags and CRLF != 0) "\r\n" else "\n"
        val sb = StringBuilder()
        raw.chunked(76).forEach { sb.append(it).append(eol) }
        return sb.toString().toByteArray(Charsets.US_ASCII)
    }
}
