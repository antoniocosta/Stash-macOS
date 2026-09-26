package com.yausername.youtubedl_android

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop port of youtubedl-android 0.17's YoutubeDL (same API) running a real
 * macOS yt-dlp. Like the Android library it keeps its own updatable yt-dlp copy
 * (`updateYoutubeDL` downloads `yt-dlp_macos` from the channel's GitHub
 * releases); until the first update it uses the Homebrew yt-dlp.
 */
object YoutubeDL {

    private const val TAG = "YoutubeDL"
    private val progressRegex = Regex("""\[download\]\s+(\d+\.\d)% .* ETA (\d+):(\d+)""")
    private val searchDirs = listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")
    private val idProcessMap = ConcurrentHashMap<String, Process>()

    @Volatile private var ownDir: File? = null
    @Volatile private var initialized = false

    @JvmStatic fun getInstance(): YoutubeDL = this

    @Synchronized
    @Throws(YoutubeDLException::class)
    fun init(appContext: Context) {
        if (initialized) return
        ownDir = File(appContext.noBackupFilesDir.parentFile, "yt-dlp").apply { mkdirs() }
        if (resolveBinary() == null) {
            throw YoutubeDLException("yt-dlp not found. Install it with: brew install yt-dlp ffmpeg")
        }
        linkNativeTools(appContext)
        initialized = true
    }

    /**
     * Upstream finds its helper executables as .so files in nativeLibraryDir
     * (libqjs.so = QuickJS, libffmpeg.so = ffmpeg). Point those names at the
     * system binaries. Called at app start too, since FFmpegBridge may run first.
     */
    fun linkNativeTools(context: Context) {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir).apply { mkdirs() }
        mapOf("libqjs.so" to "qjs", "libffmpeg.so" to "ffmpeg").forEach { (lib, tool) ->
            val target = which(tool) ?: return@forEach
            val link = File(nativeDir, lib)
            runCatching {
                if (!link.exists()) java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath())
            }.onFailure { Log.w(TAG, "could not link $lib: ${it.message}") }
        }
    }

    private fun ownBinary(): File? = ownDir?.let { File(it, "yt-dlp_macos") }?.takeIf { it.canExecute() }

    private fun resolveBinary(): File? = ownBinary() ?: which("yt-dlp")

    internal fun which(name: String): File? =
        (System.getenv("PATH").orEmpty().split(File.pathSeparator) + searchDirs)
            .map { File(it, name) }.firstOrNull { it.canExecute() }

    @JvmOverloads
    @Throws(YoutubeDLException::class, InterruptedException::class, CanceledException::class)
    fun execute(
        request: YoutubeDLRequest,
        processId: String? = null,
        callback: ((Float, Long, String) -> Unit)? = null,
    ): YoutubeDLResponse {
        check(initialized) { "instance not initialized" }
        if (processId != null && idProcessMap.containsKey(processId)) {
            throw YoutubeDLException("Process ID already exists")
        }
        val binary = resolveBinary() ?: throw YoutubeDLException("yt-dlp not found")
        if (!request.hasOption("--ffmpeg-location")) {
            which("ffmpeg")?.let { request.addOption("--ffmpeg-location", it.absolutePath) }
        }
        val command = listOf(binary.absolutePath) + request.buildCommand()
        val start = System.currentTimeMillis()
        val pb = ProcessBuilder(command)
        pb.environment()["PATH"] = (searchDirs + System.getenv("PATH").orEmpty()).joinToString(File.pathSeparator)
        val process = try {
            pb.start()
        } catch (e: Exception) {
            throw YoutubeDLException(e)
        }
        if (processId != null) idProcessMap[processId] = process

        val out = StringBuilder()
        val err = StringBuilder()
        val errThread = Thread { process.errorStream.bufferedReader().forEachLine { err.append(it).append('\n') } }
            .apply { isDaemon = true; start() }
        try {
            process.inputStream.bufferedReader().forEachLine { line ->
                out.append(line).append('\n')
                if (callback != null) {
                    progressRegex.find(line)?.let { m ->
                        val progress = m.groupValues[1].toFloat()
                        val eta = m.groupValues[2].toLong() * 60 + m.groupValues[3].toLong()
                        callback(progress, eta, line)
                    }
                }
            }
            errThread.join()
        } catch (e: InterruptedException) {
            process.destroy()
            if (processId != null) idProcessMap.remove(processId)
            throw e
        }
        val exitCode = process.waitFor()
        val outStr = out.toString()
        val errStr = err.toString()
        if (exitCode > 0) {
            if (processId != null && !idProcessMap.containsKey(processId)) throw CanceledException()
            if (!ignoreErrors(request, outStr)) {
                if (processId != null) idProcessMap.remove(processId)
                throw YoutubeDLException(errStr)
            }
        }
        if (processId != null) idProcessMap.remove(processId)
        return YoutubeDLResponse(command, exitCode, System.currentTimeMillis() - start, outStr, errStr)
    }

    private fun ignoreErrors(request: YoutubeDLRequest, out: String) =
        request.hasOption("--dump-json") && out.isNotEmpty() && request.hasOption("--ignore-errors")

    fun destroyProcessById(id: String): Boolean {
        val p = idProcessMap.remove(id) ?: return false
        p.destroy()
        return true
    }

    @Synchronized
    @Throws(YoutubeDLException::class)
    fun updateYoutubeDL(appContext: Context, updateChannel: UpdateChannel = UpdateChannel.STABLE): UpdateStatus? {
        if (!initialized) init(appContext)
        try {
            val release = Json.parseToJsonElement(httpGet(updateChannel.apiUrl)).jsonObject
            val tag = release["tag_name"]!!.jsonPrimitive.content
            if (version(appContext) == tag) return UpdateStatus.ALREADY_UP_TO_DATE
            val url = release["assets"]!!.jsonArray
                .map { it.jsonObject }
                .first { it["name"]?.jsonPrimitive?.content == "yt-dlp_macos" }["browser_download_url"]!!
                .jsonPrimitive.content
            val dir = ownDir!!
            val tmp = File(dir, "yt-dlp_macos.download")
            download(url, tmp)
            tmp.setExecutable(true)
            val target = File(dir, "yt-dlp_macos")
            if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete(); target.setExecutable(true) }
            cachedVersion = null
            return UpdateStatus.DONE
        } catch (e: YoutubeDLException) {
            throw e
        } catch (e: Exception) {
            throw YoutubeDLException("failed to update youtube-dl", e)
        }
    }

    @Volatile private var cachedVersion: Pair<String, String>? = null

    fun version(appContext: Context?): String? {
        val bin = resolveBinary() ?: return null
        cachedVersion?.let { (path, v) -> if (path == bin.absolutePath) return v }
        return runCatching {
            val p = ProcessBuilder(bin.absolutePath, "--version").redirectErrorStream(true).start()
            val v = p.inputStream.bufferedReader().readText().trim()
            if (p.waitFor() == 0 && v.isNotEmpty()) v else null
        }.getOrNull()?.also { cachedVersion = bin.absolutePath to it }
    }

    fun versionName(appContext: Context?): String? = version(appContext)

    private fun httpGet(url: String): String {
        val c = URI(url).toURL().openConnection() as HttpURLConnection
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.connectTimeout = 15_000; c.readTimeout = 30_000
        return c.inputStream.use { it.readBytes().decodeToString() }
    }

    private fun download(url: String, to: File) {
        val c = URI(url).toURL().openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        c.connectTimeout = 15_000; c.readTimeout = 120_000
        c.inputStream.use { input -> to.outputStream().use { input.copyTo(it) } }
    }

    enum class UpdateStatus { DONE, ALREADY_UP_TO_DATE }

    sealed class UpdateChannel(val apiUrl: String) {
        object _STABLE : UpdateChannel("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
        object _NIGHTLY : UpdateChannel("https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest")
        object _MASTER : UpdateChannel("https://api.github.com/repos/yt-dlp/yt-dlp-master-builds/releases/latest")

        companion object {
            @JvmField val STABLE: _STABLE = _STABLE
            @JvmField val NIGHTLY: _NIGHTLY = _NIGHTLY
            @JvmField val MASTER: _MASTER = _MASTER
        }
    }

    class CanceledException : Exception()
}

class YoutubeDLException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, e: Throwable?) : super(message, e)
    constructor(e: Throwable?) : super(e)
}

class YoutubeDLResponse(
    val command: List<String?>,
    val exitCode: Int,
    val elapsedTime: Long,
    val out: String,
    val err: String,
)

class YoutubeDLRequest {
    private val urls: List<String>
    private val options = YoutubeDLOptions()
    private val customCommandList = ArrayList<String>()

    constructor(url: String) { urls = listOf(url) }
    constructor(urls: List<String>) { this.urls = urls }

    fun addOption(option: String, argument: String): YoutubeDLRequest = apply { options.addOption(option, argument) }
    fun addOption(option: String, argument: Number): YoutubeDLRequest = apply { options.addOption(option, argument.toString()) }
    fun addOption(option: String): YoutubeDLRequest = apply { options.addOption(option) }
    fun addCommands(commands: List<String>): YoutubeDLRequest = apply { customCommandList.addAll(commands) }
    fun getOption(option: String): String? = options.getArgument(option)
    fun getArguments(option: String): List<String?>? = options.getArguments(option)
    fun hasOption(option: String): Boolean = options.hasOption(option)

    fun buildCommand(): List<String> = options.buildOptions() + customCommandList + urls

    private class YoutubeDLOptions {
        private val map = LinkedHashMap<String, MutableList<String?>>()
        fun addOption(option: String, argument: String) { map.getOrPut(option) { ArrayList() } += argument }
        fun addOption(option: String) { map.getOrPut(option) { ArrayList() } += null }
        fun getArgument(option: String): String? = map[option]?.firstOrNull()
        fun getArguments(option: String): List<String?>? = map[option]
        fun hasOption(option: String) = map.containsKey(option)
        fun buildOptions(): List<String> = map.flatMap { (k, vs) -> vs.flatMap { v -> if (v == null) listOf(k) else listOf(k, v) } }
    }
}
