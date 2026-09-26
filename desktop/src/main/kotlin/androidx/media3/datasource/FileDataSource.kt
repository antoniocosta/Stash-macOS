package androidx.media3.datasource

import android.net.Uri
import androidx.media3.common.C
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile

/** Desktop port of media3 1.9.2 `FileDataSource`: a [DataSource] for reading local files. */
class FileDataSource : BaseDataSource(/* isNetwork = */ false) {

    /** Thrown when a [FileDataSource] encounters an error reading a file. */
    class FileDataSourceException : DataSourceException {
        constructor(cause: Throwable?, errorCode: Int) : super(cause, errorCode)
        constructor(message: String?, cause: Throwable?, errorCode: Int) : super(message, cause, errorCode)
    }

    /** [DataSource.Factory] for [FileDataSource] instances. */
    class Factory : DataSource.Factory {
        private var listener: TransferListener? = null

        fun setListener(listener: TransferListener?): Factory = apply { this.listener = listener }

        override fun createDataSource(): FileDataSource =
            FileDataSource().also { ds -> listener?.let { ds.addTransferListener(it) } }
    }

    private var file: RandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining = 0L
    private var opened = false

    @Throws(FileDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        this.uri = uri
        transferInitializing(dataSpec)
        val file = openLocalFile(uri)
        this.file = file
        try {
            file.seek(dataSpec.position)
            bytesRemaining = if (dataSpec.length == LENGTH_UNSET_LONG) file.length() - dataSpec.position else dataSpec.length
        } catch (e: IOException) {
            throw FileDataSourceException(e, DataSourceException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (bytesRemaining < 0) {
            throw FileDataSourceException(
                null, null, DataSourceException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            )
        }
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    @Throws(FileDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val bytesRead = try {
            file!!.read(buffer, offset, minOf(bytesRemaining, length.toLong()).toInt())
        } catch (e: IOException) {
            throw FileDataSourceException(e, DataSourceException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (bytesRead > 0) {
            bytesRemaining -= bytesRead
            bytesTransferred(bytesRead)
        }
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    @Throws(FileDataSourceException::class)
    override fun close() {
        uri = null
        try {
            file?.close()
        } catch (e: IOException) {
            throw FileDataSourceException(e, DataSourceException.ERROR_CODE_IO_UNSPECIFIED)
        } finally {
            file = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    private companion object {
        fun openLocalFile(uri: Uri): RandomAccessFile {
            val path = uri.path ?: throw FileDataSourceException(
                "uri has no path: $uri", null, DataSourceException.ERROR_CODE_IO_UNSPECIFIED,
            )
            return try {
                RandomAccessFile(path, "r")
            } catch (e: FileNotFoundException) {
                if (!uri.query.isNullOrEmpty() || !uri.fragment.isNullOrEmpty()) {
                    throw FileDataSourceException(
                        "uri has query and/or fragment, which are not supported. Did you call Uri.parse()" +
                            " on a string containing '?' or '#'? Use Uri.fromFile(new File(path)) to avoid this." +
                            " path=$path,query=${uri.query},fragment=${uri.fragment}",
                        e, DataSourceException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    )
                }
                throw FileDataSourceException(e, DataSourceException.ERROR_CODE_IO_FILE_NOT_FOUND)
            } catch (e: SecurityException) {
                throw FileDataSourceException(e, DataSourceException.ERROR_CODE_IO_NO_PERMISSION)
            } catch (e: RuntimeException) {
                throw FileDataSourceException(e, DataSourceException.ERROR_CODE_IO_UNSPECIFIED)
            }
        }
    }
}
