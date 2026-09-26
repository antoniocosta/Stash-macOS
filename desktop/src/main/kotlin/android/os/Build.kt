package android.os

/**
 * Desktop shim for android.os.Build. SDK_INT is 35 (Android 15) so upstream takes its
 * modern code paths; device fields describe the real Mac (hw.model via sysctl, macOS version).
 */
object Build {
    @JvmField val MANUFACTURER: String = "Apple"
    @JvmField val BRAND: String = "Apple"
    @JvmField val MODEL: String = sysctl("hw.model") ?: "Mac"
    @JvmField val DEVICE: String = MODEL
    @JvmField val PRODUCT: String = "macOS"
    @JvmField val DISPLAY: String =
        "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")}), JVM ${System.getProperty("java.version")}"

    object VERSION {
        @JvmField val SDK_INT: Int = VERSION_CODES.VANILLA_ICE_CREAM
        @JvmField val RELEASE: String = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
    }

    object VERSION_CODES {
        const val LOLLIPOP = 21
        const val M = 23
        const val N = 24
        const val O = 26
        const val O_MR1 = 27
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val S_V2 = 32
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
        const val VANILLA_ICE_CREAM = 35
    }

    private fun sysctl(name: String): String? = runCatching {
        val p = ProcessBuilder("sysctl", "-n", name).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }.takeIf { it.isNotEmpty() && p.exitValue() == 0 }
    }.getOrNull()
}
