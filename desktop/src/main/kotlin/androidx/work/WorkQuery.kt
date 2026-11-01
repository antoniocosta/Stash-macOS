package androidx.work

import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import kotlinx.coroutines.guava.await as guavaAwait

/** Desktop port of WorkManager's WorkQuery: ids / tags / unique names / states, each an OR within, AND across. */
class WorkQuery private constructor(
    val ids: List<UUID>,
    val uniqueWorkNames: List<String>,
    val tags: List<String>,
    val states: List<WorkInfo.State>,
) {
    class Builder private constructor() {
        private val ids = ArrayList<UUID>()
        private val names = ArrayList<String>()
        private val tags = ArrayList<String>()
        private val states = ArrayList<WorkInfo.State>()

        fun addIds(ids: List<UUID>) = apply { this.ids += ids }
        fun addUniqueWorkNames(uniqueWorkNames: List<String>) = apply { names += uniqueWorkNames }
        fun addTags(tags: List<String>) = apply { this.tags += tags }
        fun addStates(states: List<WorkInfo.State>) = apply { this.states += states }
        fun build() = WorkQuery(ids.toList(), names.toList(), tags.toList(), states.toList())

        companion object {
            @JvmStatic fun fromIds(ids: List<UUID>) = Builder().addIds(ids)
            @JvmStatic fun fromUniqueWorkNames(names: List<String>) = Builder().addUniqueWorkNames(names)
            @JvmStatic fun fromTags(tags: List<String>) = Builder().addTags(tags)
            @JvmStatic fun fromStates(states: List<WorkInfo.State>) = Builder().addStates(states)
        }
    }

    companion object {
        @JvmStatic fun fromIds(vararg ids: UUID) = Builder.fromIds(ids.toList()).build()
        @JvmStatic fun fromIds(ids: List<UUID>) = Builder.fromIds(ids).build()
        @JvmStatic fun fromUniqueWorkNames(vararg names: String) = Builder.fromUniqueWorkNames(names.toList()).build()
        @JvmStatic fun fromUniqueWorkNames(names: List<String>) = Builder.fromUniqueWorkNames(names).build()
        @JvmStatic fun fromTags(vararg tags: String) = Builder.fromTags(tags.toList()).build()
        @JvmStatic fun fromTags(tags: List<String>) = Builder.fromTags(tags).build()
        @JvmStatic fun fromStates(vararg states: WorkInfo.State) = Builder.fromStates(states.toList()).build()
        @JvmStatic fun fromStates(states: List<WorkInfo.State>) = Builder.fromStates(states).build()
    }
}

/** work-runtime's `ListenableFuture.await()` (androidx.work package). */
suspend fun <R> ListenableFuture<R>.await(): R = guavaAwait()

/** work-runtime's `Operation.await()`: suspends until the operation finished. */
suspend fun Operation.await(): Operation.State.SUCCESS = result.guavaAwait()
