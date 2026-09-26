package com.stash.desktop.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stash.app.navigation.QobuzStatusViewModel
import com.stash.feature.home.HomeViewModel
import com.stash.feature.home.PlaylistBrowseViewModel
import com.stash.feature.library.AlbumDetailViewModel
import com.stash.feature.library.ArtistDetailViewModel
import com.stash.feature.library.LibraryViewModel
import com.stash.feature.library.LikedSongsDetailViewModel
import com.stash.feature.library.PlaylistDetailViewModel
import com.stash.feature.library.mixbuilder.MixBuilderViewModel
import com.stash.feature.library.share.ShareMixViewModel
import com.stash.feature.library.share.SharedMixViewModel
import com.stash.feature.library.share.SharedTrackViewModel
import com.stash.feature.nowplaying.NowPlayingViewModel
import com.stash.feature.search.AlbumDiscoveryViewModel
import com.stash.feature.search.ArtistProfileViewModel
import com.stash.feature.search.SearchViewModel
import com.stash.feature.settings.BlockedSongsViewModel
import com.stash.feature.settings.LyricsSettingsViewModel
import com.stash.feature.settings.SettingsViewModel
import com.stash.feature.settings.diagnostics.DiagnosticsPreviewViewModel
import com.stash.feature.settings.equalizer.EqualizerViewModel
import com.stash.feature.settings.libraryhealth.LibraryHealthViewModel
import com.stash.feature.settings.libraryhealth.LyricsFetchViewModel
import com.stash.feature.sync.DownloadManagementViewModel
import com.stash.feature.sync.FailedDownloadsViewModel
import com.stash.feature.sync.FailedMatchesViewModel
import com.stash.feature.sync.SyncViewModel
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Provider

@Module
abstract class DesktopViewModelModule {
    @Binds @IntoMap @ClassKey(QobuzStatusViewModel::class)
    internal abstract fun bindQobuzStatusViewModel(vm: QobuzStatusViewModel): ViewModel

    @Binds @IntoMap @ClassKey(BlockedSongsViewModel::class)
    internal abstract fun bindBlockedSongsViewModel(vm: BlockedSongsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LibraryHealthViewModel::class)
    internal abstract fun bindLibraryHealthViewModel(vm: LibraryHealthViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LyricsFetchViewModel::class)
    internal abstract fun bindLyricsFetchViewModel(vm: LyricsFetchViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LyricsSettingsViewModel::class)
    internal abstract fun bindLyricsSettingsViewModel(vm: LyricsSettingsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(EqualizerViewModel::class)
    internal abstract fun bindEqualizerViewModel(vm: EqualizerViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SettingsViewModel::class)
    internal abstract fun bindSettingsViewModel(vm: SettingsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(DiagnosticsPreviewViewModel::class)
    internal abstract fun bindDiagnosticsPreviewViewModel(vm: DiagnosticsPreviewViewModel): ViewModel

    @Binds @IntoMap @ClassKey(HomeViewModel::class)
    internal abstract fun bindHomeViewModel(vm: HomeViewModel): ViewModel

    @Binds @IntoMap @ClassKey(PlaylistBrowseViewModel::class)
    internal abstract fun bindPlaylistBrowseViewModel(vm: PlaylistBrowseViewModel): ViewModel

    @Binds @IntoMap @ClassKey(NowPlayingViewModel::class)
    internal abstract fun bindNowPlayingViewModel(vm: NowPlayingViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LibraryViewModel::class)
    internal abstract fun bindLibraryViewModel(vm: LibraryViewModel): ViewModel

    @Binds @IntoMap @ClassKey(ArtistDetailViewModel::class)
    internal abstract fun bindArtistDetailViewModel(vm: ArtistDetailViewModel): ViewModel

    @Binds @IntoMap @ClassKey(PlaylistDetailViewModel::class)
    internal abstract fun bindPlaylistDetailViewModel(vm: PlaylistDetailViewModel): ViewModel

    @Binds @IntoMap @ClassKey(MixBuilderViewModel::class)
    internal abstract fun bindMixBuilderViewModel(vm: MixBuilderViewModel): ViewModel

    @Binds @IntoMap @ClassKey(ShareMixViewModel::class)
    internal abstract fun bindShareMixViewModel(vm: ShareMixViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SharedMixViewModel::class)
    internal abstract fun bindSharedMixViewModel(vm: SharedMixViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SharedTrackViewModel::class)
    internal abstract fun bindSharedTrackViewModel(vm: SharedTrackViewModel): ViewModel

    @Binds @IntoMap @ClassKey(AlbumDetailViewModel::class)
    internal abstract fun bindAlbumDetailViewModel(vm: AlbumDetailViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LikedSongsDetailViewModel::class)
    internal abstract fun bindLikedSongsDetailViewModel(vm: LikedSongsDetailViewModel): ViewModel

    @Binds @IntoMap @ClassKey(AlbumDiscoveryViewModel::class)
    internal abstract fun bindAlbumDiscoveryViewModel(vm: AlbumDiscoveryViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SearchViewModel::class)
    internal abstract fun bindSearchViewModel(vm: SearchViewModel): ViewModel

    @Binds @IntoMap @ClassKey(ArtistProfileViewModel::class)
    internal abstract fun bindArtistProfileViewModel(vm: ArtistProfileViewModel): ViewModel

    @Binds @IntoMap @ClassKey(DownloadManagementViewModel::class)
    internal abstract fun bindDownloadManagementViewModel(vm: DownloadManagementViewModel): ViewModel

    @Binds @IntoMap @ClassKey(FailedMatchesViewModel::class)
    internal abstract fun bindFailedMatchesViewModel(vm: FailedMatchesViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SyncViewModel::class)
    internal abstract fun bindSyncViewModel(vm: SyncViewModel): ViewModel

    @Binds @IntoMap @ClassKey(FailedDownloadsViewModel::class)
    internal abstract fun bindFailedDownloadsViewModel(vm: FailedDownloadsViewModel): ViewModel
}

@ViewModelScoped
@Subcomponent(modules = [DesktopViewModelModule::class])
interface DesktopViewModelComponent {
    val viewModels: Map<Class<*>, @JvmSuppressWildcards Provider<ViewModel>>

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance savedStateHandle: SavedStateHandle): DesktopViewModelComponent
    }
}
