package androidx.compose.ui.graphics

import android.graphics.Bitmap

/** Android's ImageBitmap.asAndroidBitmap(): the AWT image behind a desktop ImageBitmap, as the Bitmap shim. */
fun ImageBitmap.asAndroidBitmap(): Bitmap = Bitmap(toAwtImage())

/** Android's Bitmap.asImageBitmap(): the Bitmap shim's AWT image as a Compose ImageBitmap. */
fun Bitmap.asImageBitmap(): ImageBitmap = image.toComposeImageBitmap()
