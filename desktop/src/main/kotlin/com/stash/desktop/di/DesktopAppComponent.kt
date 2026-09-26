package com.stash.desktop.di

import android.app.Application
import android.content.Context
import com.stash.app.MainActivity
import com.stash.app.ResumePlaybackActivity
import com.stash.app.StashApplication
import com.stash.app.di.AppVersionModule
import com.stash.app.di.LastFmCredentialsModule
import com.stash.app.di.LyricsFetchTriggerModule
import com.stash.app.di.LyricsUpgradeTriggerModule
import com.stash.app.di.VersionCodeModule
import com.stash.core.auth.di.AuthModule
import com.stash.core.data.di.ArtistEnrichmentModule
import com.stash.core.data.di.AudioModule
import com.stash.core.data.di.DatabaseModule
import com.stash.core.data.di.DiagnosticsModule
import com.stash.core.data.di.RepositoryModule
import com.stash.core.data.sync.BootReceiver
import com.stash.core.data.tipjar.di.TipJarModule
import com.stash.core.media.di.MediaModule
import com.stash.core.media.preview.di.PreviewCacheModule
import com.stash.core.media.service.StashPlaybackService
import com.stash.core.media.streaming.StreamCacheModule
import com.stash.core.network.di.NetworkInterceptorsModule
import com.stash.core.network.di.NetworkModule
import com.stash.data.download.di.DownloadModule
import com.stash.data.download.files.FileExistenceCheckerModule
import com.stash.data.download.lossless.di.LosslessModule
import com.stash.data.download.lossless.di.UpgraderModule
import com.stash.data.download.lossless.kennyy.di.KennyyModule
import com.stash.data.download.lossless.qbdlx.di.QbdlxModule
import com.stash.data.download.lossless.qobuz.di.QobuzModule
import com.stash.data.lyrics.di.LyricsModule
import com.stash.data.spotify.di.SpotifyDataModule
import com.stash.data.ytmusic.di.YTMusicDataModule
import com.stash.feature.settings.di.SettingsModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module(subcomponents = [DesktopViewModelComponent::class])
object DesktopPlatformModule {
    @Provides
    @Singleton
    @ApplicationContext
    fun provideApplicationContext(app: Application): Context = app

    @Provides
    @Singleton
    fun provideContext(app: Application): Context = app
}

@Singleton
@Component(
    modules = [
        DesktopPlatformModule::class,
        DesktopNetworkModule::class,
        AuthModule::class,
        NetworkModule::class,
        NetworkInterceptorsModule::class,
        AudioModule::class,
        RepositoryModule::class,
        DiagnosticsModule::class,
        ArtistEnrichmentModule::class,
        DatabaseModule::class,
        TipJarModule::class,
        MediaModule::class,
        StreamCacheModule::class,
        PreviewCacheModule::class,
        LyricsModule::class,
        YTMusicDataModule::class,
        SpotifyDataModule::class,
        LosslessModule::class,
        UpgraderModule::class,
        KennyyModule::class,
        QobuzModule::class,
        QbdlxModule::class,
        DownloadModule::class,
        FileExistenceCheckerModule::class,
        SettingsModule::class,
        AppVersionModule::class,
        LastFmCredentialsModule::class,
        VersionCodeModule::class,
        LyricsFetchTriggerModule::class,
        LyricsUpgradeTriggerModule::class,
    ],
)
interface DesktopAppComponent {
    fun viewModelComponentFactory(): DesktopViewModelComponent.Factory
    fun workerRegistrar(): DesktopWorkerRegistrar

    fun inject(app: StashApplication)
    fun inject(activity: MainActivity)
    fun inject(activity: ResumePlaybackActivity)
    fun inject(service: StashPlaybackService)
    fun inject(receiver: BootReceiver)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: Application): DesktopAppComponent
    }
}
