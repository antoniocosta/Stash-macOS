@file:JvmName("DesktopResourceFontKt")

package androidx.compose.ui.text.font

import com.stash.desktop.res.DesktopResources

/** Android's `Font(@FontRes resId, weight, style)`: loads the bundled res/font file into a desktop platform Font. */
fun Font(
    resId: Int,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal,
): Font = androidx.compose.ui.text.platform.Font(
    identity = DesktopResources.path(resId),
    data = DesktopResources.bytes(resId),
    weight = weight,
    style = style,
)
