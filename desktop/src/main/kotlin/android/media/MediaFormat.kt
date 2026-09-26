package android.media

/** Desktop shim for android.media.MediaFormat: the same typed key/value map with Android's key names. */
class MediaFormat {
    private val map = HashMap<String, Any>()

    fun containsKey(name: String): Boolean = map.containsKey(name)

    /** Like Android: NullPointerException when absent, ClassCastException when not an Integer. */
    fun getInteger(name: String): Int = (map[name] ?: throw NullPointerException("No key $name")) as Int
    fun getLong(name: String): Long = (map[name] ?: throw NullPointerException("No key $name")) as Long
    fun getString(name: String): String? = map[name] as String?

    fun setInteger(name: String, value: Int) { map[name] = value }
    fun setLong(name: String, value: Long) { map[name] = value }
    fun setString(name: String, value: String?) { if (value == null) map.remove(name) else map[name] = value }

    override fun toString(): String = map.toString()

    companion object {
        const val KEY_MIME = "mime"
        const val KEY_SAMPLE_RATE = "sample-rate"
        const val KEY_CHANNEL_COUNT = "channel-count"
        const val KEY_BIT_RATE = "bitrate"
        const val KEY_DURATION = "durationUs"
    }
}
