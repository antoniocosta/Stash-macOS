package androidx.work

import android.app.Notification
import android.content.Context
import java.util.UUID

/** Parameters handed to a worker; the runtime hooks let setProgress/setForeground reach WorkManager. */
class WorkerParameters internal constructor(
    val id: UUID,
    val inputData: Data,
    val tags: Set<String>,
    val runAttemptCount: Int,
    internal val onProgress: (Data) -> Unit,
    internal val onForeground: (ForegroundInfo) -> Unit,
)

/** Same shape as androidx.work.ForegroundInfo. On desktop the notification is surfaced via NotificationManager. */
class ForegroundInfo @JvmOverloads constructor(
    val notificationId: Int,
    val notification: Notification,
    val foregroundServiceType: Int = 0,
)

abstract class ListenableWorker(
    appContext: Context,
    private val workerParams: WorkerParameters,
) {
    val applicationContext: Context = appContext
    val id: UUID get() = workerParams.id
    val inputData: Data get() = workerParams.inputData
    val tags: Set<String> get() = workerParams.tags
    val runAttemptCount: Int get() = workerParams.runAttemptCount

    @Volatile
    internal var stopped = false
    val isStopped: Boolean get() = stopped

    /** Called by the runtime when the work is cancelled/stopped. */
    open fun onStopped() {}

    internal abstract suspend fun runWork(): Result

    abstract class Result internal constructor() {
        class Success @JvmOverloads constructor(val outputData: Data = Data.EMPTY) : Result() {
            override fun toString() = "Success {mOutputData=$outputData}"
        }
        class Failure @JvmOverloads constructor(val outputData: Data = Data.EMPTY) : Result() {
            override fun toString() = "Failure {mOutputData=$outputData}"
        }
        class Retry : Result() {
            override fun toString() = "Retry"
        }

        companion object {
            @JvmStatic fun success(): Result = Success()
            @JvmStatic fun success(outputData: Data): Result = Success(outputData)
            @JvmStatic fun failure(): Result = Failure()
            @JvmStatic fun failure(outputData: Data): Result = Failure(outputData)
            @JvmStatic fun retry(): Result = Retry()
        }
    }
}

abstract class CoroutineWorker(
    appContext: Context,
    private val params: WorkerParameters,
) : ListenableWorker(appContext, params) {

    abstract suspend fun doWork(): Result

    open suspend fun getForegroundInfo(): ForegroundInfo =
        throw IllegalStateException("Not implemented")

    suspend fun setProgress(data: Data) = params.onProgress(data)

    fun setProgressAsync(data: Data): com.google.common.util.concurrent.ListenableFuture<Void?> {
        params.onProgress(data)
        return com.google.common.util.concurrent.Futures.immediateFuture(null)
    }

    suspend fun setForeground(foregroundInfo: ForegroundInfo) = params.onForeground(foregroundInfo)

    internal override suspend fun runWork(): Result = doWork()
}

/** Blocking worker flavour, run on an IO thread. */
abstract class Worker(appContext: Context, params: WorkerParameters) : ListenableWorker(appContext, params) {
    abstract fun doWork(): Result
    internal override suspend fun runWork(): Result = doWork()
}

/** Creates workers; the desktop app installs a Dagger-backed one via WorkManager.initialize. */
abstract class WorkerFactory {
    abstract fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker?

    companion object {
        /** WorkManager's default: reflective (Context, WorkerParameters) constructor. */
        internal val DEFAULT = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                Class.forName(workerClassName)
                    .getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
                    .newInstance(appContext, workerParameters) as ListenableWorker
        }
    }
}

class Configuration private constructor(val workerFactory: WorkerFactory) {
    class Builder {
        private var factory: WorkerFactory = WorkerFactory.DEFAULT
        fun setWorkerFactory(workerFactory: WorkerFactory) = apply { factory = workerFactory }
        fun setMinimumLoggingLevel(loggingLevel: Int) = this
        fun build() = Configuration(factory)
    }

    interface Provider {
        val workManagerConfiguration: Configuration
    }
}

class WorkInfo(
    val id: UUID,
    val state: State,
    val tags: Set<String>,
    val outputData: Data = Data.EMPTY,
    val progress: Data = Data.EMPTY,
    val runAttemptCount: Int = 0,
    val generation: Int = 0,
    val constraints: Constraints = Constraints.NONE,
    val initialDelayMillis: Long = 0L,
    val periodicityInfo: PeriodicityInfo? = null,
    val nextScheduleTimeMillis: Long = Long.MAX_VALUE,
    val stopReason: Int = STOP_REASON_NOT_STOPPED,
) {
    enum class State {
        ENQUEUED, RUNNING, SUCCEEDED, FAILED, BLOCKED, CANCELLED;
        val isFinished: Boolean get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    }

    class PeriodicityInfo(val repeatIntervalMillis: Long, val flexIntervalMillis: Long)

    companion object {
        const val STOP_REASON_NOT_STOPPED = -256
        const val STOP_REASON_UNKNOWN = -512
        const val STOP_REASON_CANCELLED_BY_APP = 1
        const val STOP_REASON_PREEMPT = 2
        const val STOP_REASON_TIMEOUT = 3
        const val STOP_REASON_DEVICE_STATE = 4
        const val STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW = 5
        const val STOP_REASON_CONSTRAINT_CHARGING = 6
        const val STOP_REASON_CONSTRAINT_CONNECTIVITY = 7
        const val STOP_REASON_CONSTRAINT_DEVICE_IDLE = 8
        const val STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW = 9
        const val STOP_REASON_QUOTA = 10
        const val STOP_REASON_BACKGROUND_RESTRICTION = 11
        const val STOP_REASON_APP_STANDBY = 12
        const val STOP_REASON_USER = 13
        const val STOP_REASON_SYSTEM_PROCESSING = 14
        const val STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED = 15
    }

    override fun toString() = "WorkInfo{id='$id', state=$state, tags=$tags, outputData=$outputData, progress=$progress}"
}
