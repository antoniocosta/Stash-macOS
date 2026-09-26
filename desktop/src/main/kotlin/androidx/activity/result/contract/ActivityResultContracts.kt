package androidx.activity.result.contract

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import com.stash.desktop.files.DesktopFileDialogs

/**
 * activity's ActivityResultContract (Android-only artifact). On desktop a contract is resolved
 * in-process by [resolve] (a native macOS file dialog for the SAF contracts), instead of
 * Intent round-trips through another Activity.
 */
abstract class ActivityResultContract<I, O> {
    /** Desktop: performs the "activity" synchronously on the UI thread and returns its result. */
    abstract fun resolve(input: I): O
}

/** The subset of androidx.activity.result.contract.ActivityResultContracts upstream uses. */
class ActivityResultContracts private constructor() {

    /** SAF multi-document picker: [input] are MIME types; empty list when cancelled (Android behaviour). */
    open class OpenMultipleDocuments : ActivityResultContract<Array<String>, List<@JvmSuppressWildcards Uri>>() {
        override fun resolve(input: Array<String>): List<Uri> =
            DesktopFileDialogs.open(mimeTypes = input.toList(), multiple = true).map(Uri::fromFile)
    }

    /** SAF single-document picker. */
    open class OpenDocument : ActivityResultContract<Array<String>, Uri?>() {
        override fun resolve(input: Array<String>): Uri? =
            DesktopFileDialogs.open(mimeTypes = input.toList(), multiple = false).firstOrNull()?.let(Uri::fromFile)
    }

    /** SAF folder picker: returns a file: tree URI (desktop has no content:// providers). [input] = initial folder. */
    open class OpenDocumentTree : ActivityResultContract<Uri?, Uri?>() {
        override fun resolve(input: Uri?): Uri? =
            DesktopFileDialogs.chooseFolder(initial = input?.path)?.let(Uri::fromFile)
    }

    /** SAF create-document: save dialog pre-filled with [input] as the file name. */
    open class CreateDocument(private val mimeType: String) : ActivityResultContract<String, Uri?>() {
        override fun resolve(input: String): Uri? = DesktopFileDialogs.save(suggestedName = input)?.let(Uri::fromFile)
    }

    /** Photo picker. */
    open class PickVisualMedia : ActivityResultContract<PickVisualMediaRequest, Uri?>() {
        override fun resolve(input: PickVisualMediaRequest): Uri? {
            val mimes = when (val type = input.mediaType) {
                is ImageOnly -> listOf("image/*")
                is VideoOnly -> listOf("video/*")
                is SingleMimeType -> listOf(type.mimeType)
                else -> listOf("image/*", "video/*")
            }
            return DesktopFileDialogs.open(mimeTypes = mimes, multiple = false).firstOrNull()?.let(Uri::fromFile)
        }

        sealed interface VisualMediaType
        object ImageAndVideo : VisualMediaType
        object ImageOnly : VisualMediaType
        object VideoOnly : VisualMediaType
        class SingleMimeType(val mimeType: String) : VisualMediaType
    }

    /** Runtime permission request: macOS has no runtime permission model for these, so always granted. */
    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun resolve(input: String): Boolean = true
    }
}
