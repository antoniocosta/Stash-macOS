package androidx.media3.datasource.cache

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.LENGTH_UNSET_LONG
import java.nio.ByteBuffer
import java.util.Collections

/** Desktop port of media3 1.9.2 `ContentMetadata`: metadata associated with a cache file. */
interface ContentMetadata {

    fun get(key: String, defaultValue: ByteArray?): ByteArray?

    fun get(key: String, defaultValue: String?): String?

    fun get(key: String, defaultValue: Long): Long

    fun contains(key: String): Boolean

    companion object {
        /** Prefix for custom metadata keys. */
        const val KEY_CUSTOM_PREFIX = "custom_"
        /** Key for redirected uri (type: String). */
        const val KEY_REDIRECTED_URI = "exo_redir"
        /** Key for content length in bytes (type: long). */
        const val KEY_CONTENT_LENGTH = "exo_len"

        /** The content length, or [C.LENGTH_UNSET] if not set. */
        @JvmStatic
        fun getContentLength(contentMetadata: ContentMetadata): Long =
            contentMetadata.get(KEY_CONTENT_LENGTH, LENGTH_UNSET_LONG)

        /** The redirected uri, or null if not set. */
        @JvmStatic
        fun getRedirectedUri(contentMetadata: ContentMetadata): Uri? =
            contentMetadata.get(KEY_REDIRECTED_URI, null as String?)?.let { Uri.parse(it) }
    }
}

/** Desktop port of media3 1.9.2 `DefaultContentMetadata`: immutable [ContentMetadata]. */
class DefaultContentMetadata(metadata: Map<String, ByteArray>) : ContentMetadata {

    private val metadata: Map<String, ByteArray> =
        Collections.unmodifiableMap(HashMap<String, ByteArray>().also { m ->
            for ((k, v) in metadata) m[k] = v.copyOf()
        })

    constructor() : this(emptyMap())

    /** Returns a copy with [mutations] applied, or this instance if nothing changes. */
    fun copyWithMutationsApplied(mutations: ContentMetadataMutations): DefaultContentMetadata {
        val mutated = applyMutations(metadata, mutations)
        return if (isMetadataEqual(metadata, mutated)) this else DefaultContentMetadata(mutated)
    }

    /** Returns the set of metadata entries (defensive copies of the values). */
    fun entrySet(): Set<Map.Entry<String, ByteArray>> = metadata.entries

    override fun get(key: String, defaultValue: ByteArray?): ByteArray? =
        metadata[key]?.copyOf() ?: defaultValue

    override fun get(key: String, defaultValue: String?): String? =
        metadata[key]?.let { String(it, Charsets.UTF_8) } ?: defaultValue

    override fun get(key: String, defaultValue: Long): Long =
        metadata[key]?.let { ByteBuffer.wrap(it).long } ?: defaultValue

    override fun contains(key: String): Boolean = metadata.containsKey(key)

    override fun equals(other: Any?): Boolean =
        this === other || (other is DefaultContentMetadata && isMetadataEqual(metadata, other.metadata))

    override fun hashCode(): Int {
        var result = 0
        for ((k, v) in metadata) result += k.hashCode() xor v.contentHashCode()
        return result
    }

    companion object {
        @JvmField val EMPTY = DefaultContentMetadata(emptyMap())

        private fun isMetadataEqual(first: Map<String, ByteArray>, second: Map<String, ByteArray>): Boolean {
            if (first.size != second.size) return false
            for ((k, v) in first) if (!v.contentEquals(second[k])) return false
            return true
        }

        private fun applyMutations(
            otherMetadata: Map<String, ByteArray>,
            mutations: ContentMetadataMutations,
        ): Map<String, ByteArray> {
            val metadata = HashMap(otherMetadata)
            for (key in mutations.getRemovedValues()) metadata.remove(key)
            for ((k, v) in mutations.getEditedValues()) metadata[k] = getBytes(v)
            return metadata
        }

        private fun getBytes(value: Any): ByteArray = when (value) {
            is Long -> ByteBuffer.allocate(8).putLong(value).array()
            is String -> value.toByteArray(Charsets.UTF_8)
            is ByteArray -> value
            else -> throw IllegalArgumentException()
        }
    }
}

/**
 * Desktop port of media3 1.9.2 `ContentMetadataMutations`: a set of edits and removals to apply
 * to a [ContentMetadata].
 */
class ContentMetadataMutations {

    private val editedValues = HashMap<String, Any>()
    private val removedValues = ArrayList<String>()

    fun set(name: String, value: String): ContentMetadataMutations = checkAndSet(name, value)

    fun set(name: String, value: Long): ContentMetadataMutations = checkAndSet(name, value)

    fun set(name: String, value: ByteArray): ContentMetadataMutations = checkAndSet(name, value.copyOf())

    fun remove(name: String): ContentMetadataMutations {
        removedValues.add(name)
        editedValues.remove(name)
        return this
    }

    fun getRemovedValues(): List<String> = Collections.unmodifiableList(ArrayList(removedValues))

    fun getEditedValues(): Map<String, Any> {
        val hashMap = HashMap(editedValues)
        for ((k, v) in hashMap) if (v is ByteArray) hashMap[k] = v.copyOf()
        return Collections.unmodifiableMap(hashMap)
    }

    private fun checkAndSet(name: String, value: Any): ContentMetadataMutations {
        editedValues[name] = value
        removedValues.remove(name)
        return this
    }

    companion object {
        /** Adds a mutation to set the [ContentMetadata.KEY_CONTENT_LENGTH] value, or to remove any existing value if [C.LENGTH_UNSET] is passed. */
        @JvmStatic
        fun setContentLength(mutations: ContentMetadataMutations, length: Long): ContentMetadataMutations =
            mutations.set(ContentMetadata.KEY_CONTENT_LENGTH, length)

        /** Adds a mutation to set the [ContentMetadata.KEY_REDIRECTED_URI] value, or to remove any existing entry if null is passed. */
        @JvmStatic
        fun setRedirectedUri(mutations: ContentMetadataMutations, uri: Uri?): ContentMetadataMutations =
            if (uri == null) mutations.remove(ContentMetadata.KEY_REDIRECTED_URI)
            else mutations.set(ContentMetadata.KEY_REDIRECTED_URI, uri.toString())
    }
}
