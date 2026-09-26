package androidx.media3.datasource

import java.io.IOException

/** Desktop port of media3 1.9.2 `DataSink`: a component to which streams of data can be written. */
interface DataSink {

    /** A factory for [DataSink] instances. */
    fun interface Factory {
        fun createDataSink(): DataSink
    }

    @Throws(IOException::class)
    fun open(dataSpec: DataSpec)

    @Throws(IOException::class)
    fun write(buffer: ByteArray, offset: Int, length: Int)

    @Throws(IOException::class)
    fun close()
}
