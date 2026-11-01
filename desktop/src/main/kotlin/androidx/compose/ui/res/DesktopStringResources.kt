@file:JvmName("DesktopStringResourcesKt")

package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.stash.desktop.res.DesktopResources
import java.util.Locale
import org.xml.sax.InputSource

/*
 * Compose's Android resource accessors (StringResources.android.kt, PrimitiveResources, ...).
 * Reading LocalConfiguration subscribes the caller, so an app-language change recomposes it.
 */

@Composable
@ReadOnlyComposable
private fun locale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

@Composable
@ReadOnlyComposable
fun stringResource(id: Int): String = DesktopResources.string(id, locale())

@Composable
@ReadOnlyComposable
fun stringResource(id: Int, vararg formatArgs: Any): String {
    val locale = locale()
    return String.format(locale, DesktopResources.string(id, locale), *formatArgs)
}

@Composable
@ReadOnlyComposable
fun pluralStringResource(id: Int, count: Int): String = DesktopResources.quantityString(id, count, locale())

@Composable
@ReadOnlyComposable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String {
    val locale = locale()
    return String.format(locale, DesktopResources.quantityString(id, count, locale), *formatArgs)
}

/** Android's `ImageVector.vectorResource(id)`: the drawable's vector XML through Compose's Android-vector parser. */
@Composable
fun ImageVector.Companion.vectorResource(id: Int): ImageVector {
    val density = LocalDensity.current
    return remember(id, density) { DesktopResources.open(id).use { loadXmlImageVector(InputSource(it), density) } }
}

/** Android's `ImageBitmap.imageResource(id)`. */
@Composable
fun ImageBitmap.Companion.imageResource(id: Int): ImageBitmap =
    remember(id) { DesktopResources.open(id).use { loadImageBitmap(it) } }
