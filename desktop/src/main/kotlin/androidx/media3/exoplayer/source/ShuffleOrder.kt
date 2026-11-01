package androidx.media3.exoplayer.source

import androidx.media3.common.C
import kotlin.random.Random

/** Desktop port of media3's ShuffleOrder and its DefaultShuffleOrder (an explicit permutation). */
interface ShuffleOrder {
    fun getLength(): Int
    fun getNextIndex(index: Int): Int
    fun getPreviousIndex(index: Int): Int
    fun getLastIndex(): Int
    fun getFirstIndex(): Int
    fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder
    fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder
    fun cloneAndClear(): ShuffleOrder

    class DefaultShuffleOrder private constructor(private val shuffled: IntArray, private val random: Random) : ShuffleOrder {
        private val indexInShuffled = IntArray(shuffled.size).also { inv -> shuffled.forEachIndexed { i, v -> inv[v] = i } }

        constructor(length: Int) : this(length, System.nanoTime())
        constructor(length: Int, randomSeed: Long) : this(createShuffled(length, Random(randomSeed)), Random(randomSeed))
        constructor(shuffledIndices: IntArray, randomSeed: Long) : this(shuffledIndices.copyOf(), Random(randomSeed))

        override fun getLength(): Int = shuffled.size
        override fun getNextIndex(index: Int): Int = indexInShuffled[index].let { if (it + 1 < shuffled.size) shuffled[it + 1] else C.INDEX_UNSET }
        override fun getPreviousIndex(index: Int): Int = indexInShuffled[index].let { if (it - 1 >= 0) shuffled[it - 1] else C.INDEX_UNSET }
        override fun getLastIndex(): Int = shuffled.lastOrNull() ?: C.INDEX_UNSET
        override fun getFirstIndex(): Int = shuffled.firstOrNull() ?: C.INDEX_UNSET

        override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
            val inserted = IntArray(insertionCount) { insertionIndex + it }
            val positions = IntArray(insertionCount) { random.nextInt(shuffled.size + 1) }.sortedArray()
            val out = ArrayList<Int>(shuffled.size + insertionCount)
            var p = 0
            for (i in 0..shuffled.size) {
                while (p < insertionCount && positions[p] == i) out.add(inserted[p++])
                if (i < shuffled.size) out.add(shuffled[i].let { if (it >= insertionIndex) it + insertionCount else it })
            }
            return DefaultShuffleOrder(out.toIntArray(), random)
        }

        override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
            val n = indexToExclusive - indexFrom
            val out = shuffled.filter { it < indexFrom || it >= indexToExclusive }.map { if (it >= indexToExclusive) it - n else it }
            return DefaultShuffleOrder(out.toIntArray(), random)
        }

        override fun cloneAndClear(): ShuffleOrder = DefaultShuffleOrder(IntArray(0), random)

        private companion object {
            fun createShuffled(length: Int, random: Random): IntArray {
                val a = IntArray(length) { it }
                for (i in length - 1 downTo 1) {
                    val j = random.nextInt(i + 1)
                    val t = a[i]; a[i] = a[j]; a[j] = t
                }
                return a
            }
        }
    }

    class UnshuffledShuffleOrder(private val length: Int) : ShuffleOrder {
        override fun getLength(): Int = length
        override fun getNextIndex(index: Int): Int = if (index + 1 < length) index + 1 else C.INDEX_UNSET
        override fun getPreviousIndex(index: Int): Int = if (index - 1 >= 0) index - 1 else C.INDEX_UNSET
        override fun getLastIndex(): Int = if (length > 0) length - 1 else C.INDEX_UNSET
        override fun getFirstIndex(): Int = if (length > 0) 0 else C.INDEX_UNSET
        override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder = UnshuffledShuffleOrder(length + insertionCount)
        override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder = UnshuffledShuffleOrder(length - (indexToExclusive - indexFrom))
        override fun cloneAndClear(): ShuffleOrder = UnshuffledShuffleOrder(0)
    }
}
