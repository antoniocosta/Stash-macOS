package android.util

/** Desktop shim for android.util.DisplayMetrics, derived from a [android.content.res.Configuration]. */
class DisplayMetrics {
    @JvmField var density: Float = 1f
    @JvmField var densityDpi: Int = DENSITY_DEFAULT
    @JvmField var scaledDensity: Float = 1f
    @JvmField var widthPixels: Int = 0
    @JvmField var heightPixels: Int = 0
    @JvmField var xdpi: Float = DENSITY_DEFAULT.toFloat()
    @JvmField var ydpi: Float = DENSITY_DEFAULT.toFloat()

    companion object {
        const val DENSITY_DEFAULT = 160
    }
}
