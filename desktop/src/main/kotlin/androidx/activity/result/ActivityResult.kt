package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

/** activity's PickVisualMediaRequest (Android-only artifact). */
class PickVisualMediaRequest(val mediaType: PickVisualMedia.VisualMediaType = PickVisualMedia.ImageAndVideo)

/** activity's ActivityResultLauncher. */
abstract class ActivityResultLauncher<I> {
    abstract fun launch(input: I)
    open fun unregister() {}
}
