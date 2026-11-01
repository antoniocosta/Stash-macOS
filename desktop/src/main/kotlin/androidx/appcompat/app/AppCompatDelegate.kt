package androidx.appcompat.app

import android.content.DesktopContext
import androidx.core.os.LocaleListCompat
import com.stash.desktop.res.DesktopDisplay
import java.util.Locale

/**
 * Desktop shim for AppCompatDelegate's per-app language API. Like AppCompat's `autoStoreLocales`
 * on pre-13 Android, the choice is persisted by the library itself (shared_prefs/appcompat_locales.xml)
 * and re-applied at launch ([restoreStoredLocales], called from `Main.kt`). Applying it sets the
 * process default [Locale] and refreshes the UI's configuration, so Compose recomposes in the new
 * language without recreating the window.
 */
object AppCompatDelegate {
    private const val PREFS = "appcompat_locales"
    private const val KEY = "application_locales"

    private val systemLocale: Locale = Locale.getDefault()
    @Volatile private var applicationLocales: LocaleListCompat = LocaleListCompat.getEmptyLocaleList()

    @JvmStatic
    fun getApplicationLocales(): LocaleListCompat = applicationLocales

    @JvmStatic
    fun setApplicationLocales(locales: LocaleListCompat) {
        DesktopContext.getSharedPreferences(PREFS, 0).edit().putString(KEY, locales.toLanguageTags()).apply()
        apply(locales)
        // Android recreates the running activities so everything resolved at creation picks the language up.
        android.app.DesktopActivityHost.current?.let { activity -> javax.swing.SwingUtilities.invokeLater { activity.recreate() } }
    }

    /** Desktop: re-applies the stored app language at process start. */
    @JvmStatic
    fun restoreStoredLocales() {
        val stored = DesktopContext.getSharedPreferences(PREFS, 0).getString(KEY, null)
        if (!stored.isNullOrBlank()) apply(LocaleListCompat.forLanguageTags(stored))
    }

    private fun apply(locales: LocaleListCompat) {
        applicationLocales = locales
        Locale.setDefault(locales[0] ?: systemLocale)
        DesktopDisplay.onLocaleChanged()
    }

    const val MODE_NIGHT_FOLLOW_SYSTEM = -1
    const val MODE_NIGHT_NO = 1
    const val MODE_NIGHT_YES = 2
}
