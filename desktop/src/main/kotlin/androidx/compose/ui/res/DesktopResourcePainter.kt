@file:JvmName("DesktopResourcePainterKt")

package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import com.stash.desktop.res.DesktopResources
import org.xml.sax.InputSource

/** Android's `painterResource(@DrawableRes id)`: vector XML via Compose's Android-vector parser, bitmaps via Skia. */
@Composable
fun painterResource(id: Int): Painter {
    val path = remember(id) { DesktopResources.path(id) }
    return if (path.endsWith(".xml")) {
        val density = LocalDensity.current
        val vector = remember(id, density) {
            DesktopResources.open(id).use { loadXmlImageVector(InputSource(it), density) }
        }
        rememberVectorPainter(vector)
    } else {
        remember(id) { BitmapPainter(DesktopResources.open(id).use { loadImageBitmap(it) }) }
    }
}
