package android.os

/**
 * Desktop shim for android.os.PowerManager. macOS has no battery optimisation, Doze or app
 * standby; wake locks are bookkeeping only (a running Mac app is never suspended mid-task).
 */
class PowerManager internal constructor() {
    fun isIgnoringBatteryOptimizations(packageName: String): Boolean = true
    val isPowerSaveMode: Boolean get() = false
    val isDeviceIdleMode: Boolean get() = false
    val isInteractive: Boolean get() = true

    fun newWakeLock(levelAndFlags: Int, tag: String): WakeLock = WakeLock()

    class WakeLock internal constructor() {
        private var count = 0
        private var refCounted = true
        @Synchronized fun setReferenceCounted(value: Boolean) { refCounted = value }
        @Synchronized fun acquire() { count = if (refCounted) count + 1 else 1 }
        @Synchronized fun acquire(timeout: Long) = acquire()
        @Synchronized fun release() { count = if (refCounted) (count - 1).coerceAtLeast(0) else 0 }
        val isHeld: Boolean @Synchronized get() = count > 0
    }

    companion object {
        const val PARTIAL_WAKE_LOCK = 1
    }
}
