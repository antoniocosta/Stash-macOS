package androidx.browser.customtabs

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Desktop shim for androidx.browser's CustomTabsIntent: the URL opens in the default macOS browser. */
class CustomTabsIntent private constructor() {
    val intent: Intent = Intent(Intent.ACTION_VIEW)

    fun launchUrl(context: Context, url: Uri) {
        context.startActivity(Intent(intent).setData(url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    class Builder {
        fun setShowTitle(showTitle: Boolean) = this
        fun setShareState(shareState: Int) = this
        fun setUrlBarHidingEnabled(enabled: Boolean) = this
        fun setStartAnimations(context: Context, enterResId: Int, exitResId: Int) = this
        fun setExitAnimations(context: Context, enterResId: Int, exitResId: Int) = this
        fun setDefaultColorSchemeParams(params: Any) = this
        fun setColorScheme(colorScheme: Int) = this
        fun build() = CustomTabsIntent()
    }

    companion object {
        const val SHARE_STATE_OFF = 2
        const val COLOR_SCHEME_SYSTEM = 0
    }
}
