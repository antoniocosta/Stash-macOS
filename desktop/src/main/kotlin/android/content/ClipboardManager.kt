package android.content

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/** Desktop shim for android.content.ClipboardManager, backed by the macOS system clipboard (AWT). */
class ClipboardManager private constructor() {

    fun setPrimaryClip(clip: ClipData) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(clip.text?.toString() ?: ""), null)
        }.onFailure { android.util.Log.w("Clipboard", "system clipboard unavailable", it) }
    }

    val primaryClip: ClipData?
        get() = runCatching {
            val s = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
            s?.let { ClipData.newPlainText(null, it) }
        }.getOrNull()

    fun hasPrimaryClip(): Boolean = primaryClip != null

    companion object {
        internal val INSTANCE by lazy { ClipboardManager() }
    }
}
