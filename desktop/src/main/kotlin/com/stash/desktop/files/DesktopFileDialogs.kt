package com.stash.desktop.files

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Native macOS open/save/folder dialogs (java.awt.FileDialog) backing the SAF activity-result contracts. */
object DesktopFileDialogs {
    private val extensionsByMime = mapOf(
        "audio" to setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "alac", "aiff", "aif", "wma", "webm"),
        "image" to setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic"),
        "video" to setOf("mp4", "mov", "m4v", "webm", "mkv"),
        "application/zip" to setOf("zip"),
        "application/json" to setOf("json"),
    )

    private fun accepts(mimeTypes: List<String>): ((File) -> Boolean)? {
        if (mimeTypes.isEmpty() || mimeTypes.any { it == "*/*" || it == "application/octet-stream" }) return null
        val exts = mimeTypes.flatMap { mime ->
            extensionsByMime[mime] ?: extensionsByMime[mime.substringBefore('/')].takeIf { mime.endsWith("/*") } ?: emptySet()
        }.toSet()
        if (exts.isEmpty()) return null
        return { f -> f.extension.lowercase() in exts }
    }

    fun open(mimeTypes: List<String>, multiple: Boolean): List<File> {
        val dialog = FileDialog(null as Frame?, "Open", FileDialog.LOAD)
        dialog.isMultipleMode = multiple
        accepts(mimeTypes)?.let { filter -> dialog.setFilenameFilter { dir, name -> filter(File(dir, name)) } }
        dialog.isVisible = true
        return dialog.files.orEmpty().toList()
    }

    fun save(suggestedName: String): File? {
        val dialog = FileDialog(null as Frame?, "Save", FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return File(dialog.directory, name)
    }

    fun chooseFolder(initial: String?): File? {
        val key = "apple.awt.fileDialogForDirectories"
        val previous = System.getProperty(key)
        System.setProperty(key, "true")
        try {
            val dialog = FileDialog(null as Frame?, "Choose Folder", FileDialog.LOAD)
            initial?.let { dialog.directory = it }
            dialog.isVisible = true
            val name = dialog.file ?: return null
            return File(dialog.directory, name)
        } finally {
            if (previous == null) System.clearProperty(key) else System.setProperty(key, previous)
        }
    }
}
