package android.net.wifi

/** Desktop shim for android.net.wifi.WifiManager: Wi-Fi locks are bookkeeping only on macOS. */
class WifiManager internal constructor() {
    fun createWifiLock(lockType: Int, tag: String): WifiLock = WifiLock()

    class WifiLock internal constructor() {
        private var count = 0
        private var refCounted = true
        @Synchronized fun setReferenceCounted(value: Boolean) { refCounted = value }
        @Synchronized fun acquire() { count = if (refCounted) count + 1 else 1 }
        @Synchronized fun release() { count = if (refCounted) (count - 1).coerceAtLeast(0) else 0 }
        val isHeld: Boolean @Synchronized get() = count > 0
    }

    companion object {
        const val WIFI_MODE_FULL = 1
        const val WIFI_MODE_FULL_HIGH_PERF = 3
        const val WIFI_MODE_FULL_LOW_LATENCY = 4
    }
}
