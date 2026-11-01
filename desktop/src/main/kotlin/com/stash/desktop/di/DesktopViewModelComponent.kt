package com.stash.desktop.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stash.app.ambient.AlbumColorsViewModel
import com.stash.app.navigation.QobuzStatusViewModel
import com.stash.feature.community.CommunityHostViewModel
import com.stash.feature.community.CommunityPickerViewModel
import com.stash.feature.community.CommunityPostViewModel
import com.stash.feature.community.CommunityViewModel
import com.stash.feature.community.PostComposerViewModel
import com.stash.feature.home.HomeViewModel
import com.stash.feature.home.PlaylistBrowseViewModel
import com.stash.feature.library.AlbumDetailViewModel
import com.stash.feature.library.ArtistDetailViewModel
import com.stash.feature.library.LibraryViewModel
import com.stash.feature.library.LikedSongsDetailViewModel
import com.stash.feature.library.PlaylistDetailViewModel
import com.stash.feature.library.allsongs.AllSongsViewModel
import com.stash.feature.library.mixbuilder.MixBuilderViewModel
import com.stash.feature.library.share.LinkImportViewModel
import com.stash.feature.library.share.ShareMixViewModel
import com.stash.feature.library.share.SharedMixViewModel
import com.stash.feature.library.share.SharedTrackViewModel
import com.stash.feature.listening.YourListeningViewModel
import com.stash.feature.listening.history.HistoryViewModel
import com.stash.feature.listening.recap.RecapViewModel
import com.stash.feature.listening.recap.lastfm.LastFmPastRecapsViewModel
import com.stash.feature.listening.stats.ListeningStatsViewModel
import com.stash.feature.listening.top.TopViewModel
import com.stash.feature.nowplaying.NowPlayingViewModel
import com.stash.feature.nowplaying.cast.CastViewModel
import com.stash.feature.nowplaying.handoff.HandoffViewModel
import com.stash.feature.nowplaying.listen.JoinSessionViewModel
import com.stash.feature.nowplaying.listen.ListenTogetherViewModel
import com.stash.feature.nowplaying.ui.SignalPathViewModel
import com.stash.feature.record.RecordViewModel
import com.stash.feature.record.card.YourRecordViewModel
import com.stash.feature.record.imports.RecordImportViewModel
import com.stash.feature.record.imports.RecordIntakeViewModel
import com.stash.feature.record.lastfm.RecordLastfmViewModel
import com.stash.feature.record.mixes.RecordMixViewModel
import com.stash.feature.record.mixes.RecordMixesShelfViewModel
import com.stash.feature.record.mixes.RecordMixesTabViewModel
import com.stash.feature.record.taste.RecordInMixesViewModel
import com.stash.feature.search.AlbumDiscoveryViewModel
import com.stash.feature.search.ArtistProfileViewModel
import com.stash.feature.search.SearchViewModel
import com.stash.feature.settings.BlockedSongsViewModel
import com.stash.feature.settings.FlacUpgradeViewModel
import com.stash.feature.settings.HiResOutputViewModel
import com.stash.feature.settings.HomeMixRotationViewModel
import com.stash.feature.settings.ListeningHistorySettingsViewModel
import com.stash.feature.settings.LosslessQuotaViewModel
import com.stash.feature.settings.LyricsSettingsViewModel
import com.stash.feature.settings.SettingsViewModel
import com.stash.feature.settings.SoundCloudAccountViewModel
import com.stash.feature.settings.SupporterViewModel
import com.stash.feature.settings.WebLibraryExportViewModel
import com.stash.feature.settings.WebLibraryImportViewModel
import com.stash.feature.settings.YourRecordSettingsViewModel
import com.stash.feature.settings.cloud.CloudSaveViewModel
import com.stash.feature.settings.cloud.CloudStorageViewModel
import com.stash.feature.settings.components.YouTubeProfileViewModel
import com.stash.feature.settings.diagnostics.DiagnosticsPreviewViewModel
import com.stash.feature.settings.equalizer.EqualizerViewModel
import com.stash.feature.settings.libraryhealth.LibraryHealthViewModel
import com.stash.feature.settings.libraryhealth.LyricsFetchViewModel
import com.stash.feature.settings.update.UpdateViewModel
import com.stash.feature.settings.weblink.InboxViewModel
import com.stash.feature.settings.weblink.MirrorViewModel
import com.stash.feature.settings.weblink.WebLinkViewModel
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

    @Binds @IntoMap @ClassKey(AlbumColorsViewModel::class)
    internal abstract fun bindAlbumColorsViewModel(vm: AlbumColorsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(FlacUpgradeViewModel::class)
    internal abstract fun bindFlacUpgradeViewModel(vm: FlacUpgradeViewModel): ViewModel

    @Binds @IntoMap @ClassKey(HiResOutputViewModel::class)
    internal abstract fun bindHiResOutputViewModel(vm: HiResOutputViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LosslessQuotaViewModel::class)
    internal abstract fun bindLosslessQuotaViewModel(vm: LosslessQuotaViewModel): ViewModel

    @Binds @IntoMap @ClassKey(WebLibraryImportViewModel::class)
    internal abstract fun bindWebLibraryImportViewModel(vm: WebLibraryImportViewModel): ViewModel

    @Binds @IntoMap @ClassKey(ListeningHistorySettingsViewModel::class)
    internal abstract fun bindListeningHistorySettingsViewModel(vm: ListeningHistorySettingsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(WebLibraryExportViewModel::class)
    internal abstract fun bindWebLibraryExportViewModel(vm: WebLibraryExportViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SoundCloudAccountViewModel::class)
    internal abstract fun bindSoundCloudAccountViewModel(vm: SoundCloudAccountViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SupporterViewModel::class)
    internal abstract fun bindSupporterViewModel(vm: SupporterViewModel): ViewModel

    @Binds @IntoMap @ClassKey(InboxViewModel::class)
    internal abstract fun bindInboxViewModel(vm: InboxViewModel): ViewModel

    @Binds @IntoMap @ClassKey(WebLinkViewModel::class)
    internal abstract fun bindWebLinkViewModel(vm: WebLinkViewModel): ViewModel

    @Binds @IntoMap @ClassKey(UpdateViewModel::class)
    internal abstract fun bindUpdateViewModel(vm: UpdateViewModel): ViewModel

    @Binds @IntoMap @ClassKey(MirrorViewModel::class)
    internal abstract fun bindMirrorViewModel(vm: MirrorViewModel): ViewModel

    @Binds @IntoMap @ClassKey(YouTubeProfileViewModel::class)
    internal abstract fun bindYouTubeProfileViewModel(vm: YouTubeProfileViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CloudSaveViewModel::class)
    internal abstract fun bindCloudSaveViewModel(vm: CloudSaveViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CloudStorageViewModel::class)
    internal abstract fun bindCloudStorageViewModel(vm: CloudStorageViewModel): ViewModel

    @Binds @IntoMap @ClassKey(YourListeningViewModel::class)
    internal abstract fun bindYourListeningViewModel(vm: YourListeningViewModel): ViewModel

    @Binds @IntoMap @ClassKey(TopViewModel::class)
    internal abstract fun bindTopViewModel(vm: TopViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecapViewModel::class)
    internal abstract fun bindRecapViewModel(vm: RecapViewModel): ViewModel

    @Binds @IntoMap @ClassKey(HistoryViewModel::class)
    internal abstract fun bindHistoryViewModel(vm: HistoryViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordViewModel::class)
    internal abstract fun bindRecordViewModel(vm: RecordViewModel): ViewModel

    @Binds @IntoMap @ClassKey(YourRecordViewModel::class)
    internal abstract fun bindYourRecordViewModel(vm: YourRecordViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordIntakeViewModel::class)
    internal abstract fun bindRecordIntakeViewModel(vm: RecordIntakeViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordImportViewModel::class)
    internal abstract fun bindRecordImportViewModel(vm: RecordImportViewModel): ViewModel

    @Binds @IntoMap @ClassKey(YourRecordSettingsViewModel::class)
    internal abstract fun bindYourRecordSettingsViewModel(vm: YourRecordSettingsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(HomeMixRotationViewModel::class)
    internal abstract fun bindHomeMixRotationViewModel(vm: HomeMixRotationViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordInMixesViewModel::class)
    internal abstract fun bindRecordInMixesViewModel(vm: RecordInMixesViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordMixesShelfViewModel::class)
    internal abstract fun bindRecordMixesShelfViewModel(vm: RecordMixesShelfViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CastViewModel::class)
    internal abstract fun bindCastViewModel(vm: CastViewModel): ViewModel

    @Binds @IntoMap @ClassKey(ListenTogetherViewModel::class)
    internal abstract fun bindListenTogetherViewModel(vm: ListenTogetherViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LastFmPastRecapsViewModel::class)
    internal abstract fun bindLastFmPastRecapsViewModel(vm: LastFmPastRecapsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(ListeningStatsViewModel::class)
    internal abstract fun bindListeningStatsViewModel(vm: ListeningStatsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordLastfmViewModel::class)
    internal abstract fun bindRecordLastfmViewModel(vm: RecordLastfmViewModel): ViewModel

    @Binds @IntoMap @ClassKey(SignalPathViewModel::class)
    internal abstract fun bindSignalPathViewModel(vm: SignalPathViewModel): ViewModel

    @Binds @IntoMap @ClassKey(LinkImportViewModel::class)
    internal abstract fun bindLinkImportViewModel(vm: LinkImportViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordMixViewModel::class)
    internal abstract fun bindRecordMixViewModel(vm: RecordMixViewModel): ViewModel

    @Binds @IntoMap @ClassKey(RecordMixesTabViewModel::class)
    internal abstract fun bindRecordMixesTabViewModel(vm: RecordMixesTabViewModel): ViewModel

    @Binds @IntoMap @ClassKey(HandoffViewModel::class)
    internal abstract fun bindHandoffViewModel(vm: HandoffViewModel): ViewModel

    @Binds @IntoMap @ClassKey(JoinSessionViewModel::class)
    internal abstract fun bindJoinSessionViewModel(vm: JoinSessionViewModel): ViewModel

    @Binds @IntoMap @ClassKey(AllSongsViewModel::class)
    internal abstract fun bindAllSongsViewModel(vm: AllSongsViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CommunityPostViewModel::class)
    internal abstract fun bindCommunityPostViewModel(vm: CommunityPostViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CommunityViewModel::class)
    internal abstract fun bindCommunityViewModel(vm: CommunityViewModel): ViewModel

    @Binds @IntoMap @ClassKey(PostComposerViewModel::class)
    internal abstract fun bindPostComposerViewModel(vm: PostComposerViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CommunityHostViewModel::class)
    internal abstract fun bindCommunityHostViewModel(vm: CommunityHostViewModel): ViewModel

    @Binds @IntoMap @ClassKey(CommunityPickerViewModel::class)
    internal abstract fun bindCommunityPickerViewModel(vm: CommunityPickerViewModel): ViewModel
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
