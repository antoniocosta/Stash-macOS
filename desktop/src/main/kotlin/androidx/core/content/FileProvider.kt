package androidx.core.content

import android.content.Context
import android.net.Uri
import java.io.File

/** Desktop shim for androidx FileProvider: no content providers exist, so files are shared as canonical `file:` URIs. */
open class FileProvider {
    companion object {
        @JvmStatic
        fun getUriForFile(context: Context, authority: String, file: File): Uri = Uri.fromFile(file.canonicalFile)
    }
}
