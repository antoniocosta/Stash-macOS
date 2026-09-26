package android.content

import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Persistent [SharedPreferences] backed by a `java.util.Properties` XML file
 * (`shared_prefs/<name>.xml`, like Android). Values keep their type via a one-letter
 * prefix; `apply()` persists on a background thread, `commit()` synchronously.
 * One instance per file per process, as on Android.
 */
internal class DesktopSharedPreferences private constructor(private val file: File) : SharedPreferences {

    private val lock = Any()
    private val values: MutableMap<String, Any> = load()
    private val listeners = ConcurrentHashMap.newKeySet<SharedPreferences.OnSharedPreferenceChangeListener>()

    override val all: Map<String, *> get() = synchronized(lock) { HashMap(values) }

    private inline fun <reified T> read(key: String, def: T): T =
        synchronized(lock) { (values[key] ?: return def) as T }

    override fun getString(key: String, defValue: String?): String? = read(key, defValue)
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        synchronized(lock) { (values[key] as Set<String>?)?.toSet() ?: defValues }
    override fun getInt(key: String, defValue: Int): Int = read(key, defValue)
    override fun getLong(key: String, defValue: Long): Long = read(key, defValue)
    override fun getFloat(key: String, defValue: Float): Float = read(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean): Boolean = read(key, defValue)
    override fun contains(key: String): Boolean = synchronized(lock) { key in values }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners -= listener
    }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>() // null value == remove
        private var clear = false

        private fun put(key: String, value: Any?): SharedPreferences.Editor {
            synchronized(this) { pending[key] = value }
            return this
        }
        override fun putString(key: String, value: String?) = put(key, value)
        override fun putStringSet(key: String, values: Set<String>?) = put(key, values?.toSet())
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun putBoolean(key: String, value: Boolean) = put(key, value)
        override fun remove(key: String) = put(key, null)
        override fun clear(): SharedPreferences.Editor {
            synchronized(this) { clear = true }
            return this
        }

        /** Applies the edit in memory; returns the changed keys and the snapshot to persist. */
        private fun commitToMemory(): Pair<List<String?>, Properties> {
            val (edits, wasCleared) = synchronized(this) {
                LinkedHashMap(pending).also { pending.clear() } to clear.also { clear = false }
            }
            return synchronized(lock) {
                val changed = mutableListOf<String?>()
                if (wasCleared && values.isNotEmpty()) { values.clear(); changed += null }
                for ((k, v) in edits) {
                    val old = values[k]
                    if (v == null) values.remove(k) else values[k] = v
                    if (old != v) changed += k
                }
                changed to snapshot()
            }
        }

        override fun commit(): Boolean {
            val (changed, snapshot) = commitToMemory()
            val ok = write(snapshot)
            dispatch(changed)
            return ok
        }

        override fun apply() {
            val (changed, snapshot) = commitToMemory()
            writer.execute { write(snapshot) }
            dispatch(changed)
        }
    }

    private fun dispatch(keys: List<String?>) {
        if (listeners.isEmpty()) return
        keys.forEach { k -> listeners.forEach { it.onSharedPreferenceChanged(this, k) } }
    }

    private fun snapshot() = Properties().also { p -> values.forEach { (k, v) -> p.setProperty(k, encode(v)) } }

    private fun write(props: Properties): Boolean = synchronized(file) {
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.outputStream().use { props.storeToXML(it, null) }
            if (!tmp.renameTo(file)) { tmp.copyTo(file, overwrite = true); tmp.delete() }
        }.onFailure { android.util.Log.e("SharedPreferences", "write failed: $file", it) }.isSuccess
    }

    private fun load(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        if (!file.isFile) return map
        runCatching {
            val props = Properties().apply { file.inputStream().use { loadFromXML(it) } }
            props.stringPropertyNames().forEach { k -> decode(props.getProperty(k))?.let { map[k] = it } }
        }.onFailure { android.util.Log.e("SharedPreferences", "read failed: $file", it) }
        return map
    }

    companion object {
        private val cache = ConcurrentHashMap<String, DesktopSharedPreferences>()
        private val writer = Executors.newSingleThreadExecutor { r ->
            Thread(r, "SharedPreferences-writer").apply { isDaemon = true }
        }.also { ex ->
            Runtime.getRuntime().addShutdownHook(Thread {
                ex.shutdown()
                ex.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
            })
        }

        fun open(file: File): SharedPreferences =
            cache.computeIfAbsent(file.absolutePath) { DesktopSharedPreferences(file) }

        private fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8)
        private fun dec(s: String) = URLDecoder.decode(s, Charsets.UTF_8)

        private fun encode(v: Any): String = when (v) {
            is String -> "s:$v"
            is Int -> "i:$v"
            is Long -> "l:$v"
            is Float -> "f:$v"
            is Boolean -> "b:$v"
            is Set<*> -> "S:" + v.joinToString(",") { enc(it as String) }
            else -> error("unsupported preference type ${v.javaClass}")
        }

        private fun decode(raw: String): Any? {
            if (raw.length < 2 || raw[1] != ':') return null
            val body = raw.substring(2)
            return when (raw[0]) {
                's' -> body
                'i' -> body.toIntOrNull()
                'l' -> body.toLongOrNull()
                'f' -> body.toFloatOrNull()
                'b' -> body.toBooleanStrictOrNull()
                'S' -> if (body.isEmpty()) emptySet() else body.split(',').map(::dec).toSet()
                else -> null
            }
        }
    }
}
