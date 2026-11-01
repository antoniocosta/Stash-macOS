package android.text.format

import android.content.Context
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.chrono.IsoChronology
import java.util.Locale

/** Desktop shim for android.text.format.DateFormat, backed by the JDK's CLDR locale data. */
object DateFormat {
    /** macOS's 24-hour clock setting is not visible to the JVM; follow the locale's short time pattern. */
    @JvmStatic
    fun is24HourFormat(context: Context): Boolean =
        !DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, Locale.getDefault()).contains('a')

    /**
     * The locale's best pattern for [skeleton] (ICU's DateTimePatternGenerator on Android). The JDK
     * has no public skeleton API, so the common skeletons Stash uses are mapped to CLDR's
     * corresponding styles, keeping only the fields the skeleton names.
     */
    @JvmStatic
    fun getBestDateTimePattern(locale: Locale, skeleton: String): String {
        val hasYear = skeleton.contains('y')
        val hasDay = skeleton.contains('d')
        val longMonth = skeleton.contains("MMMM")
        val base = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            if (longMonth) FormatStyle.LONG else FormatStyle.MEDIUM, null, IsoChronology.INSTANCE, locale,
        )
        var p = base
        if (!hasYear) p = p.replace(Regex("[,.\\s/-]*y+[.年년]?[,\\s]*"), " ").replace(Regex("^[\\s,]+|[\\s,]+$"), "")
        if (!hasDay) p = p.replace(Regex("[,.\\s/-]*d+[.日일]?[,\\s]*"), " ").replace(Regex("^[\\s,]+|[\\s,]+$"), "")
        p = p.replace(Regex("E+[,\\s]*"), "").trim()
        return p.ifBlank { skeleton }
    }

    @JvmStatic
    fun getTimeFormat(context: Context): java.text.DateFormat =
        if (is24HourFormat(context)) SimpleDateFormat("HH:mm", Locale.getDefault()) else java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault())

    @JvmStatic
    fun getDateFormat(context: Context): java.text.DateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault())

    @JvmStatic
    fun getMediumDateFormat(context: Context): java.text.DateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())

    @JvmStatic
    fun format(inFormat: CharSequence, inTimeInMillis: Long): CharSequence = SimpleDateFormat(inFormat.toString(), Locale.getDefault()).format(inTimeInMillis)
}
