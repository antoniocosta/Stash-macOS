@file:JvmName("DesktopAndroidCompositionLocalsKt")

package androidx.compose.ui.platform

import android.app.DesktopActivity
import android.content.Context
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Android's `LocalContext` / `LocalView` (absent from Compose Desktop). Both default to the
 * single [DesktopActivity], so upstream composables get a working Context without a provider.
 */
val LocalContext = staticCompositionLocalOf<Context> { DesktopActivity }

val LocalView = staticCompositionLocalOf<View> { View(DesktopActivity) }
