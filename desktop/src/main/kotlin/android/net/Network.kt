package android.net

/** Desktop shim for android.net.Network: identifies the macOS interface (e.g. "en0") backing it. */
class Network internal constructor(internal val interfaceName: String) {
    override fun equals(other: Any?) = other is Network && other.interfaceName == interfaceName
    override fun hashCode() = interfaceName.hashCode()
    override fun toString() = interfaceName
}
