package com.stash.desktop.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
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
import com.stash.data.download.backfill.MetadataBackfillWorker
import com.stash.data.download.backfill.YtLibraryBackfillWorker
import com.stash.data.download.lossless.LosslessRetryWorker
import com.stash.data.download.ytdlp.YtDlpUpdateWorker
import com.stash.data.lyrics.worker.LyricsFetchWorker
import com.stash.data.lyrics.worker.LyricsTtmlUpgradeWorker
import dagger.assisted.AssistedFactory
import javax.inject.Inject
import javax.inject.Singleton

@AssistedFactory
interface ArtBackfillWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): ArtBackfillWorker
}

@AssistedFactory
interface SyncFinalizeWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): SyncFinalizeWorker
}

@AssistedFactory
interface ArtistImageBackfillWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): ArtistImageBackfillWorker
}

@AssistedFactory
interface TrackDownloadWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): TrackDownloadWorker
}

@AssistedFactory
interface FlacUpgradeWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): FlacUpgradeWorker
}

@AssistedFactory
interface BlocklistIntegrityWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): BlocklistIntegrityWorker
}

@AssistedFactory
interface QualityInfoBackfillWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): QualityInfoBackfillWorker
}

@AssistedFactory
interface TrackInfoEnrichmentWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): TrackInfoEnrichmentWorker
}

@AssistedFactory
interface LoudnessBackfillWorkerFactory {
    fun create(ctx: Context, params: WorkerParameters): LoudnessBackfillWorker
}

@AssistedFactory
interface DiscoveryDownloadWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): DiscoveryDownloadWorker
}

@AssistedFactory
interface TagEnrichmentWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): TagEnrichmentWorker
}

@AssistedFactory
interface DiffWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): DiffWorker
}

@AssistedFactory
interface PlaylistFetchWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): PlaylistFetchWorker
}

@AssistedFactory
interface StashDiscoveryWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): StashDiscoveryWorker
}

@AssistedFactory
interface StashMixRefreshWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): StashMixRefreshWorker
}

@AssistedFactory
interface LyricsFetchWorkerFactory {
    fun create(context: Context, params: WorkerParameters): LyricsFetchWorker
}

@AssistedFactory
interface LyricsTtmlUpgradeWorkerFactory {
    fun create(context: Context, params: WorkerParameters): LyricsTtmlUpgradeWorker
}

@AssistedFactory
interface SharedMixFollowWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): SharedMixFollowWorker
}

@AssistedFactory
interface SharedMixPublishWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): SharedMixPublishWorker
}

@AssistedFactory
interface SharedMixUnshareWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): SharedMixUnshareWorker
}

@AssistedFactory
interface LosslessRetryWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): LosslessRetryWorker
}

@AssistedFactory
interface YtDlpUpdateWorkerFactory {
    fun create(context: Context, params: WorkerParameters): YtDlpUpdateWorker
}

@AssistedFactory
interface YtLibraryBackfillWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): YtLibraryBackfillWorker
}

@AssistedFactory
interface MetadataBackfillWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): MetadataBackfillWorker
}

@Singleton
class DesktopWorkerRegistrar @Inject constructor(
    private val workerFactory: HiltWorkerFactory,
    private val artBackfill: ArtBackfillWorkerFactory,
    private val syncFinalize: SyncFinalizeWorkerFactory,
    private val artistImageBackfill: ArtistImageBackfillWorkerFactory,
    private val trackDownload: TrackDownloadWorkerFactory,
    private val flacUpgrade: FlacUpgradeWorkerFactory,
    private val blocklistIntegrity: BlocklistIntegrityWorkerFactory,
    private val qualityInfoBackfill: QualityInfoBackfillWorkerFactory,
    private val trackInfoEnrichment: TrackInfoEnrichmentWorkerFactory,
    private val loudnessBackfill: LoudnessBackfillWorkerFactory,
    private val discoveryDownload: DiscoveryDownloadWorkerFactory,
    private val tagEnrichment: TagEnrichmentWorkerFactory,
    private val diff: DiffWorkerFactory,
    private val playlistFetch: PlaylistFetchWorkerFactory,
    private val stashDiscovery: StashDiscoveryWorkerFactory,
    private val stashMixRefresh: StashMixRefreshWorkerFactory,
    private val lyricsFetch: LyricsFetchWorkerFactory,
    private val lyricsTtmlUpgrade: LyricsTtmlUpgradeWorkerFactory,
    private val sharedMixFollow: SharedMixFollowWorkerFactory,
    private val sharedMixPublish: SharedMixPublishWorkerFactory,
    private val sharedMixUnshare: SharedMixUnshareWorkerFactory,
    private val losslessRetry: LosslessRetryWorkerFactory,
    private val ytDlpUpdate: YtDlpUpdateWorkerFactory,
    private val ytLibraryBackfill: YtLibraryBackfillWorkerFactory,
    private val metadataBackfill: MetadataBackfillWorkerFactory,
) {
    fun registerAll() {
        workerFactory.register<ArtBackfillWorker>(artBackfill::create)
        workerFactory.register<SyncFinalizeWorker>(syncFinalize::create)
        workerFactory.register<ArtistImageBackfillWorker>(artistImageBackfill::create)
        workerFactory.register<TrackDownloadWorker>(trackDownload::create)
        workerFactory.register<FlacUpgradeWorker>(flacUpgrade::create)
        workerFactory.register<BlocklistIntegrityWorker>(blocklistIntegrity::create)
        workerFactory.register<QualityInfoBackfillWorker>(qualityInfoBackfill::create)
        workerFactory.register<TrackInfoEnrichmentWorker>(trackInfoEnrichment::create)
        workerFactory.register<LoudnessBackfillWorker>(loudnessBackfill::create)
        workerFactory.register<DiscoveryDownloadWorker>(discoveryDownload::create)
        workerFactory.register<TagEnrichmentWorker>(tagEnrichment::create)
        workerFactory.register<DiffWorker>(diff::create)
        workerFactory.register<PlaylistFetchWorker>(playlistFetch::create)
        workerFactory.register<StashDiscoveryWorker>(stashDiscovery::create)
        workerFactory.register<StashMixRefreshWorker>(stashMixRefresh::create)
        workerFactory.register<LyricsFetchWorker>(lyricsFetch::create)
        workerFactory.register<LyricsTtmlUpgradeWorker>(lyricsTtmlUpgrade::create)
        workerFactory.register<SharedMixFollowWorker>(sharedMixFollow::create)
        workerFactory.register<SharedMixPublishWorker>(sharedMixPublish::create)
        workerFactory.register<SharedMixUnshareWorker>(sharedMixUnshare::create)
        workerFactory.register<LosslessRetryWorker>(losslessRetry::create)
        workerFactory.register<YtDlpUpdateWorker>(ytDlpUpdate::create)
        workerFactory.register<YtLibraryBackfillWorker>(ytLibraryBackfill::create)
        workerFactory.register<MetadataBackfillWorker>(metadataBackfill::create)
    }
}
