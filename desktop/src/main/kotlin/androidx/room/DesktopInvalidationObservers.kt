package androidx.room

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** Runs [InvalidationTracker.Observer]s off [InvalidationTracker.createFlow] in the database's own scope. */
internal object DesktopInvalidationObservers {
    private val jobs = ConcurrentHashMap<InvalidationTracker.Observer, Job>()

    fun add(database: RoomDatabase, observer: InvalidationTracker.Observer, flow: Flow<Set<String>>) {
        if (jobs.containsKey(observer)) return
        jobs[observer] = database.getCoroutineScope().launch {
            flow.collect { observer.onInvalidated(it) }
        }
    }

    fun remove(observer: InvalidationTracker.Observer) {
        jobs.remove(observer)?.cancel()
    }
}
