package com.stash.data.ytmusic.potoken

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Desktop replacement for upstream BotGuardPoTokenMinter.kt (excluded: it runs
 * BotGuard inside an android.webkit.WebView, which has no JVM equivalent).
 *
 * Same class name and constructor so upstream YTMusicDataModule binds it
 * unmodified. Behaves as upstream's own [PoTokenMinter.None] — "processes
 * without a usable WebView" — so callers carry on without a token, exactly the
 * upstream fallback path.
 */
@Suppress("UNUSED_PARAMETER")
class BotGuardPoTokenMinter @Inject constructor(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
) : PoTokenMinter by PoTokenMinter.None
