package com.stash.desktop

import android.content.ComponentName
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.composable
import androidx.work.Data
import androidx.work.WorkerParameters
import com.stash.core.data.share.SharedMixFollowWorker
import com.stash.core.data.share.SharedMixPublishWorker
import com.stash.core.data.share.SharedMixUnshareWorker
import com.stash.core.data.sync.workers.ArtBackfillWorker
import com.stash.core.data.sync.workers.ArtistImageBackfillWorker
import com.stash.core.data.sync.workers.BlocklistIntegrityWorker
import com.stash.core.data.sync.workers.DiffWorker
import com.stash.core.data.sync.workers.DiscoveryDownloadWorker
import com.stash.core.data.sync.workers.FlacUpgradeWorker
import com.stash.core.data.sync.workers.LoudnessBackfillWorker
import com.stash.core.data.sync.workers.PlaylistFetchWorker
import com.stash.core.data.sync.workers.QualityInfoBackfillWorker
import com.stash.core.data.sync.workers.StashDiscoveryWorker
import com.stash.core.data.sync.workers.StashMixRefreshWorker
import com.stash.core.data.sync.workers.SyncFinalizeWorker
import com.stash.core.data.sync.workers.TagEnrichmentWorker
import com.stash.core.data.sync.workers.TrackDownloadWorker
import com.stash.core.data.sync.workers.TrackInfoEnrichmentWorker
import com.stash.core.media.equalizer.StashRenderersFactory
import com.stash.core.media.service.StashPlaybackService
import com.stash.data.download.backfill.MetadataBackfillWorker
import com.stash.data.download.backfill.YtLibraryBackfillWorker
import com.stash.data.download.lossless.LosslessRetryWorker
import com.stash.data.download.ytdlp.YtDlpUpdateWorker
import com.stash.data.lyrics.worker.LyricsFetchWorker
import com.stash.data.lyrics.worker.LyricsTtmlUpgradeWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SmokeTest {

    @Test
    fun `dagger graph bootstraps and injects StashApplication and Room database`(): Unit = runBlocking {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val app = runtime.application

        assertNotNull(app.workerFactory)
        assertNotNull(app.musicRepository)
        assertNotNull(app.syncNotificationManager)
        assertNotNull(app.ytDlpManager)
        assertNotNull(app.okHttpClient)
        assertNotNull(app.playlistDao)
        assertNotNull(app.trackDao)
        assertNotNull(app.listeningRecorder)
        assertNotNull(app.discordRpcCoordinator)
        assertNotNull(app.audioExtractor)

        // Verify Room SQLite native driver on macOS arm64 queries cleanly
        val tracks = app.musicRepository.getAllTracks().first()
        assertNotNull(tracks)
        val playlists = app.musicRepository.getAllPlaylists().first()
        assertNotNull(playlists)
    }

    @Test
    fun `all 27 HiltViewModels instantiate via Dagger subcomponent`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "artistName" to "Test Artist",
                "albumName" to "Test Album",
                "playlistId" to 1L,
                "browseId" to "MPREb_test",
                "artistId" to "UC_test",
                "genre" to "All",
                "shareId" to "test_share_id",
                "link" to "stash://track?t=Test&a=Artist",
            )
        )
        val probeComponent = runtime.component.viewModelComponentFactory().create(savedStateHandle)
        val vmClasses = probeComponent.viewModels.keys
        assertEquals(27, vmClasses.size, "Expected all 27 @HiltViewModel bindings in map")

        for (vmClass in vmClasses) {
            val instance = StashDesktopRuntime.runOnMainThread {
                runtime.component.viewModelComponentFactory().create(savedStateHandle).viewModels.getValue(vmClass).get()
            }
            assertNotNull(instance, "Failed to instantiate ViewModel ${vmClass.name}")
            assertTrue(vmClass.isInstance(instance), "${instance::class.java.name} is not an instance of ${vmClass.name}")
        }
    }

    @Test
    fun `all 24 HiltWorkers instantiate via HiltWorkerFactory`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val app = runtime.application

        val workerClasses = listOf(
            ArtBackfillWorker::class.java,
            ArtistImageBackfillWorker::class.java,
            BlocklistIntegrityWorker::class.java,
            DiffWorker::class.java,
            DiscoveryDownloadWorker::class.java,
            FlacUpgradeWorker::class.java,
            LosslessRetryWorker::class.java,
            LoudnessBackfillWorker::class.java,
            LyricsFetchWorker::class.java,
            LyricsTtmlUpgradeWorker::class.java,
            MetadataBackfillWorker::class.java,
            PlaylistFetchWorker::class.java,
            QualityInfoBackfillWorker::class.java,
            SharedMixFollowWorker::class.java,
            SharedMixPublishWorker::class.java,
            SharedMixUnshareWorker::class.java,
            StashDiscoveryWorker::class.java,
            StashMixRefreshWorker::class.java,
            SyncFinalizeWorker::class.java,
            TagEnrichmentWorker::class.java,
            TrackDownloadWorker::class.java,
            TrackInfoEnrichmentWorker::class.java,
            YtDlpUpdateWorker::class.java,
            YtLibraryBackfillWorker::class.java,
        )

        assertEquals(24, workerClasses.size)
        for (workerClass in workerClasses) {
            val params = WorkerParameters(
                id = UUID.randomUUID(),
                inputData = Data.EMPTY,
                tags = emptySet(),
                runAttemptCount = 0,
                onProgress = {},
                onForeground = {},
            )
            val worker = app.workerFactory.createWorker(app, workerClass.name, params)
            assertNotNull(worker, "Failed to create @HiltWorker ${workerClass.name}")
            assertTrue(workerClass.isInstance(worker))
        }
    }

    @Test
    fun `5-stage 16-bit PCM DSP chain processes audio frames cleanly via StashRenderersFactory and DefaultAudioSink`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val app = runtime.application
        val service = StashPlaybackService().also { runtime.component.inject(it) }

        service.eqController.setEnabled(true)
        service.eqController.setPreampDb(2.5f)
        service.eqController.setBandGain(0, 3.0f)
        service.eqController.setBandGain(4, 1.5f)
        service.eqController.setBassBoostDb(4.0f)

        val renderersFactory = StashRenderersFactory(app, service.eqController, service.loudnessController)
        val sink = renderersFactory.buildAudioSink(app, false, false) as DefaultAudioSink
        assertEquals(5, sink.processors.size, "Expected 5-stage DSP AudioProcessor chain")

        val format = AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT)
        sink.configureChain(format)

        // Generate 1024 stereo frames (4096 bytes) of a 440Hz sine wave
        val frameCount = 1024
        val pcmBytes = ByteArray(frameCount * 4)
        val buf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount) {
            val sample = (sin(2.0 * PI * 440.0 * i / 44_100.0) * 12_000.0).toInt().toShort()
            buf.putShort(sample) // L
            buf.putShort(sample) // R
        }

        val outBuffer = sink.processPcm16(pcmBytes, pcmBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        val remainingBytes = outBuffer.remaining()
        assertEquals(pcmBytes.size, remainingBytes, "Expected frame-preserving PCM byte length")

        var nonZeroSamples = 0
        while (outBuffer.remaining() >= 2) {
            if (outBuffer.short != 0.toShort()) nonZeroSamples++
        }
        assertTrue(nonZeroSamples > frameCount, "Expected non-zero processed PCM samples")
    }

    @Test
    fun `StashPlaybackService starts and connects to MediaController and recovers after idle-stop`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val app = runtime.application

        val token = SessionToken(app, ComponentName(app, StashPlaybackService::class.java))
        val controller1 = MediaController.Builder(app, token).buildAsync().get(5, TimeUnit.SECONDS)

        assertNotNull(controller1)
        assertTrue(controller1.isConnected)

        // Simulate 5-minute idle-stop (stopSelf -> onDestroy)
        val firstService = androidx.media3.session.DesktopMediaServiceRegistry.activeService
        assertNotNull(firstService)
        firstService.stopSelf()
        assertTrue(!controller1.isConnected, "Expected old MediaController to report disconnected after service stopSelf()")
        controller1.release()

        // Reconnect after idle-stop (simulating user clicking a song after 5 minutes idle)
        val controller2 = MediaController.Builder(app, token).buildAsync().get(5, TimeUnit.SECONDS)
        assertNotNull(controller2)
        assertTrue(controller2.isConnected)

        val item = androidx.media3.common.MediaItem.Builder()
            .setMediaId("4313")
            .setUri(android.net.Uri.parse("https://rr1---sn-test.googlevideo.com/videoplayback?clen=1000"))
            .build()
        controller2.setMediaItem(item)
        assertEquals(1, controller2.mediaItemCount, "Expected setMediaItem to populate timeline synchronously")
        assertEquals("4313", controller2.currentMediaItem?.mediaId)
        controller2.release()
    }

    @Test
    fun `StashApplication and MainActivity onCreate execute cleanly and install Compose root`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val app = runtime.application

        StashDesktopRuntime.runOnMainThread {
            app.onCreate()
        }

        val mainActivity = StashDesktopRuntime.runOnMainThread {
            com.stash.app.MainActivity().also { activity ->
                runtime.component.inject(activity)
                activity.onCreate(null)
            }
        }
        assertNotNull(mainActivity.composeContent, "Expected MainActivity.setContent to register composeContent")
        StashDesktopRuntime.runOnMainThread {
            mainActivity.onDestroy()
        }
    }

    @Test
    fun `Compose Navigation Enum NavType resolves SearchAlbumRoute AlbumSource`() {
        val provider = androidx.navigation.NavigatorProvider().apply {
            addNavigator(androidx.navigation.NavGraphNavigator(this))
            addNavigator(androidx.navigation.compose.ComposeNavigator())
        }
        val builder = androidx.navigation.NavGraphBuilder(
            provider = provider,
            startDestination = com.stash.app.navigation.HomeRoute,
            route = null,
            typeMap = emptyMap(),
        )
        builder.apply {
            composable<com.stash.app.navigation.HomeRoute> {}
            composable<com.stash.app.navigation.SearchAlbumRoute> {}
        }
        val graph = builder.build()
        assertNotNull(graph)
    }

    @OptIn(
        androidx.compose.ui.ExperimentalComposeUiApi::class,
        androidx.compose.ui.InternalComposeUiApi::class,
    )
    @Test
    fun `StashScaffold renders and bottom navigation tabs respond to clicks`() {
        val runtime = StashDesktopRuntime.bootstrap(runOnCreate = false)
        val mainActivity = StashDesktopRuntime.runOnMainThread {
            com.stash.app.MainActivity().also { activity ->
                runtime.component.inject(activity)
                activity.onCreate(null)
            }
        }

        StashDesktopRuntime.runOnMainThread {
            val scene = androidx.compose.ui.ImageComposeScene(
                width = 1280,
                height = 860,
            )
            try {
                scene.setContent {
                    val hostView = androidx.compose.runtime.remember(mainActivity) { android.view.View(mainActivity) }
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalContext provides mainActivity,
                        androidx.compose.ui.platform.LocalView provides hostView,
                    ) {
                        mainActivity.composeContent?.invoke()
                    }
                }

                var nanoTime = 1_000_000L
                fun stepFrames(count: Int = 10, deltaMs: Long = 50L) {
                    repeat(count) {
                        nanoTime += deltaMs * 1_000_000L
                        scene.render(nanoTime)
                    }
                }

                stepFrames(10)

                fun collectTexts(): Set<String> {
                    val result = linkedSetOf<String>()
                    fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
                        node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
                            .forEach { result.add(it.text) }
                        node.children.forEach(::walk)
                    }
                    scene.semanticsOwners.forEach { walk(it.unmergedRootSemanticsNode) }
                    return result
                }

                fun findClickableByText(text: String): androidx.compose.ui.semantics.SemanticsNode? {
                    var found: androidx.compose.ui.semantics.SemanticsNode? = null
                    fun walk(
                        node: androidx.compose.ui.semantics.SemanticsNode,
                        clickableAncestor: androidx.compose.ui.semantics.SemanticsNode?,
                    ) {
                        val currentClickable = if (androidx.compose.ui.semantics.SemanticsActions.OnClick in node.config) {
                            node
                        } else {
                            clickableAncestor
                        }
                        val texts = node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
                        if (texts.any { it.text == text } && currentClickable != null) {
                            found = currentClickable
                        }
                        node.children.forEach { walk(it, currentClickable) }
                    }
                    scene.semanticsOwners.forEach { walk(it.unmergedRootSemanticsNode, null) }
                    return found
                }

                fun clickByText(text: String) {
                    val node = findClickableByText(text)
                    assertNotNull(node, "Could not find clickable node with text '$text'. Available texts: ${collectTexts()}")
                    node.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action?.invoke()
                    stepFrames(25, 50L)
                }

                // 1. Library tab
                clickByText("Library")
                assertTrue("Songs" in collectTexts() && "Playlists" in collectTexts(), "Expected Library screen content, got: ${collectTexts()}")

                // 2. Search tab
                clickByText("Search")
                assertTrue("Search YouTube Music" in collectTexts(), "Expected Search screen content, got: ${collectTexts()}")

                // 3. Sync tab
                clickByText("Sync")
                assertTrue("LAST SYNC" in collectTexts(), "Expected Sync screen content, got: ${collectTexts()}")

                // 4. Settings tab
                clickByText("Settings")
                assertTrue("Audio & Quality" in collectTexts() && "Playback" in collectTexts(), "Expected Settings screen content, got: ${collectTexts()}")

                // 5. Open a Settings sub-screen (Playback) to verify nested navigation & parent backStackEntry ViewModel sharing
                clickByText("Playback")
                assertTrue("Playback" in collectTexts() && "Audio & Quality" !in collectTexts(), "Expected SettingsPlaybackScreen content, got: ${collectTexts()}")

                // 6. Return to Home tab
                clickByText("Home")
                assertTrue("Pop/Rock" in collectTexts(), "Expected Home screen content, got: ${collectTexts()}")
            } finally {
                scene.close()
                mainActivity.onDestroy()
            }
        }
    }
}

