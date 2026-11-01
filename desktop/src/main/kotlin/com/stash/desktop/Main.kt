package com.stash.desktop

import android.app.DesktopActivityHost
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.stash.desktop.res.DesktopDisplay
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
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
            dagger.hilt.android.DesktopEntryPoints.component = component

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

    // The app language chosen in Settings (AppCompatDelegate) applies before anything resolves a string.
    AppCompatDelegate.restoreStoredLocales()

    val runtime = StashDesktopRuntime.bootstrap(runOnCreate = true)

    // ActivityThread's launch sequence: attach the base context (MainActivity applies its Display size
    // override there), inject, onCreate, onResume. A recreate hands the ViewModels over.
    fun launchMainActivity(previous: MainActivity?): MainActivity =
        MainActivity().also { activity ->
            activity.attach(runtime.application)
            previous?.let(activity::retainNonConfigurationState)
            runtime.component.inject(activity)
            activity.onCreate(null)
            activity.onResume()
            DesktopActivityHost.current = activity
        }

    val activityState = mutableStateOf(StashDesktopRuntime.runOnMainThread { launchMainActivity(null) })
    DesktopActivityHost.onRecreate = { old ->
        SwingUtilities.invokeLater {
            if (old === activityState.value) {
                old.changingConfigurations = true
                old.onPause()
                old.onDestroy()
                activityState.value = launchMainActivity(old as MainActivity)
            }
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
                activityState.value.onPause()
                activityState.value.onDestroy()
                exitApplication()
            },
            title = "Stash",
            icon = windowIcon,
            state = windowState,
        ) {
            window.minimumSize = Dimension(900, 640)
            DisposableEffect(window) {
                // Minimised = in the background (onPause); shown again = onResume.
                val listener = object : WindowAdapter() {
                    override fun windowIconified(e: WindowEvent) = activityState.value.onPause()
                    override fun windowDeiconified(e: WindowEvent) = activityState.value.onResume()
                }
                window.addWindowListener(listener)
                onDispose { window.removeWindowListener(listener) }
            }
            val activity = activityState.value
            val systemDensity = LocalDensity.current
            val display by DesktopDisplay.state
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // The window's content size and the screen's scale are the system configuration.
                val widthDp = maxWidth.value.toInt()
                val heightDp = maxHeight.value.toInt()
                LaunchedEffect(widthDp, heightDp, systemDensity.density) {
                    DesktopDisplay.update(widthDp, heightDp, systemDensity.density)
                }
                // The activity's configuration: the system one plus its override (Display size),
                // which sets the density the whole window lays out at.
                val configuration = remember(activity, display) { activity.resources.configuration }
                val density = Density(configuration.densityDpi / 160f, systemDensity.fontScale)
                val saveableStateHolder = rememberSaveableStateHolder()
                key(activity) {
                    val hostView = remember(activity) { View(activity) }
                    CompositionLocalProvider(
                        LocalContext provides activity,
                        LocalActivity provides activity,
                        LocalView provides hostView,
                        LocalConfiguration provides configuration,
                        LocalDensity provides density,
                    ) {
                        // The window's own ViewModelStore / lifecycle owners stay: they outlive a
                        // recreate, like Android's retained ViewModels. Saved UI state (the nav
                        // back stack included) survives it through the holder.
                        saveableStateHolder.SaveableStateProvider("main") {
                            activity.composeContent?.invoke()
                        }
                    }
                }
            }
        }
    }
}
