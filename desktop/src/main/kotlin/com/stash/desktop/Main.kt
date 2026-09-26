package com.stash.desktop

import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.media3.session.DesktopMediaServiceRegistry
import androidx.work.WorkManager
import com.stash.app.MainActivity
import com.stash.app.StashApplication
import com.stash.core.media.service.StashPlaybackService
import com.stash.desktop.di.DaggerDesktopAppComponent
import com.stash.desktop.di.DesktopAppComponent
import com.stash.desktop.di.DesktopViewModelFactory
import java.awt.Dimension
import java.awt.Taskbar
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import org.jetbrains.skia.Image as SkiaImage

object StashDesktopRuntime {
    class Instance(
        val application: StashApplication,
        val component: DesktopAppComponent,
    )

    @Volatile
    private var current: Instance? = null

    fun <T> runOnMainThread(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) {
            return block()
        }
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait {
            result = runCatching(block)
        }
        return result!!.getOrThrow()
    }

    @Synchronized
    fun bootstrap(runOnCreate: Boolean = true): Instance {
        current?.let { return it }

        return runOnMainThread {
            val app = StashApplication()
            val component = DaggerDesktopAppComponent.factory().create(app)

            DesktopMediaServiceRegistry.serviceCreator = {
                StashPlaybackService().also { service ->
                    component.inject(service)
                }
            }

            component.inject(app)
            component.workerRegistrar().registerAll()

            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(app, app.workManagerConfiguration)
            }

            DesktopViewModelFactory.creator = { modelClass, savedStateHandle ->
                runOnMainThread {
                    val vmComponent = component.viewModelComponentFactory().create(savedStateHandle)
                    val provider = vmComponent.viewModels[modelClass]
                        ?: error("No @HiltViewModel binding registered for ${modelClass.name}")
                    provider.get()
                }
            }

            if (runOnCreate) {
                app.onCreate()
            }

            Instance(app, component).also { current = it }
        }
    }
}

private fun loadAndroidLauncherIconBytes(): ByteArray? =
    StashDesktopRuntime::class.java.getResourceAsStream("/stash-res/com.stash.app/mipmap/ic_launcher_round.png")?.use { it.readBytes() }
        ?: StashDesktopRuntime::class.java.getResourceAsStream("/stash-res/com.stash.app/mipmap/ic_launcher.png")?.use { it.readBytes() }

fun main() {
    val iconBytes = loadAndroidLauncherIconBytes()
    if (iconBytes != null) {
        runCatching {
            if (Taskbar.isTaskbarSupported()) {
                val taskbar = Taskbar.getTaskbar()
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    val awtImage = ImageIO.read(iconBytes.inputStream())
                    if (awtImage != null) {
                        taskbar.iconImage = awtImage
                    }
                }
            }
        }
    }

    val runtime = StashDesktopRuntime.bootstrap(runOnCreate = true)
    val mainActivity = StashDesktopRuntime.runOnMainThread {
        MainActivity().also { activity ->
            runtime.component.inject(activity)
            activity.onCreate(null)
        }
    }

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 860.dp)
        val windowIcon = remember(iconBytes) {
            iconBytes?.let { bytes ->
                runCatching {
                    BitmapPainter(SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap())
                }.getOrNull()
            }
        }
        Window(
            onCloseRequest = {
                mainActivity.onDestroy()
                exitApplication()
            },
            title = "Stash",
            icon = windowIcon,
            state = windowState,
        ) {
            window.minimumSize = Dimension(900, 640)
            val hostView = remember(mainActivity) { View(mainActivity) }
            CompositionLocalProvider(
                LocalContext provides mainActivity,
                LocalView provides hostView,
            ) {
                mainActivity.composeContent?.invoke()
            }
        }
    }
}
