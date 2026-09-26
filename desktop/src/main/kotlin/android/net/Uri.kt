package android.net

import android.os.Parcelable
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Desktop shim for android.net.Uri: a string-backed URI parsed lazily with AOSP `StringUri`
 * rules (naive `:` scheme split, opaque vs hierarchical, percent-decoding accessors).
 */
abstract class Uri internal constructor() : Parcelable, Comparable<Uri> {

    abstract val scheme: String?
    abstract val schemeSpecificPart: String?
    abstract val encodedSchemeSpecificPart: String?
    abstract val authority: String?
    abstract val encodedAuthority: String?
    abstract val userInfo: String?
    abstract val host: String?
    abstract val port: Int
    abstract val path: String?
    abstract val encodedPath: String?
    abstract val query: String?
    abstract val encodedQuery: String?
    abstract val fragment: String?
    abstract val encodedFragment: String?
    abstract val pathSegments: List<String>
    abstract val lastPathSegment: String?
    abstract val isHierarchical: Boolean
    val isOpaque: Boolean get() = !isHierarchical
    val isRelative: Boolean get() = scheme == null
    val isAbsolute: Boolean get() = !isRelative

    /** First decoded value of query parameter [key], "" if present without value, null if absent. */
    abstract fun getQueryParameter(key: String): String?

    /** A Builder seeded with this URI's encoded components (opaque part for opaque URIs), as in AOSP. */
    fun buildUpon(): Builder =
        if (isHierarchical) {
            Builder().scheme(scheme).encodedAuthority(encodedAuthority).encodedPath(encodedPath)
                .encodedQuery(encodedQuery).encodedFragment(encodedFragment)
        } else {
            Builder().scheme(scheme).encodedOpaquePart(encodedSchemeSpecificPart).encodedFragment(encodedFragment)
        }

    abstract override fun toString(): String
    override fun equals(other: Any?): Boolean = other is Uri && toString() == other.toString()
    override fun hashCode(): Int = toString().hashCode()
    override fun compareTo(other: Uri): Int = toString().compareTo(other.toString())

    /**
     * AOSP Uri.Builder. Components are held encoded; decoded setters encode with [Uri.encode]
     * (paths keep "/"). Setting authority/path/query clears an opaque part; build() yields an
     * opaque URI when an opaque part is set, otherwise a hierarchical one whose path is made
     * absolute when a scheme or authority is present.
     */
    class Builder {
        private var scheme: String? = null
        private var opaquePart: String? = null
        private var authority: String? = null
        private var path: String? = null
        private var query: String? = null
        private var fragment: String? = null

        fun scheme(scheme: String?): Builder = apply { this.scheme = scheme }

        fun opaquePart(opaquePart: String?): Builder = encodedOpaquePart(encode(opaquePart, null))
        fun encodedOpaquePart(opaquePart: String?): Builder = apply { this.opaquePart = opaquePart }

        fun authority(authority: String?): Builder = encodedAuthority(encode(authority, null))
        fun encodedAuthority(authority: String?): Builder = apply { opaquePart = null; this.authority = authority }

        fun path(path: String?): Builder = encodedPath(encode(path, "/"))
        fun encodedPath(path: String?): Builder = apply { opaquePart = null; this.path = path }

        fun appendPath(newSegment: String): Builder = appendEncodedPath(encode(newSegment, null)!!)
        fun appendEncodedPath(newSegment: String): Builder = apply {
            opaquePart = null
            val old = path.orEmpty()
            path = when {
                old.isEmpty() -> "/$newSegment"
                old.endsWith('/') -> old + newSegment
                else -> "$old/$newSegment"
            }
        }

        fun query(query: String?): Builder = encodedQuery(encode(query, null))
        fun encodedQuery(query: String?): Builder = apply { opaquePart = null; this.query = query }

        fun appendQueryParameter(key: String, value: String?): Builder = apply {
            opaquePart = null
            val param = encode(key, null) + "=" + encode(value, null)
            val old = query
            query = if (old.isNullOrEmpty()) param else "$old&$param"
        }

        fun clearQuery(): Builder = apply { query = null }

        fun fragment(fragment: String?): Builder = encodedFragment(encode(fragment, null))
        fun encodedFragment(fragment: String?): Builder = apply { this.fragment = fragment }

        fun build(): Uri {
            val sb = StringBuilder()
            val opaque = opaquePart
            if (opaque != null) {
                val s = scheme ?: throw UnsupportedOperationException("An opaque URI must have a scheme.")
                sb.append(s).append(':').append(opaque)
            } else {
                scheme?.let { sb.append(it).append(':') }
                authority?.let { sb.append("//").append(it) }
                var p = path.orEmpty()
                if ((scheme != null || authority != null) && p.isNotEmpty() && !p.startsWith("/")) p = "/$p"
                sb.append(p)
                query?.takeIf { it.isNotEmpty() }?.let { sb.append('?').append(it) }
            }
            fragment?.takeIf { it.isNotEmpty() }?.let { sb.append('#').append(it) }
            return StringUri(sb.toString())
        }

        override fun toString(): String = build().toString()
    }

    companion object {
        @JvmField val EMPTY: Uri = StringUri("")

        @JvmStatic fun parse(uriString: String): Uri = StringUri(uriString)

        /** `file://` + absolute path encoded with "/" allowed, exactly like AOSP. */
        @JvmStatic fun fromFile(file: File): Uri = StringUri("file://" + encode(file.absolutePath, "/"))

        @JvmStatic fun encode(s: String?): String? = encode(s, null)

        @JvmStatic fun encode(s: String?, allow: String?): String? {
            if (s == null) return null
            val out = StringBuilder(s.length)
            for (b in s.toByteArray(Charsets.UTF_8)) {
                val c = (b.toInt() and 0xFF).toChar()
                if (b >= 0 && (c.isLetterOrDigit() && c.code < 128 || c in "_-!.~'()*" ||
                        (allow != null && allow.indexOf(c) >= 0))
                ) out.append(c)
                else out.append('%').append(HEX[(b.toInt() shr 4) and 0xF]).append(HEX[b.toInt() and 0xF])
            }
            return out.toString()
        }

        @JvmStatic fun decode(s: String?): String? = s?.let { decode(it, convertPlus = false) }

        private const val HEX = "0123456789ABCDEF"

        /** AOSP UriCodec.decode(throwOnFailure = false): malformed escapes become U+FFFD. */
        internal fun decode(s: String, convertPlus: Boolean): String {
            if (s.indexOf('%') < 0 && !(convertPlus && s.indexOf('+') >= 0)) return s
            val sb = StringBuilder(s.length)
            val bytes = ByteArrayOutputStream()
            fun flush() {
                if (bytes.size() > 0) { sb.append(bytes.toByteArray().toString(Charsets.UTF_8)); bytes.reset() }
            }
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '%') {
                    val hi = if (i + 1 < s.length) Character.digit(s[i + 1], 16) else -1
                    val lo = if (i + 2 < s.length) Character.digit(s[i + 2], 16) else -1
                    if (hi < 0 || lo < 0) { flush(); sb.append('\uFFFD'); i = minOf(i + 3, s.length); continue }
                    bytes.write((hi shl 4) or lo); i += 3; continue
                }
                flush()
                sb.append(if (convertPlus && c == '+') ' ' else c)
                i++
            }
            flush()
            return sb.toString()
        }
    }
}

private class StringUri(private val uriString: String) : Uri() {

    private val ssi: Int = uriString.indexOf(':')
    private val fsi: Int = uriString.indexOf('#', maxOf(ssi, 0))

    override val scheme: String? get() = if (ssi == -1) null else uriString.substring(0, ssi)

    override val isHierarchical: Boolean
        get() = ssi == -1 || (uriString.length != ssi + 1 && uriString[ssi + 1] == '/')

    override val encodedSchemeSpecificPart: String?
        get() = uriString.substring(ssi + 1, if (fsi == -1) uriString.length else fsi)
    override val schemeSpecificPart: String? get() = encodedSchemeSpecificPart?.let { decode(it, false) }

    override val encodedAuthority: String?
        get() {
            val len = uriString.length
            if (len > ssi + 2 && uriString[ssi + 1] == '/' && uriString[ssi + 2] == '/') {
                var end = ssi + 3
                while (end < len && uriString[end] !in "/\\?#") end++
                return uriString.substring(ssi + 3, end)
            }
            return null
        }
    override val authority: String? get() = encodedAuthority?.let { decode(it, false) }

    override val userInfo: String?
        get() = encodedAuthority?.let { a -> a.lastIndexOf('@').takeIf { it >= 0 }?.let { decode(a.substring(0, it), false) } }

    private fun portSeparator(a: String): Int {
        val i = a.lastIndexOf(':')
        if (i < 0 || a.indexOf(']', i) >= 0) return -1
        return if (a.substring(i + 1).all { it in '0'..'9' }) i else -1
    }

    override val host: String?
        get() {
            val a = encodedAuthority ?: return null
            val start = a.lastIndexOf('@') + 1
            val ps = portSeparator(a)
            return decode(a.substring(start, if (ps == -1) a.length else ps), false)
        }

    override val port: Int
        get() {
            val a = encodedAuthority ?: return -1
            val ps = portSeparator(a)
            return if (ps == -1) -1 else a.substring(ps + 1).toIntOrNull() ?: -1
        }

    override val encodedPath: String?
        get() {
            val len = uriString.length
            if (ssi > -1 && (ssi + 1 == len || uriString[ssi + 1] != '/')) return null // opaque
            var start: Int
            if (len > ssi + 2 && uriString[ssi + 1] == '/' && uriString[ssi + 2] == '/') {
                start = ssi + 3
                while (start < len) {
                    when (uriString[start]) {
                        '?', '#' -> return ""
                        '/', '\\' -> break
                    }
                    start++
                }
            } else {
                start = ssi + 1
            }
            var end = start
            while (end < len && uriString[end] != '?' && uriString[end] != '#') end++
            return uriString.substring(start, end)
        }
    override val path: String? get() = encodedPath?.let { decode(it, false) }

    override val pathSegments: List<String>
        get() = encodedPath.orEmpty().split('/').filter { it.isNotEmpty() }.map { decode(it, false) }
    override val lastPathSegment: String? get() = pathSegments.lastOrNull()

    override val encodedQuery: String?
        get() {
            val qsi = uriString.indexOf('?', maxOf(ssi, 0))
            if (qsi == -1) return null
            return when {
                fsi == -1 -> uriString.substring(qsi + 1)
                fsi < qsi -> null
                else -> uriString.substring(qsi + 1, fsi)
            }
        }
    override val query: String? get() = encodedQuery?.let { decode(it, false) }

    override val encodedFragment: String? get() = if (fsi == -1) null else uriString.substring(fsi + 1)
    override val fragment: String? get() = encodedFragment?.let { decode(it, false) }

    override fun getQueryParameter(key: String): String? {
        if (isOpaque) throw UnsupportedOperationException("This isn't a hierarchical URI.")
        val q = encodedQuery ?: return null
        val encodedKey = encode(key, null)
        for (param in q.split('&')) {
            val eq = param.indexOf('=')
            val name = if (eq == -1) param else param.substring(0, eq)
            if (name == encodedKey) return if (eq == -1) "" else decode(param.substring(eq + 1), true)
        }
        return null
    }

    override fun toString(): String = uriString
}
