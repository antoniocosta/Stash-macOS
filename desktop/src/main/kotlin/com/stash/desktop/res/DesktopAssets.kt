package com.stash.desktop.res

import java.io.InputStream

/** The merged `src/main/assets` of every upstream module (AssetManager's view), copied under stash-assets/ at build time. */
object DesktopAssets {
    fun open(path: String): InputStream? =
        DesktopAssets::class.java.classLoader.getResourceAsStream("stash-assets/" + path.trimStart('/'))
}
