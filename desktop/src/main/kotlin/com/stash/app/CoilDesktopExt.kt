package com.stash.app

import android.content.Context
import android.content.DesktopContext
import coil3.PlatformContext
import java.io.File

/**
 * Desktop extension satisfying `context.cacheDir` in `CoilConfiguration.kt`,
 * where `context` is `coil3.PlatformContext`.
 */
val PlatformContext.cacheDir: File
    get() = (this as? Context ?: DesktopContext).cacheDir
