package android.text.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Desktop port of the android.text.format.DateUtils members upstream uses. The relative-span logic
 * follows AOSP's getRelativeTimeSpanString (English output, e.g. "3 minutes ago", "In 2 hours",
 * "Yesterday", "Sep 24" / "Sep 24, 2025").
 */
object DateUtils {
    const val SECOND_IN_MILLIS = 1000L
    const val MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60
    const val HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60
    const val DAY_IN_MILLIS = HOUR_IN_MILLIS * 24
    const val WEEK_IN_MILLIS = DAY_IN_MILLIS * 7

    const val FORMAT_SHOW_TIME = 0x00001
    const val FORMAT_SHOW_YEAR = 0x00004
    const val FORMAT_NO_YEAR = 0x00008
    const val FORMAT_SHOW_DATE = 0x00010
    const val FORMAT_NUMERIC_DATE = 0x00020
    const val FORMAT_ABBREV_MONTH = 0x10000
    const val FORMAT_ABBREV_RELATIVE = 0x40000
    const val FORMAT_ABBREV_ALL = 0x80000

    @JvmStatic
    fun getRelativeTimeSpanString(time: Long, now: Long, minResolution: Long): CharSequence =
        getRelativeTimeSpanString(time, now, minResolution, FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH)

    @JvmStatic
    fun getRelativeTimeSpanString(time: Long, now: Long, minResolution: Long, flags: Int): CharSequence {
        val abbrev = flags and (FORMAT_ABBREV_RELATIVE or FORMAT_ABBREV_ALL) != 0
        val past = now >= time
        val duration = Math.abs(now - time)
        fun span(count: Long, unit: String, abbr: String): String {
            val u = if (abbrev) abbr else if (count == 1L) unit else unit + "s"
            return if (past) "$count $u ago" else "In $count $u"
        }
        return when {
            duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS ->
                span(duration / SECOND_IN_MILLIS, "second", "sec.")
            duration < HOUR_IN_MILLIS && minResolution < HOUR_IN_MILLIS ->
                span(duration / MINUTE_IN_MILLIS, "minute", "min.")
            duration < DAY_IN_MILLIS && minResolution < DAY_IN_MILLIS ->
                span(duration / HOUR_IN_MILLIS, "hour", if (duration / HOUR_IN_MILLIS == 1L) "hr." else "hr.")
            duration < WEEK_IN_MILLIS && minResolution < WEEK_IN_MILLIS -> relativeDay(time, now)
            else -> formatDate(time, now, flags)
        }
    }

    private fun relativeDay(time: Long, now: Long): String {
        val zone = ZoneId.systemDefault()
        val days = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
            Instant.ofEpochMilli(time).atZone(zone).toLocalDate(),
        )
        return when {
            days == 0L -> "Today"
            days == -1L -> "Yesterday"
            days == 1L -> "Tomorrow"
            days < 0 -> "${-days} days ago"
            else -> "In $days days"
        }
    }

    private fun formatDate(time: Long, now: Long, flags: Int): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(time).atZone(zone)
        val showYear = flags and FORMAT_SHOW_YEAR != 0 ||
            (flags and FORMAT_NO_YEAR == 0 && date.year != Instant.ofEpochMilli(now).atZone(zone).year)
        val month = if (flags and (FORMAT_ABBREV_MONTH or FORMAT_ABBREV_ALL) != 0) "MMM" else "MMMM"
        val pattern = if (flags and FORMAT_NUMERIC_DATE != 0) (if (showYear) "M/d/yyyy" else "M/d")
        else if (showYear) "$month d, yyyy" else "$month d"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)
    }
}
