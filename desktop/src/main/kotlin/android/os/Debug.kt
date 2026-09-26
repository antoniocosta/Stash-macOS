package android.os

import java.lang.management.ManagementFactory

/**
 * Desktop shim for android.os.Debug. MemoryInfo reports real JVM numbers in KB:
 * java-heap = heap used, native-heap = non-heap used, total-pss = process RSS (ps);
 * stats with no desktop equivalent (e.g. summary.graphics) return null, as Android does
 * for unknown stat names.
 */
object Debug {
    class MemoryInfo {
        internal val stats = HashMap<String, String>()
        fun getMemoryStat(statName: String): String? = stats[statName]
    }

    @JvmStatic
    fun getMemoryInfo(memoryInfo: MemoryInfo) {
        val mx = ManagementFactory.getMemoryMXBean()
        memoryInfo.stats["summary.java-heap"] = (mx.heapMemoryUsage.used / 1024).toString()
        memoryInfo.stats["summary.native-heap"] = (mx.nonHeapMemoryUsage.used / 1024).toString()
        rssKb()?.let { memoryInfo.stats["summary.total-pss"] = it.toString() }
    }

    private fun rssKb(): Long? = runCatching {
        val p = ProcessBuilder("ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).start()
        p.inputStream.bufferedReader().readText().trim().toLong().also { p.waitFor() }
    }.getOrNull()
}
