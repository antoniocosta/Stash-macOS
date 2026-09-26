package coil3

import android.graphics.Bitmap
import org.jetbrains.skiko.toBufferedImage

/**
 * 0-argument overload of `coil3.Image.toBitmap()` returning `android.graphics.Bitmap`
 * so `state.result.image.toBitmap()` in `NowPlayingScreen.kt` hands an Android
 * `Bitmap` directly to `Palette.from(bitmap)`.
 */
fun Image.toBitmap(): Bitmap {
    val skiaBitmap = this.toBitmap(this.width.coerceAtLeast(1), this.height.coerceAtLeast(1))
    return Bitmap(skiaBitmap.toBufferedImage())
}
