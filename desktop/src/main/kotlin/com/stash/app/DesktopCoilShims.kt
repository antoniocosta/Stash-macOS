package com.stash.app

import android.content.Context
import android.content.DesktopContext
import coil3.PlatformContext
import com.stash.core.common.DataSaverArtCap

/** On Android PlatformContext is Context; here it's a supertype, so narrow it (the app passes its Context). */
fun DataSaverArtCap(context: PlatformContext): DataSaverArtCap =
    DataSaverArtCap(context as? Context ?: DesktopContext)
