package android.os

import java.io.File

/** Desktop shim for android.os.StatFs, backed by java.io.File space queries on the containing volume. */
class StatFs(path: String) {
    private val file = File(path)
    val availableBytes: Long get() = file.usableSpace
    val freeBytes: Long get() = file.freeSpace
    val totalBytes: Long get() = file.totalSpace
}
