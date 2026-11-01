package androidx.media3.exoplayer.upstream

interface LoadErrorHandlingPolicy {
    /** media3's LoadErrorInfo: the failed load's error and how many times it has failed in a row. */
    class LoadErrorInfo(@JvmField val exception: java.io.IOException, @JvmField val errorCount: Int)

    fun getMinimumLoadableRetryCount(dataType: Int): Int = 3

    /** media3's rule: no retry for FileNotFound / cleartext / invalid-response-code errors, else min(1s * (n-1), 5s). */
    fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
        val e = loadErrorInfo.exception
        return if (e is java.io.FileNotFoundException || e is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
            androidx.media3.common.C.TIME_UNSET
        } else {
            minOf((loadErrorInfo.errorCount - 1) * 1000L, 5000L)
        }
    }
}

open class DefaultLoadErrorHandlingPolicy(
    private val minimumLoadableRetryCount: Int = DEFAULT_MINIMUM_LOADABLE_RETRY_COUNT,
) : LoadErrorHandlingPolicy {
    override fun getMinimumLoadableRetryCount(dataType: Int): Int = minimumLoadableRetryCount

    companion object {
        const val DEFAULT_MINIMUM_LOADABLE_RETRY_COUNT = 3
    }
}
