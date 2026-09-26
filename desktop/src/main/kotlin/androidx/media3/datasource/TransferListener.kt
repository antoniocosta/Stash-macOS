package androidx.media3.datasource

/** Desktop port of media3 1.9.2 `TransferListener`: observes data transfers of a [DataSource]. */
interface TransferListener {
    fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean)
    fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean)
    fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int)
    fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean)
}
