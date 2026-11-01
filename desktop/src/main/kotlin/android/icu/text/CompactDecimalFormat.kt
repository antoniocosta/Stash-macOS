package android.icu.text

import java.text.NumberFormat
import java.util.Locale

/** Desktop shim for ICU's CompactDecimalFormat over the JDK's CompactNumberFormat (CLDR data). */
class CompactDecimalFormat private constructor(private val format: NumberFormat) {
    enum class CompactStyle { SHORT, LONG }

    var maximumFractionDigits: Int
        get() = format.maximumFractionDigits
        set(value) { format.maximumFractionDigits = value }

    var minimumFractionDigits: Int
        get() = format.minimumFractionDigits
        set(value) { format.minimumFractionDigits = value }

    fun format(number: Long): String = format.format(number)
    fun format(number: Double): String = format.format(number)
    fun format(number: Any): String = format.format(number)

    companion object {
        @JvmStatic
        fun getInstance(locale: Locale, style: CompactStyle): CompactDecimalFormat =
            CompactDecimalFormat(NumberFormat.getCompactNumberInstance(locale, if (style == CompactStyle.LONG) NumberFormat.Style.LONG else NumberFormat.Style.SHORT))
    }
}
