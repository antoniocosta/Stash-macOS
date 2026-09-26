@file:JvmName("DesktopSystemGestureExclusionKt")

package androidx.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/** Android system back/edge gestures don't exist on macOS; nothing to exclude, so the modifier is unchanged. */
fun Modifier.systemGestureExclusion(): Modifier = this

/** See [systemGestureExclusion]. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.systemGestureExclusion(exclusion: (LayoutCoordinates) -> Rect): Modifier = this
