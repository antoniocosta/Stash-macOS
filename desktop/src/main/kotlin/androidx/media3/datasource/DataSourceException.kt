package androidx.media3.datasource

import java.io.IOException

/**
 * Desktop port of media3 1.9.2 `DataSourceException`. [reason] is a
 * `PlaybackException.ErrorCode` (values inlined to keep this independent of the common module).
 */
open class DataSourceException : IOException {

    @JvmField val reason: Int

    constructor(reason: Int) : super() { this.reason = reason }
    constructor(cause: Throwable?, reason: Int) : super(cause) { this.reason = reason }
    constructor(message: String?, reason: Int) : super(message) { this.reason = reason }
    constructor(message: String?, cause: Throwable?, reason: Int) : super(message, cause) { this.reason = reason }

    companion object {
        /** PlaybackException.ERROR_CODE_IO_UNSPECIFIED */
        internal const val ERROR_CODE_IO_UNSPECIFIED = 2000
        /** PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED */
        internal const val ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 2001
        /** PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT */
        internal const val ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 2002
        /** PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE */
        internal const val ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE = 2003
        /** PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS */
        internal const val ERROR_CODE_IO_BAD_HTTP_STATUS = 2004
        /** PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND */
        internal const val ERROR_CODE_IO_FILE_NOT_FOUND = 2005
        /** PlaybackException.ERROR_CODE_IO_NO_PERMISSION */
        internal const val ERROR_CODE_IO_NO_PERMISSION = 2006
        /** PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED */
        internal const val ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED = 2007
        /** PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE */
        internal const val ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 2008

        /** True if [e] or one of its causes is a position-out-of-range [DataSourceException]. */
        @JvmStatic
        fun isCausedByPositionOutOfRange(e: IOException): Boolean {
            var cause: Throwable? = e
            while (cause != null) {
                if (cause is DataSourceException && cause.reason == ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE) return true
                cause = cause.cause
            }
            return false
        }
    }
}
