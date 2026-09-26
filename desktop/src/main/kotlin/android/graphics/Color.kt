package android.graphics

object Color {
    const val BLACK: Int = 0xFF000000.toInt()
    const val DKGRAY: Int = 0xFF444444.toInt()
    const val GRAY: Int = 0xFF888888.toInt()
    const val LTGRAY: Int = 0xFFCCCCCC.toInt()
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val RED: Int = 0xFFFF0000.toInt()
    const val GREEN: Int = 0xFF00FF00.toInt()
    const val BLUE: Int = 0xFF0000FF.toInt()
    const val YELLOW: Int = 0xFFFFFF00.toInt()
    const val CYAN: Int = 0xFF00FFFF.toInt()
    const val MAGENTA: Int = 0xFFFF00FF.toInt()
    const val TRANSPARENT: Int = 0x00000000

    @JvmStatic
    fun alpha(color: Int): Int = color ushr 24

    @JvmStatic
    fun red(color: Int): Int = (color shr 16) and 0xFF

    @JvmStatic
    fun green(color: Int): Int = (color shr 8) and 0xFF

    @JvmStatic
    fun blue(color: Int): Int = color and 0xFF

    @JvmStatic
    fun rgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)

    @JvmStatic
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        ((alpha and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
}
