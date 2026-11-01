package android.net

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

/**
 * Desktop shim for android.net.ConnectivityManager. The active network is the first up,
 * non-loopback, non-virtual interface with a routable address (java.net.NetworkInterface);
 * it reports INTERNET/VALIDATED/NOT_METERED, transport WIFI or ETHERNET per macOS
 * `networksetup -listallhardwareports`, plus VPN when a utun/ppp/ipsec tunnel carries an address.
 */
class ConnectivityManager private constructor() {

    val activeNetwork: Network?
        get() = interfaces().firstOrNull { !isTunnel(it.name) && hasRoutableAddress(it) }?.let { Network(it.name) }

    fun getNetworkCapabilities(network: Network?): NetworkCapabilities? {
        network ?: return null
        val nic = runCatching { NetworkInterface.getByName(network.interfaceName) }.getOrNull() ?: return null
        if (runCatching { !nic.isUp }.getOrDefault(true) || !hasRoutableAddress(nic)) return null
        val transports = mutableSetOf(
            if (wifiDevices.contains(nic.name)) NetworkCapabilities.TRANSPORT_WIFI else NetworkCapabilities.TRANSPORT_ETHERNET,
        )
        if (interfaces().any { isTunnel(it.name) && hasRoutableAddress(it) }) transports += NetworkCapabilities.TRANSPORT_VPN
        val caps = mutableSetOf(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
        )
        return NetworkCapabilities(transports, caps)
    }

    val isActiveNetworkMetered: Boolean get() = false

    /** Android's NetworkCallback; the desktop reports the current default network once on registration. */
    open class NetworkCallback {
        open fun onAvailable(network: Network) {}
        open fun onLost(network: Network) {}
        open fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {}
        open fun onUnavailable() {}
    }

    private val callbacks = java.util.concurrent.CopyOnWriteArrayList<NetworkCallback>()

    fun registerDefaultNetworkCallback(networkCallback: NetworkCallback) {
        callbacks.addIfAbsent(networkCallback)
        activeNetwork?.let { n ->
            networkCallback.onAvailable(n)
            getNetworkCapabilities(n)?.let { networkCallback.onCapabilitiesChanged(n, it) }
        }
    }

    fun registerDefaultNetworkCallback(networkCallback: NetworkCallback, handler: android.os.Handler?) =
        registerDefaultNetworkCallback(networkCallback)

    fun unregisterNetworkCallback(networkCallback: NetworkCallback) {
        callbacks.remove(networkCallback)
    }

    /** macOS has no per-app Data Saver. */
    val restrictBackgroundStatus: Int get() = RESTRICT_BACKGROUND_STATUS_DISABLED

    private fun interfaces(): List<NetworkInterface> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback && !isVirtual(it.name) }
            .sortedByDescending { it.name.startsWith("en") }
    }.getOrDefault(emptyList())

    private fun hasRoutableAddress(nic: NetworkInterface) = nic.inetAddresses.toList().any {
        (it is Inet4Address && !it.isLinkLocalAddress) || (it is Inet6Address && !it.isLinkLocalAddress && !it.isLoopbackAddress)
    }

    private fun isTunnel(name: String) = name.startsWith("utun") || name.startsWith("ppp") || name.startsWith("ipsec")
    private fun isVirtual(name: String) = VIRTUAL_PREFIXES.any { name.startsWith(it) }

    /** BSD device names macOS labels as Wi-Fi hardware ports (e.g. en0 on laptops). */
    private val wifiDevices: Set<String> by lazy {
        runCatching {
            val p = ProcessBuilder("networksetup", "-listallhardwareports").redirectErrorStream(true).start()
            val lines = p.inputStream.bufferedReader().readLines().also { p.waitFor() }
            lines.zipWithNext().filter { (a, _) -> a.contains("Wi-Fi") || a.contains("AirPort") }
                .mapNotNull { (_, b) -> b.substringAfter("Device:", "").trim().takeIf(String::isNotEmpty) }.toSet()
        }.getOrDefault(emptySet())
    }

    companion object {
        const val RESTRICT_BACKGROUND_STATUS_DISABLED = 1
        const val RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2
        const val RESTRICT_BACKGROUND_STATUS_ENABLED = 3
        private val VIRTUAL_PREFIXES = listOf("awdl", "llw", "bridge", "anpi", "ap", "gif", "stf", "vmenet")
        internal val INSTANCE = ConnectivityManager()
    }
}
