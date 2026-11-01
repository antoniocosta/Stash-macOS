package android.os

import java.io.Closeable
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile

/** Desktop shim for android.os.ParcelFileDescriptor over a local file. */
class ParcelFileDescriptor private constructor(private val file: File, private val raf: RandomAccessFile) : Closeable {
    val fileDescriptor: FileDescriptor get() = raf.fd

    /** The file's size in bytes. */
    val statSize: Long get() = file.length()

    override fun close() = raf.close()

    /** Reads the descriptor's file; closing the stream closes the descriptor. */
    class AutoCloseInputStream(private val pfd: ParcelFileDescriptor) : FileInputStream(pfd.fileDescriptor) {
        override fun close() {
            try { super.close() } finally { pfd.close() }
        }
    }

    class AutoCloseOutputStream(private val pfd: ParcelFileDescriptor) : FileOutputStream(pfd.fileDescriptor) {
        override fun close() {
            try { super.close() } finally { pfd.close() }
        }
    }

    companion object {
        const val MODE_READ_ONLY = 0x10000000
        const val MODE_WRITE_ONLY = 0x20000000
        const val MODE_READ_WRITE = 0x30000000
        const val MODE_CREATE = 0x08000000
        const val MODE_TRUNCATE = 0x04000000
        const val MODE_APPEND = 0x02000000

        @JvmStatic
        fun parseMode(mode: String): Int = when (mode) {
            "r" -> MODE_READ_ONLY
            "w", "wt" -> MODE_WRITE_ONLY or MODE_CREATE or MODE_TRUNCATE
            "wa" -> MODE_WRITE_ONLY or MODE_CREATE or MODE_APPEND
            "rw" -> MODE_READ_WRITE or MODE_CREATE
            "rwt" -> MODE_READ_WRITE or MODE_CREATE or MODE_TRUNCATE
            else -> throw IllegalArgumentException("Bad mode '$mode'")
        }

        @JvmStatic
        fun open(file: File, mode: Int): ParcelFileDescriptor {
            val write = mode and MODE_WRITE_ONLY != 0
            if (write && mode and MODE_CREATE != 0) file.parentFile?.mkdirs()
            val raf = RandomAccessFile(file, if (write) "rw" else "r")
            if (mode and MODE_TRUNCATE != 0) raf.setLength(0)
            return ParcelFileDescriptor(file, raf)
        }
    }
}
