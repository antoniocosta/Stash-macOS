package android.net

/** Desktop shim for android.net.NetworkCapabilities: an immutable snapshot computed by [ConnectivityManager]. */
class NetworkCapabilities internal constructor(
    private val transports: Set<Int>,
    private val capabilities: Set<Int>,
) {
    fun hasTransport(transportType: Int): Boolean = transportType in transports
    fun hasCapability(capability: Int): Boolean = capability in capabilities

    companion object {
        const val TRANSPORT_CELLULAR = 0
        const val TRANSPORT_WIFI = 1
        const val TRANSPORT_BLUETOOTH = 2
        const val TRANSPORT_ETHERNET = 3
        const val TRANSPORT_VPN = 4

        const val NET_CAPABILITY_NOT_METERED = 11
        const val NET_CAPABILITY_INTERNET = 12
        const val NET_CAPABILITY_NOT_VPN = 15
        const val NET_CAPABILITY_VALIDATED = 16
    }
}
