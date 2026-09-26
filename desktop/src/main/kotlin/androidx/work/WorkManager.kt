package androidx.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.Properties
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Desktop androidx.work.WorkManager: same public API, backed by an in-process
 * coroutine scheduler (no JobScheduler on macOS). Implements unique-work
 * policies, chains with overwriting input merge, retries with backoff, periodic
 * work (last-run times persisted so restarts don't re-fire every job), and
 * observable WorkInfo. Constraints are treated as satisfied (a running Mac is
 * online and powered for our purposes).
 */
abstract class WorkManager internal constructor() {

    abstract fun enqueue(request: WorkRequest): Operation
    abstract fun enqueue(requests: List<WorkRequest>): Operation
    abstract fun enqueueUniqueWork(
        uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, request: OneTimeWorkRequest,
    ): Operation
    abstract fun enqueueUniqueWork(
        uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, requests: List<OneTimeWorkRequest>,
    ): Operation
    abstract fun enqueueUniquePeriodicWork(
        uniqueWorkName: String, existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy, request: PeriodicWorkRequest,
    ): Operation
    abstract fun beginWith(request: OneTimeWorkRequest): WorkContinuation
    abstract fun beginWith(requests: List<OneTimeWorkRequest>): WorkContinuation
    abstract fun beginUniqueWork(
        uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, request: OneTimeWorkRequest,
    ): WorkContinuation
    abstract fun beginUniqueWork(
        uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, requests: List<OneTimeWorkRequest>,
    ): WorkContinuation

    abstract fun cancelWorkById(id: UUID): Operation
    abstract fun cancelAllWorkByTag(tag: String): Operation
    abstract fun cancelUniqueWork(uniqueWorkName: String): Operation
    abstract fun cancelAllWork(): Operation
    abstract fun pruneWork(): Operation

    abstract fun getWorkInfoById(id: UUID): ListenableFuture<WorkInfo?>
    abstract fun getWorkInfoByIdFlow(id: UUID): Flow<WorkInfo?>
    abstract fun getWorkInfosByTag(tag: String): ListenableFuture<List<WorkInfo>>
    abstract fun getWorkInfosByTagFlow(tag: String): Flow<List<WorkInfo>>
    abstract fun getWorkInfosForUniqueWork(uniqueWorkName: String): ListenableFuture<List<WorkInfo>>
    abstract fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>>

    abstract fun createCancelPendingIntent(id: UUID): PendingIntent

    companion object {
        const val ACTION_CANCEL_WORK = "androidx.work.desktop.CANCEL_WORK"
        const val EXTRA_WORK_ID = "androidx.work.desktop.WORK_ID"

        @Volatile private var instance: DesktopWorkManager? = null

        @JvmStatic
        fun initialize(context: Context, configuration: Configuration) = synchronized(this) {
            check(instance == null) { "WorkManager is already initialized" }
            instance = DesktopWorkManager(context.applicationContext, configuration)
        }

        @JvmStatic fun isInitialized(): Boolean = instance != null

        @JvmStatic
        fun getInstance(context: Context): WorkManager = instance ?: synchronized(this) {
            instance ?: DesktopWorkManager(context.applicationContext, Configuration.Builder().build())
                .also { instance = it }
        }
    }
}

interface Operation {
    val result: ListenableFuture<State.SUCCESS>

    sealed class State {
        object SUCCESS : State()
        object IN_PROGRESS : State()
        class FAILURE(val throwable: Throwable) : State()
    }
}

abstract class WorkContinuation internal constructor() {
    abstract fun then(work: OneTimeWorkRequest): WorkContinuation
    abstract fun then(work: List<OneTimeWorkRequest>): WorkContinuation
    abstract fun enqueue(): Operation
}

// ---------------------------------------------------------------------------
// Runtime
// ---------------------------------------------------------------------------

internal class DoneFuture<T>(value: T) : ListenableFuture<T> {
    private val f = CompletableFuture.completedFuture(value)
    override fun addListener(listener: Runnable, executor: Executor) = executor.execute(listener)
    override fun cancel(mayInterruptIfRunning: Boolean) = false
    override fun isCancelled() = false
    override fun isDone() = true
    override fun get(): T = f.get()
    override fun get(timeout: Long, unit: TimeUnit): T = f.get(timeout, unit)
}

private object DoneOperation : Operation {
    override val result: ListenableFuture<Operation.State.SUCCESS> = DoneFuture(Operation.State.SUCCESS)
}

internal class DesktopWorkManager(
    private val context: Context,
    private val configuration: Configuration,
) : WorkManager() {

    private class Rec(
        @Volatile var request: WorkRequest,
        val uniqueName: String?,
        val prerequisites: List<UUID>,
    ) {
        @Volatile var state = WorkInfo.State.ENQUEUED
        @Volatile var output: Data = Data.EMPTY
        @Volatile var progress: Data = Data.EMPTY
        @Volatile var attempt = 0
        @Volatile var job: Job? = null
        @Volatile var worker: ListenableWorker? = null
        val id: UUID get() = request.id
        fun info() = WorkInfo(id, state, request.tags, output, progress, attempt)
    }

    private val lock = Any()
    private val recs = LinkedHashMap<UUID, Rec>()
    private val version = MutableStateFlow(0L)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val periodicFile = File(context.noBackupFilesDir, "workmanager-periodic.properties")
    private val lastRun = Properties().apply { if (periodicFile.exists()) periodicFile.inputStream().use { load(it) } }

    private fun changed() { version.value = version.value + 1 }

    // --- enqueue ------------------------------------------------------------

    override fun enqueue(request: WorkRequest) = enqueue(listOf(request))

    override fun enqueue(requests: List<WorkRequest>): Operation {
        synchronized(lock) { requests.forEach { insert(it, null, emptyList()) } }
        requests.forEach { trySchedule(it.id) }
        changed()
        return DoneOperation
    }

    override fun enqueueUniqueWork(uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, request: OneTimeWorkRequest) =
        enqueueUniqueWork(uniqueWorkName, existingWorkPolicy, listOf(request))

    override fun enqueueUniqueWork(uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, requests: List<OneTimeWorkRequest>) =
        Continuation(uniqueWorkName, existingWorkPolicy, listOf(requests)).enqueue()

    override fun enqueueUniquePeriodicWork(
        uniqueWorkName: String, existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy, request: PeriodicWorkRequest,
    ): Operation {
        synchronized(lock) {
            val existing = recs.values.filter { it.uniqueName == uniqueWorkName }
            val live = existing.firstOrNull { !it.state.isFinished }
            when {
                live != null && existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.KEEP -> return DoneOperation
                live != null && existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.UPDATE -> {
                    // Keep id and timing; the new spec applies from the next run.
                    live.request = rebind(request, live.id)
                    changed()
                    return DoneOperation
                }
                else -> {
                    existing.forEach { cancel(it) }
                    existing.forEach { recs.remove(it.id) }
                    insert(request, uniqueWorkName, emptyList())
                }
            }
        }
        trySchedule(request.id)
        changed()
        return DoneOperation
    }

    /** Same work, different id (UPDATE keeps the enqueued id, like WorkManager). */
    private fun rebind(r: PeriodicWorkRequest, id: UUID): PeriodicWorkRequest =
        PeriodicWorkRequest.Builder(Class.forName(r.workerClassName).asSubclass(ListenableWorker::class.java),
            r.intervalMillis, TimeUnit.MILLISECONDS)
            .setId(id).setConstraints(r.constraints).setInputData(r.inputData)
            .setBackoffCriteria(r.backoffPolicy, r.backoffDelayMillis, TimeUnit.MILLISECONDS)
            .also { b -> r.tags.forEach { b.addTag(it) } }
            .build()

    override fun beginWith(request: OneTimeWorkRequest): WorkContinuation = beginWith(listOf(request))
    override fun beginWith(requests: List<OneTimeWorkRequest>): WorkContinuation = Continuation(null, null, listOf(requests))
    override fun beginUniqueWork(uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, request: OneTimeWorkRequest) =
        beginUniqueWork(uniqueWorkName, existingWorkPolicy, listOf(request))
    override fun beginUniqueWork(uniqueWorkName: String, existingWorkPolicy: ExistingWorkPolicy, requests: List<OneTimeWorkRequest>): WorkContinuation =
        Continuation(uniqueWorkName, existingWorkPolicy, listOf(requests))

    private inner class Continuation(
        val name: String?,
        val policy: ExistingWorkPolicy?,
        val levels: List<List<OneTimeWorkRequest>>,
    ) : WorkContinuation() {
        override fun then(work: OneTimeWorkRequest) = then(listOf(work))
        override fun then(work: List<OneTimeWorkRequest>): WorkContinuation = Continuation(name, policy, levels + listOf(work))

        override fun enqueue(): Operation {
            val inserted = ArrayList<UUID>()
            synchronized(lock) {
                var parents: List<UUID> = emptyList()
                if (name != null) {
                    val existing = recs.values.filter { it.uniqueName == name }
                    val unfinished = existing.filter { !it.state.isFinished }
                    val leaves = existing.filter { e -> existing.none { e.id in it.prerequisites } }
                    val leafFailed = leaves.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }
                    when (policy!!) {
                        ExistingWorkPolicy.KEEP -> if (unfinished.isNotEmpty()) return DoneOperation else drop(existing)
                        ExistingWorkPolicy.REPLACE -> { existing.forEach { cancel(it) }; drop(existing) }
                        ExistingWorkPolicy.APPEND ->
                            if (unfinished.isNotEmpty() || leafFailed) parents = leaves.map { it.id } else drop(existing)
                        ExistingWorkPolicy.APPEND_OR_REPLACE ->
                            if (unfinished.isNotEmpty() && !leafFailed) parents = leaves.map { it.id }
                            else { existing.forEach { cancel(it) }; drop(existing) }
                    }
                }
                for (level in levels) {
                    level.forEach { insert(it, name, parents); inserted += it.id }
                    parents = level.map { it.id }
                }
            }
            inserted.forEach { trySchedule(it) }
            changed()
            return DoneOperation
        }
    }

    private fun drop(existing: List<Rec>) = existing.forEach { recs.remove(it.id) }

    private fun insert(request: WorkRequest, uniqueName: String?, prerequisites: List<UUID>) {
        recs[request.id] = Rec(request, uniqueName, prerequisites).apply {
            if (prerequisites.isNotEmpty()) state = WorkInfo.State.BLOCKED
        }
    }

    // --- execution ----------------------------------------------------------

    private fun trySchedule(id: UUID) {
        val rec: Rec
        val input: Data
        synchronized(lock) {
            rec = recs[id] ?: return
            if (rec.state.isFinished || rec.job != null) return
            val prereqs = rec.prerequisites.mapNotNull { recs[it] }
            if (prereqs.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }) {
                val s = if (prereqs.any { it.state == WorkInfo.State.CANCELLED }) WorkInfo.State.CANCELLED else WorkInfo.State.FAILED
                finish(rec, s, Data.EMPTY)
                return
            }
            if (prereqs.any { it.state != WorkInfo.State.SUCCEEDED }) return
            // OverwritingInputMerger: own input, then each prerequisite's output overwrites.
            input = Data.Builder().putAll(rec.request.inputData).apply { prereqs.forEach { putAll(it.output) } }.build()
            rec.state = WorkInfo.State.ENQUEUED
            rec.job = scope.launch { runLoop(rec, input) }
        }
        changed()
    }

    private suspend fun runLoop(rec: Rec, input: Data) {
        var delayMs = rec.request.initialDelayMillis
        if (rec.request is PeriodicWorkRequest && rec.uniqueName != null) {
            val last = lastRun.getProperty(rec.uniqueName)?.toLongOrNull()
            if (last != null) {
                val interval = (rec.request as PeriodicWorkRequest).intervalMillis
                delayMs = maxOf(delayMs, last + interval - System.currentTimeMillis())
            }
        }
        while (true) {
            if (delayMs > 0) delay(delayMs)
            val req = rec.request
            val result = runOnce(rec, req, if (req is PeriodicWorkRequest) req.inputData else input) ?: return
            when {
                req is PeriodicWorkRequest -> {
                    if (result is ListenableWorker.Result.Retry) {
                        rec.attempt++
                        delayMs = backoff(req, rec.attempt)
                    } else {
                        rec.attempt = 0
                        rec.output = Data.EMPTY
                        rec.uniqueName?.let { persistLastRun(it) }
                        delayMs = req.intervalMillis
                    }
                    rec.state = WorkInfo.State.ENQUEUED
                    changed()
                }
                result is ListenableWorker.Result.Retry -> {
                    rec.attempt++
                    rec.state = WorkInfo.State.ENQUEUED
                    changed()
                    delayMs = backoff(req, rec.attempt)
                }
                result is ListenableWorker.Result.Success -> { finish(rec, WorkInfo.State.SUCCEEDED, result.outputData); return }
                result is ListenableWorker.Result.Failure -> { finish(rec, WorkInfo.State.FAILED, result.outputData); return }
                else -> { finish(rec, WorkInfo.State.FAILED, Data.EMPTY); return }
            }
        }
    }

    private fun backoff(req: WorkRequest, attempt: Int): Long = when (req.backoffPolicy) {
        BackoffPolicy.LINEAR -> req.backoffDelayMillis * attempt
        BackoffPolicy.EXPONENTIAL -> req.backoffDelayMillis * (1L shl (attempt - 1).coerceIn(0, 30))
    }.coerceAtMost(WorkRequest.MAX_BACKOFF_MILLIS)

    /** Returns null when the work was cancelled. */
    private suspend fun runOnce(rec: Rec, req: WorkRequest, input: Data): ListenableWorker.Result? {
        val params = WorkerParameters(
            id = rec.id, inputData = input, tags = req.tags, runAttemptCount = rec.attempt,
            onProgress = { rec.progress = it; changed() },
            onForeground = { info -> Log.i(TAG, "foreground ${req.workerClassName}: ${info.notification}") },
        )
        val worker = try {
            configuration.workerFactory.createWorker(context, req.workerClassName, params)
                ?: WorkerFactory.DEFAULT.createWorker(context, req.workerClassName, params)
                ?: error("No worker for ${req.workerClassName}")
        } catch (e: Throwable) {
            Log.e(TAG, "Could not instantiate ${req.workerClassName}", e)
            return ListenableWorker.Result.failure()
        }
        rec.worker = worker
        rec.state = WorkInfo.State.RUNNING
        rec.progress = Data.EMPTY
        changed()
        return try {
            if (worker is Worker) kotlinx.coroutines.withContext(Dispatchers.IO) { worker.runWork() } else worker.runWork()
        } catch (e: CancellationException) {
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Work ${req.workerClassName} threw", e)
            ListenableWorker.Result.failure()
        } finally {
            rec.worker = null
        }
    }

    private fun finish(rec: Rec, state: WorkInfo.State, output: Data) {
        val dependents: List<UUID>
        synchronized(lock) {
            rec.state = state
            rec.output = output
            rec.job = null
            dependents = recs.values.filter { rec.id in it.prerequisites }.map { it.id }
        }
        changed()
        dependents.forEach { trySchedule(it) }
    }

    private fun persistLastRun(name: String) = synchronized(lastRun) {
        lastRun.setProperty(name, System.currentTimeMillis().toString())
        periodicFile.parentFile?.mkdirs()
        periodicFile.outputStream().use { lastRun.store(it, "WorkManager periodic last-run times") }
    }

    // --- cancel -------------------------------------------------------------

    private fun cancel(rec: Rec) {
        if (rec.state.isFinished) return
        rec.worker?.let { it.stopped = true; runCatching { it.onStopped() } }
        rec.job?.cancel()
        rec.job = null
        rec.state = WorkInfo.State.CANCELLED
        recs.values.filter { rec.id in it.prerequisites }.forEach { cancel(it) }
    }

    private fun cancelWhere(pred: (Rec) -> Boolean): Operation {
        synchronized(lock) { recs.values.filter(pred).forEach { cancel(it) } }
        changed()
        return DoneOperation
    }

    override fun cancelWorkById(id: UUID) = cancelWhere { it.id == id }
    override fun cancelAllWorkByTag(tag: String) = cancelWhere { tag in it.request.tags }
    override fun cancelUniqueWork(uniqueWorkName: String) = cancelWhere { it.uniqueName == uniqueWorkName }
    override fun cancelAllWork() = cancelWhere { true }
    override fun pruneWork(): Operation {
        synchronized(lock) { recs.values.removeAll { it.state.isFinished } }
        changed()
        return DoneOperation
    }

    // --- observe ------------------------------------------------------------

    private fun infos(pred: (Rec) -> Boolean): List<WorkInfo> = synchronized(lock) { recs.values.filter(pred).map { it.info() } }

    override fun getWorkInfoById(id: UUID): ListenableFuture<WorkInfo?> = DoneFuture(infos { it.id == id }.firstOrNull())
    override fun getWorkInfoByIdFlow(id: UUID): Flow<WorkInfo?> =
        version.map { infos { it.id == id }.firstOrNull() }.distinctUntilChanged()
    override fun getWorkInfosByTag(tag: String): ListenableFuture<List<WorkInfo>> = DoneFuture(infos { tag in it.request.tags })
    override fun getWorkInfosByTagFlow(tag: String): Flow<List<WorkInfo>> =
        version.map { infos { tag in it.request.tags } }.distinctUntilChanged()
    override fun getWorkInfosForUniqueWork(uniqueWorkName: String): ListenableFuture<List<WorkInfo>> =
        DoneFuture(infos { it.uniqueName == uniqueWorkName })
    override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> =
        version.map { infos { it.uniqueName == uniqueWorkName } }.distinctUntilChanged()

    override fun createCancelPendingIntent(id: UUID): PendingIntent =
        PendingIntent.getBroadcast(
            context, id.hashCode(),
            Intent(ACTION_CANCEL_WORK).putExtra(EXTRA_WORK_ID, id.toString()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val TAG = "WorkManager"
    }
}
