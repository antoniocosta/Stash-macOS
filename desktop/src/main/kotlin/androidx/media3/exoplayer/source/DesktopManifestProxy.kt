package androidx.media3.exoplayer.source

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Loopback HTTP front for an HLS / DASH source, so ffmpeg (which demuxes the stream) reads the
 * manifest and every segment through the media source's own [DataSource] — upstream's gates
 * (Download mode), manifest refresh on 401/403/410 and request headers all apply, as they do when
 * media3 loads the chunks on Android.
 *
 * Each absolute URL in a playlist is rewritten to `/<session>/<n>/<file name>`; a relative
 * reference then resolves against that proxy directory and maps back to the same directory of
 * URL n. A request for URL n's own file name gets URL n exactly as the playlist (or upstream)
 * wrote it.
 */
object DesktopManifestProxy {
    class Session internal constructor(
        internal val token: String,
        internal val factory: DataSource.Factory,
        manifest: String,
    ) : AutoCloseable {
        internal val urls = CopyOnWriteArrayList<String>()

        /** The first load failure (ffmpeg only sees an HTTP error status). */
        @Volatile var error: IOException? = null
            internal set

        /** What ffmpeg opens. */
        val url: String = proxyUrl(manifest)

        internal fun proxyUrl(absolute: String): String {
            var index = urls.indexOf(absolute)
            if (index < 0) {
                urls += absolute
                index = urls.size - 1
            }
            val name = URI.create(absolute).rawPath.orEmpty().substringAfterLast('/').ifEmpty { "index" }
            return "http://127.0.0.1:${server.address.port}/$token/$index/$name"
        }

        override fun close() {
            sessions.remove(token)
        }
    }

    private val sessions = ConcurrentHashMap<String, Session>()
    private val random = SecureRandom()

    private val server: HttpServer by lazy {
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            createContext("/") { exchange -> exchange.use(::handle) }
            executor = Executors.newCachedThreadPool { r -> Thread(r, "StashManifestProxy").apply { isDaemon = true } }
            start()
        }
    }

    fun open(manifestUri: Uri, factory: DataSource.Factory): Session {
        val token = ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        return Session(token, factory, manifestUri.toString()).also { sessions[token] = it }
    }

    private fun handle(exchange: HttpExchange) {
        val raw = exchange.requestURI
        val parts = raw.rawPath.removePrefix("/").split('/', limit = 3)
        val session = parts.getOrNull(0)?.let(sessions::get)
        val base = parts.getOrNull(1)?.toIntOrNull()?.let { session?.urls?.getOrNull(it) }
        if (session == null || base == null || parts.size < 3) {
            exchange.sendResponseHeaders(404, -1)
            return
        }
        val rest = parts[2] + (raw.rawQuery?.let { "?$it" } ?: "")
        val ownName = URI.create(base).rawPath.orEmpty().substringAfterLast('/').ifEmpty { "index" }
        val target = if (rest == ownName) base else URI.create(base).resolve(rest).toString()

        // `bytes=0-` (what ffmpeg sends for every file) is the whole resource: a plain 200.
        val range = exchange.requestHeaders.getFirst("Range")?.let(::parseRange)?.takeUnless { it.first == 0L && it.second == null }
        val spec = DataSpec.Builder().setUri(Uri.parse(target))
            .setPosition(range?.first ?: 0L)
            .setLength(range?.second?.let { it - range.first + 1 } ?: C.LENGTH_UNSET.toLong())
            .build()
        val dataSource = session.factory.createDataSource()
        val length = try {
            dataSource.open(spec)
        } catch (e: IOException) {
            if (session.error == null) session.error = e
            runCatching { dataSource.close() }
            exchange.sendResponseHeaders(502, -1)
            return
        }
        try {
            val head = ByteArray(16)
            val headLen = readFully(dataSource, head)
            val text = String(head, 0, headLen, Charsets.UTF_8).trimStart('﻿', ' ', '\r', '\n')
            val isManifest = range == null && (text.startsWith("#EXTM3U") || text.startsWith("<?xml") || text.startsWith("<MPD"))
            if (isManifest) {
                val body = head.copyOf(headLen) + readAll(dataSource)
                val rewritten = rewrite(session, String(body, Charsets.UTF_8), dataSource.uri?.toString() ?: target)
                    .toByteArray(Charsets.UTF_8)
                toClient {
                    exchange.sendResponseHeaders(200, rewritten.size.toLong())
                    exchange.responseBody.write(rewritten)
                }
                return
            }
            toClient {
                if (range != null) {
                    val known = length != C.LENGTH_UNSET.toLong()
                    val end = if (known) range.first + length - 1 else range.second
                    // An open-ended range read to the end gives the resource's total size.
                    val total = if (known && range.second == null) "${range.first + length}" else "*"
                    exchange.responseHeaders.add("Content-Range", "bytes ${range.first}-${end ?: ""}/$total")
                    exchange.sendResponseHeaders(206, if (length != C.LENGTH_UNSET.toLong()) length else 0)
                } else {
                    exchange.sendResponseHeaders(200, if (length != C.LENGTH_UNSET.toLong()) length else 0)
                }
                exchange.responseBody.write(head, 0, headLen)
            }
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = dataSource.read(buf, 0, buf.size)
                if (n == C.RESULT_END_OF_INPUT) break
                toClient { exchange.responseBody.write(buf, 0, n) }
            }
        } catch (_: ClientGone) {
            // ffmpeg hung up (it probes, seeks and drops connections routinely): not a load error.
        } catch (e: IOException) {
            if (session.error == null) session.error = e
        } finally {
            runCatching { dataSource.close() }
        }
    }

    private class ClientGone : Exception()

    /** A failed write to ffmpeg's connection, told apart from a failed read of the source. */
    private inline fun toClient(block: () -> Unit) {
        try {
            block()
        } catch (_: IOException) {
            throw ClientGone()
        }
    }

    private val absoluteUrl = Regex("""https?://[^\s"'<>]+""")
    private val uriAttribute = Regex("""URI="([^"]+)"""")

    /**
     * HLS: every URI (segment lines and `URI="…"` attributes) is resolved against the URL the
     * playlist was really served from (redirects included), then proxied. DASH: absolute URLs are
     * proxied (XML-escaped `&amp;` kept); relative ones resolve against the proxied MPD.
     */
    private fun rewrite(session: Session, body: String, servedUrl: String): String {
        val served = URI.create(servedUrl)
        fun proxied(ref: String) = session.proxyUrl(served.resolve(ref).toString())
        if (body.trimStart('\uFEFF').startsWith("#EXTM3U")) {
            return body.lines().joinToString("\n") { line ->
                when {
                    line.isBlank() -> line
                    line.startsWith("#") -> uriAttribute.replace(line) { m -> "URI=\"${proxied(m.groupValues[1])}\"" }
                    else -> proxied(line.trim())
                }
            }
        }
        return absoluteUrl.replace(body) { m ->
            val escaped = m.value.contains("&amp;")
            val proxy = session.proxyUrl(m.value.replace("&amp;", "&"))
            if (escaped) proxy.replace("&", "&amp;") else proxy
        }
    }

    private fun parseRange(header: String): Pair<Long, Long?>? {
        val m = Regex("""bytes=(\d+)-(\d*)""").matchEntire(header.trim()) ?: return null
        return m.groupValues[1].toLong() to m.groupValues[2].takeIf { it.isNotEmpty() }?.toLong()
    }

    private fun readFully(source: DataSource, buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val n = source.read(buf, total, buf.size - total)
            if (n == C.RESULT_END_OF_INPUT) break
            total += n
        }
        return total
    }

    private fun readAll(source: DataSource): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = source.read(buf, 0, buf.size)
            if (n == C.RESULT_END_OF_INPUT) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
