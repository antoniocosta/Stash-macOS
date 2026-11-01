package android.content.res

import android.os.LocaleList
import java.util.Locale

/**
 * Desktop shim for android.content.res.Configuration. The live values come from
 * [com.stash.desktop.res.DesktopDisplay] (the main window's size and the screen's backing scale);
 * a `Configuration()` starts with every field undefined, exactly like Android's, so it can be
 * used as an override delta (see [updateFrom]).
 */
class Configuration() {
    var densityDpi: Int = DENSITY_DPI_UNDEFINED
    var screenWidthDp: Int = SCREEN_WIDTH_DP_UNDEFINED
    var screenHeightDp: Int = SCREEN_HEIGHT_DP_UNDEFINED
    var smallestScreenWidthDp: Int = SMALLEST_SCREEN_WIDTH_DP_UNDEFINED
    var fontScale: Float = 0f
    var uiMode: Int = 0
    var orientation: Int = ORIENTATION_UNDEFINED
    private var localeList: LocaleList = LocaleList.getEmptyLocaleList()

    constructor(other: Configuration) : this() {
        setTo(other)
    }

    val locales: LocaleList get() = localeList

    @Deprecated("Use locales")
    @set:JvmName("setLocaleField") // Android's public `locale` field; setLocale() is the method below
    var locale: Locale?
        get() = localeList[0]
        set(value) { localeList = if (value == null) LocaleList.getEmptyLocaleList() else LocaleList(value) }

    fun setLocales(locales: LocaleList?) {
        localeList = locales ?: LocaleList.getEmptyLocaleList()
    }

    fun setLocale(locale: Locale?) {
        @Suppress("DEPRECATION")
        this.locale = locale
    }

    fun setTo(other: Configuration) {
        densityDpi = other.densityDpi
        screenWidthDp = other.screenWidthDp
        screenHeightDp = other.screenHeightDp
        smallestScreenWidthDp = other.smallestScreenWidthDp
        fontScale = other.fontScale
        uiMode = other.uiMode
        orientation = other.orientation
        localeList = other.localeList
    }

    /** Copies every defined field of [delta] over this one (Android's `updateFrom`). */
    fun updateFrom(delta: Configuration): Int {
        if (delta.densityDpi != DENSITY_DPI_UNDEFINED) densityDpi = delta.densityDpi
        if (delta.screenWidthDp != SCREEN_WIDTH_DP_UNDEFINED) screenWidthDp = delta.screenWidthDp
        if (delta.screenHeightDp != SCREEN_HEIGHT_DP_UNDEFINED) screenHeightDp = delta.screenHeightDp
        if (delta.smallestScreenWidthDp != SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) smallestScreenWidthDp = delta.smallestScreenWidthDp
        if (delta.fontScale != 0f) fontScale = delta.fontScale
        if (delta.uiMode != 0) uiMode = delta.uiMode
        if (delta.orientation != ORIENTATION_UNDEFINED) orientation = delta.orientation
        if (!delta.localeList.isEmpty) localeList = delta.localeList
        return 0
    }

    override fun equals(other: Any?): Boolean =
        other is Configuration && densityDpi == other.densityDpi && screenWidthDp == other.screenWidthDp &&
            screenHeightDp == other.screenHeightDp && smallestScreenWidthDp == other.smallestScreenWidthDp &&
            fontScale == other.fontScale && uiMode == other.uiMode && orientation == other.orientation &&
            localeList == other.localeList

    override fun hashCode(): Int =
        listOf(densityDpi, screenWidthDp, screenHeightDp, smallestScreenWidthDp, fontScale, uiMode, orientation, localeList).hashCode()

    override fun toString(): String =
        "{densityDpi=$densityDpi w${screenWidthDp}dp h${screenHeightDp}dp sw${smallestScreenWidthDp}dp $localeList}"

    companion object {
        const val DENSITY_DPI_UNDEFINED = 0
        const val SCREEN_WIDTH_DP_UNDEFINED = 0
        const val SCREEN_HEIGHT_DP_UNDEFINED = 0
        const val SMALLEST_SCREEN_WIDTH_DP_UNDEFINED = 0
        const val ORIENTATION_UNDEFINED = 0
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
        const val UI_MODE_NIGHT_MASK = 0x30
        const val UI_MODE_NIGHT_NO = 0x10
        const val UI_MODE_NIGHT_YES = 0x20
    }
}
