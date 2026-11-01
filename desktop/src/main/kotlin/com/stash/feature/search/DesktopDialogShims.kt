package com.stash.feature.search

import androidx.compose.ui.window.DialogProperties

/** Android's DialogProperties overload with decorFitsSystemWindows (no window insets on desktop, so it's dropped). */
@Suppress("UNUSED_PARAMETER")
fun DialogProperties(usePlatformDefaultWidth: Boolean, decorFitsSystemWindows: Boolean): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth)
