package android.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** Desktop port of android.util.AtomicFile: writes go to a .new file renamed over the base on finish. */
class AtomicFile(val baseFile: File) {
    private val newName = File(baseFile.path + ".new")
    private val legacyBackup = File(baseFile.path + ".bak")

    fun startWrite(): FileOutputStream {
        if (legacyBackup.exists()) legacyBackup.renameTo(baseFile)
        newName.parentFile?.mkdirs()
        return try { FileOutputStream(newName) } catch (e: IOException) { throw IOException("Failed to create new file $newName", e) }
    }

    fun finishWrite(str: FileOutputStream?) {
        str ?: return
        runCatching { str.fd.sync() }
        runCatching { str.close() }
        if (!newName.renameTo(baseFile)) {
            baseFile.delete()
            newName.renameTo(baseFile)
        }
    }

    fun failWrite(str: FileOutputStream?) {
        str ?: return
        runCatching { str.close() }
        newName.delete()
    }

    fun delete() {
        baseFile.delete(); newName.delete(); legacyBackup.delete()
    }

    fun exists(): Boolean = baseFile.exists() || legacyBackup.exists()

    fun openRead(): FileInputStream {
        if (legacyBackup.exists()) legacyBackup.renameTo(baseFile)
        return FileInputStream(baseFile)
    }

    fun readFully(): ByteArray = openRead().use { it.readBytes() }
}
