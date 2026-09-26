package android.content.pm

/** Desktop shim for android.content.pm.ServiceInfo: foreground-service type constants only (no services on desktop). */
open class ServiceInfo {
    companion object {
        const val FOREGROUND_SERVICE_TYPE_NONE = 0
        const val FOREGROUND_SERVICE_TYPE_DATA_SYNC = 1 shl 0
        const val FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = 1 shl 1
    }
}
