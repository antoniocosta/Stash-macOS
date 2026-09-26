package com.stash.desktop.media

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Runs the system ffprobe/ffmpeg (PATH, then /opt/homebrew/bin, then /usr/local/bin) and exposes
 * the probe result. Backs the android.media shims; a missing binary or unreadable file is an
 * [IOException], never invented metadata.
 */
internal object FfTools {
    private val json = Json { ignoreUnknownKeys = true }

    fun resolve(tool: String): File? {
        val dirs = System.getenv("PATH").orEmpty().split(File.pathSeparator) + "/opt/homebrew/bin" + "/usr/local/bin"
        return dirs.filter { it.isNotBlank() }.map { File(it, tool) }.firstOrNull { it.canExecute() }
    }

    fun probe(file: File): Probe {
        if (!file.isFile) throw IOException("${file.path} does not exist")
        val ffprobe = resolve("ffprobe") ?: throw IOException("ffprobe not found (install ffmpeg)")
        val out = exec(listOf(ffprobe.path, "-v", "error", "-print_format", "json", "-show_format", "-show_streams", "file:" + file.absolutePath))
        return Probe(json.parseToJsonElement(out.toString(Charsets.UTF_8)).jsonObject)
    }

    /** Raw bytes of stream [index] (e.g. an attached cover picture), or null if ffmpeg is unavailable/fails. */
    fun extractStream(file: File, index: Int): ByteArray? {
        val ffmpeg = resolve("ffmpeg") ?: return null
        return runCatching {
            exec(listOf(ffmpeg.path, "-v", "error", "-i", "file:" + file.absolutePath, "-map", "0:$index", "-c", "copy", "-f", "image2pipe", "-"))
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun exec(cmd: List<String>): ByteArray {
        val p = ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        p.outputStream.close()
        val bytes = p.inputStream.use { it.readBytes() }
        if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); throw IOException("${cmd[0]} timed out") }
        if (p.exitValue() != 0) throw IOException("${File(cmd[0]).name} failed (exit ${p.exitValue()}) for ${cmd.last { it.startsWith("file:") }}")
        return bytes
    }

    class Probe(root: JsonObject) {
        val format: JsonObject = root["format"] as? JsonObject ?: JsonObject(emptyMap())
        val streams: List<JsonObject> = (root["streams"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

        fun str(o: JsonObject, key: String): String? = (o[key] as? JsonPrimitive)?.contentOrNull
        fun int(o: JsonObject, key: String): Int? = (o[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.contentOrNull?.toIntOrNull() }

        fun isAttachedPic(s: JsonObject): Boolean =
            ((s["disposition"] as? JsonObject)?.get("attached_pic") as? JsonPrimitive)?.intOrNull == 1

        /** Case-insensitive tag lookup: container tags first, then stream tags (Ogg/Opus keep them there). */
        fun tag(name: String): String? =
            (listOf(format) + streams).firstNotNullOfOrNull { o ->
                (o["tags"] as? JsonObject)?.entries?.firstOrNull { it.key.equals(name, ignoreCase = true) }
                    ?.let { (it.value as? JsonPrimitive)?.contentOrNull }
            }
    }
}
