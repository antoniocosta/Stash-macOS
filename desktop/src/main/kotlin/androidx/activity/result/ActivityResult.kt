package androidx.activity.result

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

/** activity's PickVisualMediaRequest (Android-only artifact). */
class PickVisualMediaRequest(val mediaType: PickVisualMedia.VisualMediaType = PickVisualMedia.ImageAndVideo)

/** activity's ActivityResultLauncher. */
abstract class ActivityResultLauncher<I> {
    abstract fun launch(input: I)
    open fun unregister() {}
}

/** activity's ActivityResult: an activity's result code and data. */
class ActivityResult(val resultCode: Int, val data: android.content.Intent?)

/** activity's IntentSenderRequest. */
class IntentSenderRequest private constructor(val intentSender: android.content.IntentSender, val fillInIntent: android.content.Intent?) {
    class Builder(private val intentSender: android.content.IntentSender) {
        private var fillIn: android.content.Intent? = null
        fun setFillInIntent(fillInIntent: android.content.Intent?) = apply { fillIn = fillInIntent }
        fun setFlags(values: Int, mask: Int) = this
        fun build() = IntentSenderRequest(intentSender, fillIn)
    }
}
