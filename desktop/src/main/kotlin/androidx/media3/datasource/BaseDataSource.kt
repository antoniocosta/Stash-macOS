package androidx.media3.datasource

/**
 * Desktop port of media3 1.9.2 `BaseDataSource`: [TransferListener] bookkeeping for
 * [DataSource] implementations.
 */
abstract class BaseDataSource protected constructor(private val isNetwork: Boolean) : DataSource {

    private val listeners = ArrayList<TransferListener>(1)
    private var listenerCount = 0
    private var dataSpec: DataSpec? = null

    final override fun addTransferListener(transferListener: TransferListener) {
        if (!listeners.contains(transferListener)) {
            listeners.add(transferListener)
            listenerCount++
        }
    }

    protected fun transferInitializing(dataSpec: DataSpec) {
        for (i in 0 until listenerCount) listeners[i].onTransferInitializing(this, dataSpec, isNetwork)
    }

    protected fun transferStarted(dataSpec: DataSpec) {
        this.dataSpec = dataSpec
        for (i in 0 until listenerCount) listeners[i].onTransferStart(this, dataSpec, isNetwork)
    }

    protected fun bytesTransferred(bytesTransferred: Int) {
        val spec = dataSpec ?: return
        for (i in 0 until listenerCount) listeners[i].onBytesTransferred(this, spec, isNetwork, bytesTransferred)
    }

    protected fun transferEnded() {
        val spec = dataSpec ?: return
        for (i in 0 until listenerCount) listeners[i].onTransferEnd(this, spec, isNetwork)
        dataSpec = null
    }
}
