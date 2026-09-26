package android.os

/**
 * Desktop shim for android.os.SystemClock. Monotonic JVM clock (System.nanoTime); only
 * differences are meaningful, which is how upstream uses it.
 */
object SystemClock {
    @JvmStatic fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    @JvmStatic fun elapsedRealtimeNanos(): Long = System.nanoTime()
    @JvmStatic fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
}
