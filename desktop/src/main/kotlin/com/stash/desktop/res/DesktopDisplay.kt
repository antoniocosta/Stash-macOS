package com.stash.desktop.res

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.awt.GraphicsEnvironment
import java.util.Locale

/**
 * The "system" half of every [Configuration]: the main window's size in dp, the screen's backing
 * scale as a density, and the process locale. `Main.kt` reports window changes through [update];
 * an app-locale change ([androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]) through
 * [onLocaleChanged]. Compose reads [state] (via `LocalConfiguration`), so both recompose the UI.
 */
object DesktopDisplay {
    @Volatile private var widthDp = 1280
    @Volatile private var heightDp = 860
    @Volatile private var densityDpi = defaultDensityDpi()

    private val current = mutableStateOf(build())

    val state: State<Configuration> get() = current

    /** A fresh copy of the system configuration. */
    fun configuration(): Configuration = build()

    fun update(widthDp: Int, heightDp: Int, density: Float) {
        val dpi = (density * 160f).toInt().coerceAtLeast(1)
        if (widthDp == this.widthDp && heightDp == this.heightDp && dpi == densityDpi) return
        this.widthDp = widthDp
        this.heightDp = heightDp
        this.densityDpi = dpi
        current.value = build()
    }

    fun onLocaleChanged() {
        current.value = build()
    }

    private fun build() = Configuration().apply {
        densityDpi = this@DesktopDisplay.densityDpi
        screenWidthDp = widthDp
        screenHeightDp = heightDp
        smallestScreenWidthDp = minOf(widthDp, heightDp)
        fontScale = 1f
        orientation = if (widthDp >= heightDp) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        setLocales(LocaleList(Locale.getDefault()))
    }

    private fun defaultDensityDpi(): Int = runCatching {
        if (GraphicsEnvironment.isHeadless()) return@runCatching 160
        val tx = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform
        (tx.scaleX * 160).toInt()
    }.getOrDefault(160)
}
