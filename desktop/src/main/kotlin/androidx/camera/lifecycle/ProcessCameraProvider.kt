package androidx.camera.lifecycle

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.lifecycle.LifecycleOwner

/** Desktop shim for CameraX's ProcessCameraProvider: initialisation fails (no camera pipeline on the JVM). */
class ProcessCameraProvider private constructor() {
    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, cameraSelector: CameraSelector, vararg useCases: UseCase): Camera =
        throw IllegalStateException("No camera available")

    fun unbindAll() {}

    companion object {
        @JvmStatic
        suspend fun awaitInstance(context: Context): ProcessCameraProvider =
            throw IllegalStateException("CameraX is not available on the desktop")
    }
}

/** camera-lifecycle's `ProcessCameraProvider.Companion.awaitInstance` extension import. */
suspend fun ProcessCameraProvider.Companion.awaitInstance(context: Context): ProcessCameraProvider =
    throw IllegalStateException("CameraX is not available on the desktop")
