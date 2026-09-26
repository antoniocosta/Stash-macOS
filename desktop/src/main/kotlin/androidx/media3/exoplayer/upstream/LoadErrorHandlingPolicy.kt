package androidx.media3.exoplayer.upstream

interface LoadErrorHandlingPolicy {
    fun getMinimumLoadableRetryCount(dataType: Int): Int = 3
}

open class DefaultLoadErrorHandlingPolicy(
    private val minimumLoadableRetryCount: Int = DEFAULT_MINIMUM_LOADABLE_RETRY_COUNT,
) : LoadErrorHandlingPolicy {
    override fun getMinimumLoadableRetryCount(dataType: Int): Int = minimumLoadableRetryCount

    companion object {
        const val DEFAULT_MINIMUM_LOADABLE_RETRY_COUNT = 3
    }
}
