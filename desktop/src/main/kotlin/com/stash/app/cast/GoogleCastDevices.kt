package com.stash.app.cast

import android.content.Context
import android.util.Log
import com.stash.core.media.cast.CastConnection
import com.stash.core.media.cast.CastDevice
import com.stash.core.media.cast.CastDevices
import com.stash.core.media.cast.CastRemote
import com.stash.core.media.diagnostics.PlaybackDiagnosticsLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Desktop replacement for upstream's `GoogleCastDevices` (same class name and constructor, so
 * upstream's `CastModule` binds it unchanged). Google Cast is Play Services' Cast framework plus
 * androidx.mediarouter, neither of which exists on the JVM. Upstream's own path for a device
 * without Play Services is to stay [CastConnection.Unavailable], which hides the cast button;
 * this is that path. See desktop/PORTING.md §3.
 */
@Singleton
class GoogleCastDevices @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnostics: PlaybackDiagnosticsLog,
) : CastDevices {
    override val connection: StateFlow<CastConnection> = MutableStateFlow(CastConnection.Unavailable).asStateFlow()
    override val devices: StateFlow<List<CastDevice>> = MutableStateFlow(emptyList<CastDevice>()).asStateFlow()
    override val remote: StateFlow<CastRemote?> = MutableStateFlow<CastRemote?>(null).asStateFlow()

    init {
        Log.i("GoogleCastDevices", "no Play Services (macOS) — Cast stays off")
        diagnostics.castConnection = "unavailable"
    }

    override fun startScan() = Unit
    override fun stopScan() = Unit
    override fun connect(deviceId: String) = Unit
    override fun disconnect() = Unit
}
