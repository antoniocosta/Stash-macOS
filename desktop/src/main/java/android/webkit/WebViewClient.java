package android.webkit;

import android.graphics.Bitmap;

/**
 * Desktop shim for android.webkit.WebViewClient. Java, like the framework class, so its
 * parameters are platform types and upstream may override them as nullable or not.
 */
public class WebViewClient {
  public void onPageStarted(WebView view, String url, Bitmap favicon) {}

  public void onPageFinished(WebView view, String url) {}

  public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {}

  public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    return false;
  }

  public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
    return null;
  }

  public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
    return false;
  }
}
