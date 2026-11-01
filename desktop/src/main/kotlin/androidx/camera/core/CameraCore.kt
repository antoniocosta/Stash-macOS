package androidx.camera.core

import java.nio.ByteBuffer
import java.util.concurrent.Executor

/*
 * Desktop shims for CameraX core. There is no camera pipeline on the JVM: ProcessCameraProvider
 * fails to initialise (as on a device without a camera), and upstream's QR scanner shows its
 * "camera unavailable" path. These types exist so that code compiles unchanged.
 */

class CameraSelector private constructor(val lensFacing: Int) {
    companion object {
        const val LENS_FACING_FRONT = 0
        const val LENS_FACING_BACK = 1
        @JvmField val DEFAULT_BACK_CAMERA = CameraSelector(LENS_FACING_BACK)
        @JvmField val DEFAULT_FRONT_CAMERA = CameraSelector(LENS_FACING_FRONT)
    }
}

abstract class UseCase

class Preview private constructor() : UseCase() {
    interface SurfaceProvider

    fun setSurfaceProvider(surfaceProvider: SurfaceProvider?) {}

    class Builder {
        fun build() = Preview()
    }
}

interface ImageProxy : AutoCloseable {
    interface PlaneProxy {
        val buffer: ByteBuffer
        val rowStride: Int
        val pixelStride: Int
    }

    val width: Int
    val height: Int
    val format: Int
    val planes: Array<PlaneProxy>
}

class ImageAnalysis private constructor() : UseCase() {
    fun interface Analyzer {
        fun analyze(image: ImageProxy)
    }

    fun setAnalyzer(executor: Executor, analyzer: Analyzer) {}
    fun clearAnalyzer() {}

    class Builder {
        fun setBackpressureStrategy(strategy: Int) = this
        fun build() = ImageAnalysis()
    }

    companion object {
        const val STRATEGY_KEEP_ONLY_LATEST = 0
        const val STRATEGY_BLOCK_PRODUCER = 1
    }
}

interface Camera
