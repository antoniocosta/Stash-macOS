package androidx.media3.datasource

import androidx.media3.common.C
import java.io.IOException
import java.util.Arrays

/** Desktop port of media3 1.9.2 `DataSourceUtil`: utility methods for [DataSource]. */
object DataSourceUtil {

    /** Reads data from the specified opened [DataSource] until it ends. */
    @JvmStatic
    @Throws(IOException::class)
    fun readToEnd(dataSource: DataSource): ByteArray {
        var data = ByteArray(1024)
        var position = 0
        var bytesRead = 0
        while (bytesRead != C.RESULT_END_OF_INPUT) {
            if (position == data.size) data = Arrays.copyOf(data, data.size * 2)
            bytesRead = dataSource.read(data, position, data.size - position)
            if (bytesRead != C.RESULT_END_OF_INPUT) position += bytesRead
        }
        return Arrays.copyOf(data, position)
    }

    /** Reads [length] bytes from the specified opened [DataSource], throwing on premature end. */
    @JvmStatic
    @Throws(IOException::class)
    fun readExactly(dataSource: DataSource, length: Int): ByteArray {
        val data = ByteArray(length)
        var position = 0
        while (position < length) {
            val bytesRead = dataSource.read(data, position, data.size - position)
            if (bytesRead == C.RESULT_END_OF_INPUT) {
                throw IllegalStateException("Not enough data could be read: $position < $length")
            }
            position += bytesRead
        }
        return data
    }

    /** Closes a [DataSource], suppressing any [IOException] that may occur. */
    @JvmStatic
    fun closeQuietly(dataSource: DataSource?) {
        try {
            dataSource?.close()
        } catch (e: IOException) {
            // Ignore.
        }
    }
}
