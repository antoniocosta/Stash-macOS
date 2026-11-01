package android.content

/** Desktop shim for android.content.IntentSender: what a PendingIntent hands to another component. */
class IntentSender internal constructor(internal val send: (Intent?) -> Unit) {
    class SendIntentException(message: String? = null) : Exception(message)
}

/** Desktop shim for android.content.ActivityNotFoundException. */
class ActivityNotFoundException(message: String? = null) : RuntimeException(message)

/** Desktop shim for android.content.ContentValues. */
class ContentValues() {
    private val values = LinkedHashMap<String, Any?>()

    fun put(key: String, value: String?) { values[key] = value }
    fun put(key: String, value: Int?) { values[key] = value }
    fun put(key: String, value: Long?) { values[key] = value }
    fun put(key: String, value: Boolean?) { values[key] = value }
    fun put(key: String, value: Double?) { values[key] = value }
    fun put(key: String, value: ByteArray?) { values[key] = value }
    fun putNull(key: String) { values[key] = null }
    fun containsKey(key: String): Boolean = values.containsKey(key)
    fun get(key: String): Any? = values[key]
    fun getAsString(key: String): String? = values[key]?.toString()
    fun getAsInteger(key: String): Int? = (values[key] as? Number)?.toInt()
    fun getAsLong(key: String): Long? = (values[key] as? Number)?.toLong()
    fun keySet(): Set<String> = values.keys
    fun size(): Int = values.size
}
