package com.yausername.ffmpeg

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

/** Desktop port of youtubedl-android's FFmpeg: verifies the system ffmpeg (brew) is present. */
object FFmpeg {
    @Volatile private var initialized = false

    @JvmStatic fun getInstance(): FFmpeg = this

    @Synchronized
    @Throws(YoutubeDLException::class)
    fun init(appContext: Context) {
        if (initialized) return
        YoutubeDL.which("ffmpeg") ?: throw YoutubeDLException("ffmpeg not found. Install it with: brew install ffmpeg")
        initialized = true
    }
}
