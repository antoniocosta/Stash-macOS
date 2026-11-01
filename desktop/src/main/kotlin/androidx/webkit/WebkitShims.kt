package androidx.webkit

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.stash.desktop.res.DesktopAssets

/*
 * Desktop shims for androidx.webkit. The desktop has no Chromium WebView, so the features behind
 * WebViewFeature (web message listeners, renderer clients) report unsupported — upstream's own
 * answer for an old WebView, which Your record shows as a notice while its native Mixes tab works.
 * WebViewAssetLoader is real (it serves the module's bundled assets).
 */

object WebViewFeature {
    const val WEB_MESSAGE_LISTENER = "WEB_MESSAGE_LISTENER"
    const val WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE = "WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE"
    const val DOCUMENT_START_SCRIPT = "DOCUMENT_START_SCRIPT"

    @JvmStatic fun isFeatureSupported(feature: String): Boolean = false
}

class WebMessageCompat(val data: String?)

abstract class JavaScriptReplyProxy {
    abstract fun postMessage(message: String)
}

abstract class WebViewRenderProcess {
    abstract fun terminate(): Boolean
}

abstract class WebViewRenderProcessClient {
    abstract fun onRenderProcessUnresponsive(view: WebView, renderer: WebViewRenderProcess?)
    abstract fun onRenderProcessResponsive(view: WebView, renderer: WebViewRenderProcess?)
}

object WebViewCompat {
    fun interface WebMessageListener {
        fun onPostMessage(view: WebView, message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy)
    }

    @JvmStatic
    fun addWebMessageListener(webView: WebView, jsObjectName: String, allowedOriginRules: Set<String>, listener: WebMessageListener): Unit =
        throw UnsupportedOperationException("WEB_MESSAGE_LISTENER is not supported on the desktop")

    @JvmStatic
    fun removeWebMessageListener(webView: WebView, jsObjectName: String) {}

    @JvmStatic
    fun setWebViewRenderProcessClient(webView: WebView, client: WebViewRenderProcessClient?) {}
}

/** androidx.webkit's WebViewAssetLoader: `https://appassets.androidplatform.net/<prefix>/...` to path handlers. */
class WebViewAssetLoader private constructor(
    private val domain: String,
    private val handlers: List<Pair<String, PathHandler>>,
) {
    fun interface PathHandler {
        fun handle(path: String): WebResourceResponse?
    }

    /** Serves the app's assets (every upstream module's src/main/assets, merged as AGP merges them). */
    class AssetsPathHandler(context: Context) : PathHandler {
        override fun handle(path: String): WebResourceResponse? {
            val stream = DesktopAssets.open(path) ?: return null
            return WebResourceResponse(mimeFor(path), null, stream)
        }
    }

    fun shouldInterceptRequest(url: Uri): WebResourceResponse? {
        if (url.scheme != "https" || url.host != domain) return null
        val path = url.path ?: return null
        for ((prefix, handler) in handlers) {
            if (path.startsWith(prefix)) return handler.handle(path.removePrefix(prefix))
        }
        return null
    }

    class Builder {
        private var domain = DEFAULT_DOMAIN
        private val handlers = ArrayList<Pair<String, PathHandler>>()
        fun setDomain(domain: String) = apply { this.domain = domain }
        fun addPathHandler(path: String, handler: PathHandler) = apply { handlers += path to handler }
        fun build() = WebViewAssetLoader(domain, handlers.toList())
    }

    companion object {
        const val DEFAULT_DOMAIN = "appassets.androidplatform.net"

        private fun mimeFor(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "js", "mjs" -> "text/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "woff2" -> "font/woff2"
            "woff" -> "font/woff"
            "ttf" -> "font/ttf"
            else -> "application/octet-stream"
        }
    }
}
