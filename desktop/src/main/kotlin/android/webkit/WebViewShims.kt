package android.webkit

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class CookieManager private constructor() {
    private val cookiesByHost = ConcurrentHashMap<String, MutableMap<String, String>>()

    @Volatile
    private var acceptCookies: Boolean = true

    fun setAcceptCookie(accept: Boolean) {
        acceptCookies = accept
    }

    fun setAcceptThirdPartyCookies(@Suppress("UNUSED_PARAMETER") webview: WebView?, accept: Boolean) {
        acceptCookies = accept
    }

    fun removeAllCookies(callback: ((Boolean) -> Unit)?) {
        cookiesByHost.clear()
        callback?.invoke(true)
    }

    fun flush() {}

    fun setCookie(url: String, value: String) {
        if (!acceptCookies) return
        val host = extractHost(url)
        val map = cookiesByHost.computeIfAbsent(host) { ConcurrentHashMap() }
        val attrs = setOf("path", "domain", "expires", "max-age", "secure", "httponly", "samesite")
        for (part in value.split(';')) {
            val trimmed = part.trim()
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                val k = trimmed.substring(0, eq).trim()
                val v = trimmed.substring(eq + 1).trim()
                if (k.lowercase() !in attrs) {
                    map[k] = v
                }
            }
        }
    }

    fun getCookie(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val host = extractHost(url)
        val merged = LinkedHashMap<String, String>()
        for ((storedHost, map) in cookiesByHost) {
            if (host == storedHost || host.endsWith(".$storedHost") || storedHost.endsWith(".$host") || storedHost == "*") {
                merged.putAll(map)
            }
        }
        if (merged.isEmpty()) return null
        return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun extractHost(url: String): String =
        runCatching { URI(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: "*"

    companion object {
        private val INSTANCE = CookieManager()

        @JvmStatic
        fun getInstance(): CookieManager = INSTANCE
    }
}

class WebSettings {
    var javaScriptEnabled: Boolean = false
    var domStorageEnabled: Boolean = false
    var databaseEnabled: Boolean = false
    var useWideViewPort: Boolean = false
    var loadWithOverviewMode: Boolean = false
    var builtInZoomControls: Boolean = false
    var displayZoomControls: Boolean = false
    var cacheMode: Int = LOAD_DEFAULT
    var mixedContentMode: Int = MIXED_CONTENT_NEVER_ALLOW
    var mediaPlaybackRequiresUserGesture: Boolean = true
    var userAgentString: String =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    fun setSupportZoom(@Suppress("UNUSED_PARAMETER") support: Boolean) {}

    companion object {
        const val LOAD_DEFAULT = -1
        const val LOAD_NORMAL = 0
        const val LOAD_CACHE_ELSE_NETWORK = 1
        const val LOAD_NO_CACHE = 2
        const val LOAD_CACHE_ONLY = 3

        const val MIXED_CONTENT_ALWAYS_ALLOW = 0
        const val MIXED_CONTENT_NEVER_ALLOW = 1
        const val MIXED_CONTENT_COMPATIBILITY_MODE = 2
    }
}

interface WebResourceRequest {
    val url: Uri
    val isForMainFrame: Boolean get() = true
}

internal class SimpleWebResourceRequest(
    override val url: Uri,
) : WebResourceRequest

open class WebViewClient {
    open fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {}
    open fun onPageFinished(view: WebView?, url: String?) {}
    open fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {}
    open fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
}

open class WebView(context: Context) : ViewGroup(context) {
    val settings: WebSettings = WebSettings()
    var webViewClient: WebViewClient = WebViewClient()

    private val history = ArrayList<String>()

    var currentUrlState by mutableStateOf<String?>(null)
        internal set

    val url: String?
        get() = currentUrlState

    @Volatile
    internal var simulatedJsResult: String = "null"

    open fun loadUrl(url: String) {
        currentUrlState?.let { prev ->
            if (prev != url) history.add(prev)
        }
        currentUrlState = url
        webViewClient.onPageStarted(this, url, null)
        webViewClient.doUpdateVisitedHistory(this, url, false)
        webViewClient.onPageFinished(this, url)
    }

    open fun canGoBack(): Boolean = history.isNotEmpty()

    open fun goBack() {
        if (history.isNotEmpty()) {
            val prev = history.removeAt(history.lastIndex)
            currentUrlState = prev
            webViewClient.onPageStarted(this, prev, null)
            webViewClient.onPageFinished(this, prev)
        }
    }

    open fun reload() {
        val u = currentUrlState ?: return
        webViewClient.onPageStarted(this, u, null)
        webViewClient.doUpdateVisitedHistory(this, u, true)
        webViewClient.onPageFinished(this, u)
    }

    open fun stopLoading() {}

    open fun clearHistory() {
        history.clear()
    }

    open fun clearCache(includeDiskFiles: Boolean) {}

    open fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        resultCallback?.invoke(simulatedJsResult)
    }

    open fun destroy() {}

    internal fun submitCookiesOrToken(input: String, targetUrl: String?) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val effectiveUrl = targetUrl ?: currentUrlState ?: "https://localhost"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val req = SimpleWebResourceRequest(Uri.parse(trimmed))
            val handled = webViewClient.shouldOverrideUrlLoading(this, req)
            if (!handled) {
                loadUrl(trimmed)
            }
            return
        }
        val cookieManager = CookieManager.getInstance()
        if ('=' in trimmed) {
            cookieManager.setCookie(effectiveUrl, trimmed)
            if (effectiveUrl.contains("spotify.com", ignoreCase = true)) {
                cookieManager.setCookie("https://open.spotify.com", trimmed)
                cookieManager.setCookie("https://accounts.spotify.com", trimmed)
            }
        } else {
            val clean = trimmed.removeSurrounding("\"").removeSurrounding("'").trim()
            simulatedJsResult = "\"$clean\""
            // Also register as Spotify sp_dc cookie in case a bare sp_dc value was pasted
            cookieManager.setCookie(effectiveUrl, "sp_dc=$clean")
            cookieManager.setCookie("https://open.spotify.com", "sp_dc=$clean")
            cookieManager.setCookie("https://accounts.spotify.com", "sp_dc=$clean")
        }
        webViewClient.doUpdateVisitedHistory(this, effectiveUrl, false)
        webViewClient.onPageFinished(this, effectiveUrl)
    }
}
