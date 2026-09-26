package androidx.media3.datasource.cache

import android.util.Log
import androidx.media3.datasource.LENGTH_UNSET_LONG
import androidx.media3.database.DatabaseProvider
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.util.NavigableSet
import java.util.Random
import java.util.TreeSet
import java.util.regex.Pattern

/**
 * Desktop port of media3 1.9.2 `SimpleCache`: a [Cache] implementation that maintains an
 * in-memory representation of the cached spans, backed by one file per span on disk.
 *
 * Same on-disk layout as media3 without a database: span files
 * `<subdir 0-9>/<contentId>.<position>.<lastTouchTimestamp>.v3.exo`, a `<uid-hex>.uid` marker
 * and the legacy `cached_content_index.exi` index (version 2, unencrypted) mapping content ids
 * to keys and [ContentMetadata]. The [DatabaseProvider] is accepted for signature parity only.
 * Same locking model: every operation is synchronized on the cache; [startReadWrite] blocks
 * on the cache monitor until an overlapping locked hole is released.
 */
class SimpleCache(
    cacheDir: File,
    private val evictor: CacheEvictor,
    @Suppress("UNUSED_PARAMETER") databaseProvider: DatabaseProvider?,
    @Suppress("UNUSED_PARAMETER") legacyIndexSecretKey: ByteArray?,
    @Suppress("UNUSED_PARAMETER") legacyIndexEncrypt: Boolean,
    @Suppress("UNUSED_PARAMETER") preferLegacyIndex: Boolean,
) : Cache {

    private val cacheDir: File = cacheDir
    private val listeners = HashMap<String, ArrayList<Cache.Listener>>()
    private val random = Random()
    private val touchCacheSpans = evictor.requiresCacheSpanTouches()

    private val contents = HashMap<String, CachedContent>()
    private val idToKey = java.util.TreeMap<Int, String>()
    private var indexChanged = false

    private var uid = Cache.UID_UNSET
    private var totalSpace = 0L
    private var released = false
    private var initializationException: Cache.CacheException? = null

    @Deprecated("Use a constructor that takes a DatabaseProvider for improved performance.")
    constructor(cacheDir: File, evictor: CacheEvictor) : this(cacheDir, evictor, null, null, false, true)

    constructor(cacheDir: File, evictor: CacheEvictor, databaseProvider: DatabaseProvider) :
        this(cacheDir, evictor, databaseProvider, null, false, false)

    init {
        if (!lockFolder(cacheDir)) {
            throw IllegalStateException("Another SimpleCache instance uses the folder: $cacheDir")
        }
        synchronized(this) { initialize() }
    }

    // ---------------------------------------------------------------------------------------------
    // Cache

    @Synchronized
    override fun getUid(): Long = uid

    @Synchronized
    override fun release() {
        if (released) return
        listeners.clear()
        removeStaleSpans()
        try {
            storeIndex(force = false)
        } catch (e: IOException) {
            Log.e(TAG, "Storing index file failed", e)
        } finally {
            unlockFolder(cacheDir)
            released = true
        }
    }

    @Synchronized
    override fun addListener(key: String, listener: Cache.Listener): NavigableSet<CacheSpan> {
        check(!released)
        listeners.getOrPut(key) { ArrayList() }.add(listener)
        return getCachedSpans(key)
    }

    @Synchronized
    override fun removeListener(key: String, listener: Cache.Listener) {
        if (released) return
        val list = listeners[key] ?: return
        list.remove(listener)
        if (list.isEmpty()) listeners.remove(key)
    }

    @Synchronized
    override fun getCachedSpans(key: String): NavigableSet<CacheSpan> {
        check(!released)
        val content = contents[key]
        return if (content == null || content.spans.isEmpty()) TreeSet() else TreeSet(content.spans)
    }

    @Synchronized
    override fun getKeys(): Set<String> {
        check(!released)
        return HashSet(contents.keys)
    }

    @Synchronized
    override fun getCacheSpace(): Long {
        check(!released)
        return totalSpace
    }

    @Synchronized
    @Throws(InterruptedException::class, Cache.CacheException::class)
    override fun startReadWrite(key: String, position: Long, length: Long): CacheSpan {
        check(!released)
        checkInitialization()
        while (true) {
            val span = startReadWriteNonBlocking(key, position, length)
            if (span != null) return span
            // Lock not available. Wait until it is (releaseHoleSpan/commitFile notify).
            (this as Object).wait()
        }
    }

    @Synchronized
    @Throws(Cache.CacheException::class)
    override fun startReadWriteNonBlocking(key: String, position: Long, length: Long): CacheSpan? {
        check(!released)
        checkInitialization()
        val span = getSpan(key, position, length)
        if (span.isCached) {
            // Read case.
            return touchSpan(key, span)
        }
        val content = getOrAddContent(key)
        return if (content.lockRange(position, span.length)) span else null
    }

    @Synchronized
    @Throws(Cache.CacheException::class)
    override fun startFile(key: String, position: Long, length: Long): File {
        check(!released)
        checkInitialization()
        val content = checkNotNull(contents[key])
        check(content.isFullyLocked(position, length))
        if (!cacheDir.exists()) {
            // The cache directory has been deleted from underneath us. Recreate it, and remove
            // in-memory spans corresponding to cache files that no longer exist.
            createCacheDirectories(cacheDir)
            removeStaleSpans()
        }
        evictor.onStartFile(this, key, position, length)
        val cacheSubDir = File(cacheDir, random.nextInt(SUBDIRECTORY_COUNT).toString())
        if (!cacheSubDir.exists()) createCacheDirectories(cacheSubDir)
        val lastTouchTimestamp = System.currentTimeMillis()
        return spanFile(cacheSubDir, content.id, position, lastTouchTimestamp)
    }

    @Synchronized
    @Throws(Cache.CacheException::class)
    override fun commitFile(file: File, length: Long) {
        check(!released)
        if (!file.exists()) return
        if (length == 0L) {
            file.delete()
            return
        }
        val span = checkNotNull(createCacheEntry(file, length))
        val content = checkNotNull(contents[span.key])
        check(content.isFullyLocked(span.position, span.length))
        val contentLength = ContentMetadata.getContentLength(content.metadata)
        if (contentLength != LENGTH_UNSET_LONG) {
            check(span.position + span.length <= contentLength)
        }
        addSpan(span)
        try {
            storeIndex(force = false)
        } catch (e: IOException) {
            throw Cache.CacheException(e)
        }
        (this as Object).notifyAll()
    }

    @Synchronized
    override fun releaseHoleSpan(holeSpan: CacheSpan) {
        check(!released)
        val content = checkNotNull(contents[holeSpan.key])
        content.unlockRange(holeSpan.position)
        maybeRemoveContent(content.key)
        (this as Object).notifyAll()
    }

    @Synchronized
    override fun removeResource(key: String) {
        check(!released)
        for (span in getCachedSpans(key)) removeSpanInternal(span)
    }

    @Synchronized
    override fun removeSpan(span: CacheSpan) {
        check(!released)
        removeSpanInternal(span)
    }

    @Synchronized
    override fun isCached(key: String, position: Long, length: Long): Boolean {
        check(!released)
        val content = contents[key] ?: return false
        return content.getCachedBytesLength(position, length) >= length
    }

    @Synchronized
    override fun getCachedLength(key: String, position: Long, length: Long): Long {
        check(!released)
        val len = if (length == LENGTH_UNSET_LONG) Long.MAX_VALUE else length
        val content = contents[key]
        return content?.getCachedBytesLength(position, len) ?: -len
    }

    @Synchronized
    override fun getCachedBytes(key: String, position: Long, length: Long): Long {
        var endPosition = if (length == LENGTH_UNSET_LONG) Long.MAX_VALUE else position + length
        if (endPosition < 0) endPosition = Long.MAX_VALUE
        var currentPosition = position
        var cachedBytes = 0L
        while (currentPosition < endPosition) {
            val maxRemainingLength = endPosition - currentPosition
            var blockLength = getCachedLength(key, currentPosition, maxRemainingLength)
            if (blockLength > 0) cachedBytes += blockLength else blockLength = -blockLength
            currentPosition += blockLength
        }
        return cachedBytes
    }

    @Synchronized
    @Throws(Cache.CacheException::class)
    override fun applyContentMetadataMutations(key: String, mutations: ContentMetadataMutations) {
        check(!released)
        checkInitialization()
        val content = getOrAddContent(key)
        val old = content.metadata
        content.metadata = old.copyWithMutationsApplied(mutations)
        if (content.metadata !== old) indexChanged = true
        try {
            storeIndex(force = false)
        } catch (e: IOException) {
            throw Cache.CacheException(e)
        }
    }

    @Synchronized
    override fun getContentMetadata(key: String): ContentMetadata {
        check(!released)
        return contents[key]?.metadata ?: DefaultContentMetadata.EMPTY
    }

    /** Checks whether the cache was initialized successfully. */
    @Synchronized
    @Throws(Cache.CacheException::class)
    fun checkInitialization() {
        initializationException?.let { throw it }
    }

    // ---------------------------------------------------------------------------------------------
    // Internals

    private fun initialize() {
        if (!cacheDir.exists()) {
            try {
                createCacheDirectories(cacheDir)
            } catch (e: Cache.CacheException) {
                initializationException = e
                return
            }
        }
        val files = cacheDir.listFiles()
        if (files == null) {
            val message = "Failed to list cache directory files: $cacheDir"
            Log.e(TAG, message)
            initializationException = Cache.CacheException(message)
            return
        }
        uid = loadUid(files)
        if (uid == Cache.UID_UNSET) {
            try {
                uid = createUid(cacheDir)
            } catch (e: IOException) {
                val message = "Failed to create cache UID: $cacheDir"
                Log.e(TAG, message, e)
                initializationException = Cache.CacheException(message, e)
                return
            }
        }
        loadIndex()
        loadDirectory(cacheDir, isRoot = true, files)
        // Remove contents that have neither spans nor locks, then persist.
        for (key in ArrayList(contents.keys)) maybeRemoveContent(key)
        try {
            storeIndex(force = false)
        } catch (e: IOException) {
            Log.e(TAG, "Storing index file failed", e)
            initializationException = Cache.CacheException(e)
            return
        }
        evictor.onCacheInitialized()
    }

    private fun loadDirectory(directory: File, isRoot: Boolean, files: Array<File>?) {
        if (files.isNullOrEmpty()) {
            // Either failed to list, or the directory is empty. Delete it if it's not the root.
            if (!isRoot) directory.delete()
            return
        }
        for (file in files) {
            val fileName = file.name
            if (isRoot && fileName.indexOf('.') == -1) {
                loadDirectory(file, isRoot = false, file.listFiles())
            } else {
                if (isRoot && (fileName.startsWith(INDEX_FILE_NAME) || fileName.endsWith(UID_FILE_SUFFIX))) {
                    // Skip the (expected) UID and index files in the root directory.
                    continue
                }
                val span = if (file.isFile) createCacheEntry(file, file.length()) else null
                if (span != null) addSpan(span) else file.delete()
            }
        }
    }

    /** Parses a span file name, returning null if it isn't a known span of a known content. */
    private fun createCacheEntry(file: File, length: Long): CacheSpan? {
        val matcher = CACHE_FILE_PATTERN_V3.matcher(file.name)
        if (!matcher.matches()) return null
        val id = matcher.group(1)!!.toIntOrNull() ?: return null
        val key = idToKey[id] ?: return null
        val len = if (length == LENGTH_UNSET_LONG) file.length() else length
        if (len == 0L) return null
        val position = matcher.group(2)!!.toLongOrNull() ?: return null
        val timestamp = matcher.group(3)!!.toLongOrNull() ?: return null
        return CacheSpan(key, position, len, timestamp, file)
    }

    private fun getSpan(key: String, position: Long, length: Long): CacheSpan {
        val content = contents[key] ?: return CacheSpan(key, position, length)
        while (true) {
            val span = content.getSpan(position, length)
            if (span.isCached && span.file!!.length() != span.length) {
                // The file has been modified or deleted underneath us. It's likely that other
                // files have been modified too, so scan the whole in-memory representation.
                removeStaleSpans()
                continue
            }
            return span
        }
    }

    private fun touchSpan(key: String, span: CacheSpan): CacheSpan {
        if (!touchCacheSpans) return span
        val file = span.file!!
        val lastTouchTimestamp = System.currentTimeMillis()
        val content = checkNotNull(contents[key])
        check(content.spans.remove(span))
        var newFile = file
        val candidate = spanFile(file.parentFile, content.id, span.position, lastTouchTimestamp)
        if (file.renameTo(candidate)) {
            newFile = candidate
        } else {
            Log.w(TAG, "Failed to rename $file to $candidate")
        }
        val newSpan = CacheSpan(span.key, span.position, span.length, lastTouchTimestamp, newFile)
        content.spans.add(newSpan)
        notifySpanTouched(span, newSpan)
        return newSpan
    }

    private fun addSpan(span: CacheSpan) {
        getOrAddContent(span.key).spans.add(span)
        totalSpace += span.length
        notifySpanAdded(span)
    }

    private fun removeSpanInternal(span: CacheSpan) {
        val content = contents[span.key] ?: return
        if (!content.spans.remove(span)) return
        span.file?.delete()
        totalSpace -= span.length
        maybeRemoveContent(content.key)
        notifySpanRemoved(span)
    }

    /** Scans all in-memory spans and removes any whose file was modified or deleted. */
    private fun removeStaleSpans() {
        val spansToBeRemoved = ArrayList<CacheSpan>()
        for (content in contents.values) {
            for (span in content.spans) {
                if (span.file!!.length() != span.length) spansToBeRemoved.add(span)
            }
        }
        for (span in spansToBeRemoved) removeSpanInternal(span)
    }

    private fun getOrAddContent(key: String): CachedContent {
        contents[key]?.let { return it }
        val id = newId()
        val content = CachedContent(id, key)
        contents[key] = content
        idToKey[id] = key
        indexChanged = true
        return content
    }

    private fun maybeRemoveContent(key: String) {
        val content = contents[key] ?: return
        if (content.spans.isEmpty() && content.lockedRanges.isEmpty()) {
            contents.remove(key)
            idToKey.remove(content.id)
            indexChanged = true
        }
    }

    private fun newId(): Int {
        if (idToKey.isEmpty()) return 0
        val next = idToKey.lastKey() + 1
        if (next >= 0) return next
        // Int overflow: find the smallest unused id.
        var id = 0
        while (idToKey.containsKey(id)) id++
        return id
    }

    private fun notifySpanRemoved(span: CacheSpan) {
        listeners[span.key]?.let { list -> for (i in list.indices.reversed()) list[i].onSpanRemoved(this, span) }
        evictor.onSpanRemoved(this, span)
    }

    private fun notifySpanAdded(span: CacheSpan) {
        listeners[span.key]?.let { list -> for (i in list.indices.reversed()) list[i].onSpanAdded(this, span) }
        evictor.onSpanAdded(this, span)
    }

    private fun notifySpanTouched(oldSpan: CacheSpan, newSpan: CacheSpan) {
        listeners[oldSpan.key]?.let { list -> for (i in list.indices.reversed()) list[i].onSpanTouched(this, oldSpan, newSpan) }
        evictor.onSpanTouched(this, oldSpan, newSpan)
    }

    // --- Index persistence: media3 CachedContentIndex.LegacyStorage format (version 2). ---------

    private val indexFile get() = File(cacheDir, INDEX_FILE_NAME)

    private fun loadIndex() {
        contents.clear()
        idToKey.clear()
        val file = indexFile
        if (!file.exists()) return
        try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                val version = input.readInt()
                if (version < 0 || version > INDEX_VERSION) error("unsupported index version $version")
                val flags = input.readInt()
                if (flags and FLAG_ENCRYPTED_INDEX != 0) error("encrypted index unsupported")
                val count = input.readInt()
                var hashCode = 0
                repeat(count) {
                    val id = input.readInt()
                    val key = input.readUTF()
                    val metadata = if (version < 2) {
                        val length = input.readLong()
                        val m = ContentMetadataMutations()
                        ContentMetadataMutations.setContentLength(m, length)
                        DefaultContentMetadata.EMPTY.copyWithMutationsApplied(m)
                    } else {
                        readContentMetadata(input)
                    }
                    val content = CachedContent(id, key).also { it.metadata = metadata }
                    contents[key] = content
                    idToKey[id] = key
                    hashCode += content.headerHashCode(version)
                }
                val fileHashCode = input.readInt()
                val isEOF = input.read() == -1
                if (fileHashCode != hashCode || !isEOF) error("index checksum mismatch")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cache content index file.", e)
            contents.clear()
            idToKey.clear()
            file.delete()
        }
    }

    private fun storeIndex(force: Boolean) {
        if (!indexChanged && !force && indexFile.exists()) return
        val tmp = File(cacheDir, "$INDEX_FILE_NAME.new")
        DataOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { output ->
            output.writeInt(INDEX_VERSION)
            output.writeInt(0) // flags: unencrypted
            output.writeInt(contents.size)
            var hashCode = 0
            for (content in contents.values) {
                output.writeInt(content.id)
                output.writeUTF(content.key)
                writeContentMetadata(content.metadata, output)
                hashCode += content.headerHashCode(INDEX_VERSION)
            }
            output.writeInt(hashCode)
            output.flush()
        }
        try {
            Files.move(tmp.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        indexChanged = false
    }

    private fun readContentMetadata(input: DataInputStream): DefaultContentMetadata {
        val size = input.readInt()
        val metadata = HashMap<String, ByteArray>()
        repeat(size) {
            val name = input.readUTF()
            val valueSize = input.readInt()
            if (valueSize < 0 || valueSize > MAX_METADATA_BUFFER) throw IOException("Invalid value size: $valueSize")
            val value = ByteArray(valueSize)
            input.readFully(value)
            metadata[name] = value
        }
        return DefaultContentMetadata(metadata)
    }

    private fun writeContentMetadata(metadata: DefaultContentMetadata, output: DataOutputStream) {
        val entries = metadata.entrySet()
        output.writeInt(entries.size)
        for ((name, value) in entries) {
            output.writeUTF(name)
            output.writeInt(value.size)
            output.write(value)
        }
    }

    /** In-memory state of one resource (media3 `CachedContent`). */
    private class CachedContent(val id: Int, val key: String) {
        val spans = TreeSet<CacheSpan>()
        val lockedRanges = ArrayList<Range>()
        var metadata: DefaultContentMetadata = DefaultContentMetadata.EMPTY

        fun getSpan(position: Long, length: Long): CacheSpan {
            val lookupSpan = CacheSpan(key, position, LENGTH_UNSET_LONG)
            val floorSpan = spans.floor(lookupSpan)
            if (floorSpan != null && floorSpan.position + floorSpan.length > position) return floorSpan
            val ceilSpan = spans.ceiling(lookupSpan)
            var len = length
            if (ceilSpan != null) {
                val holeLength = ceilSpan.position - position
                len = if (len == LENGTH_UNSET_LONG) holeLength else minOf(holeLength, len)
            }
            return CacheSpan(key, position, len)
        }

        fun getCachedBytesLength(position: Long, length: Long): Long {
            require(position >= 0)
            require(length >= 0)
            val span = getSpan(position, length)
            if (span.isHoleSpan()) {
                // We don't have a span covering the start of the queried region.
                return -minOf(if (span.isOpenEnded()) Long.MAX_VALUE else span.length, length)
            }
            var queryEndPosition = position + length
            if (queryEndPosition < 0) queryEndPosition = Long.MAX_VALUE
            var currentEndPosition = span.position + span.length
            if (currentEndPosition < queryEndPosition) {
                for (next in spans.tailSet(span, false)) {
                    if (next.position > currentEndPosition) break
                    // We expect currentEndPosition to always equal (next.position + next.length),
                    // but we don't make any assumptions about the spans being non-overlapping.
                    currentEndPosition = maxOf(currentEndPosition, next.position + next.length)
                    if (currentEndPosition >= queryEndPosition) break
                }
            }
            return minOf(currentEndPosition - position, length)
        }

        fun isFullyLocked(position: Long, length: Long): Boolean =
            lockedRanges.any { it.contains(position, length) }

        fun lockRange(position: Long, length: Long): Boolean {
            for (range in lockedRanges) if (range.intersects(position, length)) return false
            lockedRanges.add(Range(position, length))
            return true
        }

        fun unlockRange(position: Long) {
            val index = lockedRanges.indexOfFirst { it.position == position }
            if (index == -1) throw IllegalStateException()
            lockedRanges.removeAt(index)
        }

        fun headerHashCode(version: Int): Int {
            var result = id
            result = 31 * result + key.hashCode()
            if (version < 2) {
                val length = ContentMetadata.getContentLength(metadata)
                result = 31 * result + (length xor (length ushr 32)).toInt()
            } else {
                result = 31 * result + metadata.hashCode()
            }
            return result
        }
    }

    private class Range(val position: Long, val length: Long) {
        fun contains(otherPosition: Long, otherLength: Long): Boolean =
            if (length == LENGTH_UNSET_LONG) {
                otherPosition >= position
            } else if (otherLength == LENGTH_UNSET_LONG) {
                false
            } else {
                position <= otherPosition && otherPosition + otherLength <= position + length
            }

        fun intersects(otherPosition: Long, otherLength: Long): Boolean =
            if (position <= otherPosition) {
                length == LENGTH_UNSET_LONG || position + length > otherPosition
            } else {
                otherLength == LENGTH_UNSET_LONG || otherPosition + otherLength > position
            }
    }

    companion object {
        private const val TAG = "SimpleCache"
        private const val SUBDIRECTORY_COUNT = 10
        private const val UID_FILE_SUFFIX = ".uid"
        private const val INDEX_FILE_NAME = "cached_content_index.exi"
        private const val INDEX_VERSION = 2
        private const val FLAG_ENCRYPTED_INDEX = 1
        private const val MAX_METADATA_BUFFER = 10 * 1024 * 1024
        private val CACHE_FILE_PATTERN_V3: Pattern =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.v3\\.exo$", Pattern.DOTALL)

        private val lockedCacheDirs = HashSet<File>()

        /** Returns whether [cacheFolder] is locked by a [SimpleCache] instance. */
        @JvmStatic
        @Synchronized
        fun isCacheFolderLocked(cacheFolder: File): Boolean = lockedCacheDirs.contains(cacheFolder.absoluteFile)

        /**
         * Deletes all content belonging to a cache instance. Must not be called while a
         * [SimpleCache] instance uses the folder.
         */
        @JvmStatic
        fun delete(cacheDir: File, @Suppress("UNUSED_PARAMETER") databaseProvider: DatabaseProvider?) {
            if (!cacheDir.exists()) return
            check(!isCacheFolderLocked(cacheDir)) { "Cache folder is locked: $cacheDir" }
            cacheDir.deleteRecursively()
        }

        @Synchronized
        private fun lockFolder(cacheDir: File): Boolean = lockedCacheDirs.add(cacheDir.absoluteFile)

        @Synchronized
        private fun unlockFolder(cacheDir: File) {
            lockedCacheDirs.remove(cacheDir.absoluteFile)
        }

        private fun spanFile(dir: File, id: Int, position: Long, timestamp: Long): File =
            File(dir, "$id.$position.$timestamp.v3.exo")

        private fun loadUid(files: Array<File>): Long {
            for (file in files) {
                val fileName = file.name
                if (fileName.endsWith(UID_FILE_SUFFIX)) {
                    try {
                        return fileName.substring(0, fileName.indexOf('.')).toLong(16)
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Malformed UID file: $file")
                        file.delete()
                    }
                }
            }
            return Cache.UID_UNSET
        }

        private fun createUid(directory: File): Long {
            // Ensure the UID is non-negative so that it's always a valid Long when parsed.
            val uid = SecureRandom().nextLong().let { if (it == Long.MIN_VALUE) 0L else Math.abs(it) }
            val file = File(directory, java.lang.Long.toString(uid, 16) + UID_FILE_SUFFIX)
            if (!file.createNewFile()) throw IOException("Failed to create UID file: $file")
            return uid
        }

        private fun createCacheDirectories(cacheDir: File) {
            if (!cacheDir.mkdirs() && !cacheDir.isDirectory) {
                val message = "Failed to create cache directory: $cacheDir"
                Log.e(TAG, message)
                throw Cache.CacheException(message)
            }
        }
    }
}
