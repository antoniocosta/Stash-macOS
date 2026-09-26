package android.graphics

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/** Desktop shim for android.graphics.Bitmap wrapping a java.awt BufferedImage; compress() uses javax.imageio. */
class Bitmap internal constructor(image: BufferedImage) {
    private var img: BufferedImage? = image

    /** The backing image, for desktop code that needs to hand the pixels to AWT/Skia. */
    val image: BufferedImage get() = img ?: throw IllegalStateException("Can't use a recycled bitmap")

    val width: Int get() = image.width
    val height: Int get() = image.height
    val isRecycled: Boolean get() = img == null

    fun recycle() { img = null }

    fun getPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        image.getRGB(x, y, width, height, pixels, offset, stride)
    }

    /** Encodes to [stream]; false when the format has no ImageIO writer (e.g. WEBP), like an Android encode failure. */
    fun compress(format: CompressFormat, quality: Int, stream: OutputStream): Boolean {
        require(quality in 0..100) { "quality must be 0..100" }
        val src = image
        return when (format) {
            CompressFormat.PNG -> ImageIO.write(src, "png", stream)
            CompressFormat.JPEG -> {
                val rgb = if (src.type == BufferedImage.TYPE_INT_RGB) src else
                    BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB).also { dst ->
                        dst.createGraphics().apply { drawImage(src, 0, 0, java.awt.Color.BLACK, null); dispose() }
                    }
                val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull() ?: return false
                val ios = ImageIO.createImageOutputStream(stream)
                try {
                    writer.output = ios
                    val param = writer.defaultWriteParam.apply {
                        compressionMode = ImageWriteParam.MODE_EXPLICIT
                        compressionQuality = quality / 100f
                    }
                    writer.write(null, IIOImage(rgb, null, null), param)
                    true
                } finally {
                    writer.dispose()
                    ios.flush()
                }
            }
        }
    }

    enum class CompressFormat { JPEG, PNG }

    enum class Config { ARGB_8888, RGB_565 }

    fun copy(config: Config, isMutable: Boolean): Bitmap {
        val src = img ?: return Bitmap(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
        val type = when (config) {
            Config.RGB_565 -> BufferedImage.TYPE_USHORT_565_RGB
            Config.ARGB_8888 -> BufferedImage.TYPE_INT_ARGB
        }
        val dst = BufferedImage(src.width, src.height, type)
        dst.createGraphics().apply {
            drawImage(src, 0, 0, null)
            dispose()
        }
        return Bitmap(dst)
    }

    companion object {
        /** Same instance when the region is the whole immutable source, like Android. */
        @JvmStatic
        fun createBitmap(source: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
            require(x >= 0 && y >= 0 && width > 0 && height > 0) { "x, y must be >= 0; width, height must be > 0" }
            require(x + width <= source.width && y + height <= source.height) { "region exceeds bitmap bounds" }
            if (x == 0 && y == 0 && width == source.width && height == source.height) return source
            return Bitmap(copy(source.image.getSubimage(x, y, width, height), width, height, false))
        }

        @JvmStatic
        fun createScaledBitmap(src: Bitmap, dstWidth: Int, dstHeight: Int, filter: Boolean): Bitmap {
            if (dstWidth == src.width && dstHeight == src.height) return src
            require(dstWidth > 0 && dstHeight > 0) { "width and height must be > 0" }
            return Bitmap(copy(src.image, dstWidth, dstHeight, filter))
        }

        private fun copy(src: BufferedImage, w: Int, h: Int, filter: Boolean): BufferedImage =
            BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).also { dst ->
                dst.createGraphics().apply {
                    setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        if (filter) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                    )
                    drawImage(src, 0, 0, w, h, null)
                    dispose()
                }
            }
    }
}
