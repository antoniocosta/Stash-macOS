package com.stash.desktop

import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.stash.core.media.service.StashPlaybackService
import com.stash.core.model.TrackItem
import com.stash.desktop.media.FfTools
import com.stash.data.ytmusic.model.SearchResultSection
import com.stash.data.ytmusic.model.TopResultItem
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue

/**
 * Real playback through the desktop pipeline (ffmpeg decode → DSP → AudioTrack shim), no accounts.
 * Players run at volume 0. The YouTube Music test needs the network and runs only with
 * `-Dstash.networkTests=true`.
 */
class PlaybackTest {
    private fun ffmpeg(vararg args: String) {
        val p = ProcessBuilder(listOf(checkNotNull(FfTools.resolve("ffmpeg")) { "ffmpeg not installed" }.path, "-hide_banner", "-loglevel", "error", "-y") + args)
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        check(p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0) { "ffmpeg failed: $out" }
    }

    private fun waitFor(timeoutMs: Long, what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("Timed out waiting for $what")
            Thread.sleep(50)
        }
    }

    private fun tone(dir: File, name: String, seconds: Int): File =
        File(dir, name).also { ffmpeg("-f", "lavfi", "-i", "sine=frequency=440:duration=$seconds", "-ac", "2", it.absolutePath) }

    @Test
    fun `local file plays to the end through StashPlaybackService`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val app = runtime.application
        val file = tone(Files.createTempDirectory("stash-play").toFile(), "tone.m4a", 2)

        val token = SessionToken(app, ComponentName(app, StashPlaybackService::class.java))
        val controller = MediaController.Builder(app, token).buildAsync().get(5, TimeUnit.SECONDS)
        try {
            val errors = CopyOnWriteArrayList<PlaybackException>()
            controller.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            controller.volume = 0f
            controller.setMediaItem(MediaItem.Builder().setMediaId("local-tone").setUri(Uri.fromFile(file)).build())
            controller.prepare()
            controller.play()

            waitFor(10_000, "position to advance") { controller.currentPosition > 500 || errors.isNotEmpty() }
            assertTrue(errors.isEmpty(), "playback error: ${errors.firstOrNull()?.message}")
            waitFor(10_000, "end of the track") { controller.playbackState == Player.STATE_ENDED || errors.isNotEmpty() }
            assertTrue(errors.isEmpty(), "playback error: ${errors.firstOrNull()?.message}")
            assertTrue(controller.duration in 1_500..2_500, "duration was ${controller.duration}")
        } finally {
            controller.stop()
            controller.release()
        }
    }

    /** A CDN stand-in: serves an HLS rendition (absolute segment URLs) and records each request's User-Agent. */
    private class HlsServer(dir: File) : AutoCloseable {
        val requests = CopyOnWriteArrayList<Pair<String, String?>>()
        private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            createContext("/") { ex ->
                ex.use {
                    requests += ex.requestURI.path to ex.requestHeaders.getFirst("User-Agent")
                    val f = File(dir, ex.requestURI.path.removePrefix("/hls/"))
                    if (!f.isFile) { ex.sendResponseHeaders(404, -1); return@use }
                    val bytes = f.readBytes()
                    ex.sendResponseHeaders(200, bytes.size.toLong())
                    ex.responseBody.write(bytes)
                }
            }
            start()
        }
        val base = "http://127.0.0.1:${server.address.port}/hls/"
        override fun close() = server.stop(0)
    }

    private fun hlsFixture(): Pair<File, (String) -> Unit> {
        val dir = Files.createTempDirectory("stash-hls").toFile()
        val src = tone(dir, "src.wav", 4)
        return dir to { base ->
            ffmpeg("-i", src.absolutePath, "-c:a", "aac", "-b:a", "128k", "-f", "hls", "-hls_time", "1",
                "-hls_playlist_type", "vod", "-hls_base_url", base, File(dir, "index.m3u8").absolutePath)
        }
    }

    private fun standalonePlayer(): ExoPlayer {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        return StashDesktopRuntime.runOnMainThread { ExoPlayer.Builder(runtime.application).build() }.also { it.volume = 0f }
    }

    @Test
    fun `HLS plays with every segment loaded through the source's DataSource`() {
        val (dir, encode) = hlsFixture()
        HlsServer(dir).use { server ->
            encode(server.base)
            assertTrue(File(dir, "index.m3u8").readText().contains(server.base), "fixture uses absolute segment URLs")
            val player = standalonePlayer()
            val errors = CopyOnWriteArrayList<PlaybackException>()
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            try {
                val item = MediaItem.Builder().setUri(server.base + "index.m3u8").build()
                player.setMediaSource(HlsMediaSource.Factory(DefaultHttpDataSource.Factory().setUserAgent("StashTest")).createMediaSource(item))
                player.prepare()
                player.play()
                waitFor(20_000, "end of the HLS stream") { player.playbackState == Player.STATE_ENDED || errors.isNotEmpty() }
                assertTrue(errors.isEmpty(), "playback error: ${errors.firstOrNull()?.message}")
                assertTrue(player.currentPosition >= 3_000, "played ${player.currentPosition} ms of 4000")
            } finally {
                player.release()
            }
            val segments = server.requests.filter { it.first.endsWith(".ts") }
            assertTrue(segments.size >= 4, "segments requested: ${server.requests}")
            assertTrue(server.requests.all { it.second == "StashTest" }, "a request bypassed the DataSource: ${server.requests}")
        }
    }

    @Test
    fun `HLS load refused by the DataSource surfaces as a player error`() {
        val (dir, encode) = hlsFixture()
        HlsServer(dir).use { server ->
            encode(server.base)
            val player = standalonePlayer()
            val errors = CopyOnWriteArrayList<PlaybackException>()
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { errors += error }
            })
            // What upstream's DownloadModeGate does when streaming is switched off.
            val refusing = DataSource.Factory {
                object : DataSource {
                    override fun addTransferListener(transferListener: TransferListener) {}
                    override fun open(dataSpec: DataSpec): Long = throw IOException("Download mode: streaming disabled")
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = throw IOException("closed")
                    override fun getUri(): Uri? = null
                    override fun close() {}
                }
            }
            try {
                player.setMediaSource(HlsMediaSource.Factory(refusing).createMediaSource(MediaItem.fromUri(server.base + "index.m3u8")))
                player.prepare()
                player.play()
                waitFor(15_000, "the player error") { errors.isNotEmpty() }
                assertTrue(errors.first().message.orEmpty().contains("streaming disabled"), "error was: ${errors.first().message}")
            } finally {
                player.release()
            }
            assertTrue(server.requests.isEmpty(), "nothing may reach the network: ${server.requests}")
        }
    }

    @Test
    fun `YouTube Music search result streams without an account`() {
        assumeTrue("network test; run with -Dstash.networkTests=true", System.getProperty("stash.networkTests") == "true")
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val results = runBlocking { runtime.component.ytMusicApiClient().searchAll("Daft Punk Get Lucky") }
        val song = results.sections.firstNotNullOfOrNull { section ->
            when (section) {
                is SearchResultSection.Songs -> section.tracks.firstOrNull()
                is SearchResultSection.Top -> (section.item as? TopResultItem.TrackTop)?.track
                else -> null
            }
        }
        assertNotNull(song, "no song in search results: $results")
        println("Streaming search result: ${song.artist} - ${song.title} (${song.videoId})")

        val repo = runtime.component.playerRepository()
        // Online mode (streaming), restored afterwards: the tests share the app's real settings.
        val streaming = runtime.component.streamingPreference()
        val wasEnabled = runBlocking { streaming.current() }
        runBlocking { streaming.setEnabled(true) }
        try {
            repo.setVolume(0f)
            val result = runBlocking {
                repo.playFromStream(TrackItem(song.videoId, song.title, song.artist, song.durationSeconds, song.thumbnailUrl, song.album))
            }
            println("Routing result: $result")
            val position = runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(90_000) { repo.currentPosition.first { it > 5_000 } }
            }
            assertNotNull(position, "streamed playback never passed 5 s; state: ${repo.playerState.value}")
            assertTrue(repo.playerState.value.isPlaying, "state: ${repo.playerState.value}")
            println("Streamed $position ms of ${repo.playerState.value.durationMs} ms")
            runBlocking { repo.pause() }
        } finally {
            runBlocking { streaming.setEnabled(wasEnabled) }
        }
    }
}
