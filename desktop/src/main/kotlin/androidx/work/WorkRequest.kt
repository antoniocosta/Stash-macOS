package androidx.work

import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

/*
 * Desktop androidx.work request model: same builders, enums and defaults as
 * WorkManager 2.10, executed by the in-process runtime in WorkManager.kt.
 */

enum class NetworkType { NOT_REQUIRED, CONNECTED, UNMETERED, NOT_ROAMING, METERED, TEMPORARILY_UNMETERED }
enum class BackoffPolicy { EXPONENTIAL, LINEAR }
enum class OutOfQuotaPolicy { RUN_AS_NON_EXPEDITED_WORK_REQUEST, DROP_WORK_REQUEST }
enum class ExistingWorkPolicy { REPLACE, KEEP, APPEND, APPEND_OR_REPLACE }
enum class ExistingPeriodicWorkPolicy { REPLACE, KEEP, UPDATE, CANCEL_AND_REENQUEUE }

class Constraints private constructor(
    val requiredNetworkType: NetworkType,
    private val batteryNotLow: Boolean,
    private val charging: Boolean,
    private val storageNotLow: Boolean,
    private val deviceIdle: Boolean,
) {
    fun requiresBatteryNotLow() = batteryNotLow
    fun requiresCharging() = charging
    fun requiresStorageNotLow() = storageNotLow
    fun requiresDeviceIdle() = deviceIdle

    class Builder {
        private var network = NetworkType.NOT_REQUIRED
        private var batteryNotLow = false
        private var charging = false
        private var storageNotLow = false
        private var deviceIdle = false
        fun setRequiredNetworkType(networkType: NetworkType) = apply { network = networkType }
        fun setRequiresBatteryNotLow(requiresBatteryNotLow: Boolean) = apply { batteryNotLow = requiresBatteryNotLow }
        fun setRequiresCharging(requiresCharging: Boolean) = apply { charging = requiresCharging }
        fun setRequiresStorageNotLow(requiresStorageNotLow: Boolean) = apply { storageNotLow = requiresStorageNotLow }
        fun setRequiresDeviceIdle(requiresDeviceIdle: Boolean) = apply { deviceIdle = requiresDeviceIdle }
        fun build() = Constraints(network, batteryNotLow, charging, storageNotLow, deviceIdle)
    }

    companion object {
        @JvmField val NONE: Constraints = Builder().build()
    }
}

abstract class WorkRequest internal constructor(
    val id: UUID,
    internal val workerClassName: String,
    internal val constraints: Constraints,
    internal val inputData: Data,
    val tags: Set<String>,
    internal val initialDelayMillis: Long,
    internal val backoffPolicy: BackoffPolicy,
    internal val backoffDelayMillis: Long,
) {
    val stringId: String get() = id.toString()

    abstract class Builder<B : Builder<B, W>, W : WorkRequest> internal constructor(workerClass: Class<out ListenableWorker>) {
        internal var id: UUID = UUID.randomUUID()
        internal val workerClassName: String = workerClass.name
        internal var constraints: Constraints = Constraints.NONE
        internal var inputData: Data = Data.EMPTY
        internal val tags = linkedSetOf(workerClass.name)
        internal var initialDelayMillis = 0L
        internal var backoffPolicy = BackoffPolicy.EXPONENTIAL
        internal var backoffDelayMillis = DEFAULT_BACKOFF_DELAY_MILLIS

        @Suppress("UNCHECKED_CAST")
        private fun self(): B = this as B

        fun setId(id: UUID): B = self().also { this.id = id }
        fun setConstraints(constraints: Constraints): B = self().also { this.constraints = constraints }
        fun setInputData(inputData: Data): B = self().also { this.inputData = inputData }
        fun addTag(tag: String): B = self().also { tags += tag }
        fun setInitialDelay(duration: Long, timeUnit: TimeUnit): B = self().also { initialDelayMillis = timeUnit.toMillis(duration) }
        fun setInitialDelay(duration: Duration): B = self().also { initialDelayMillis = duration.toMillis() }
        fun setBackoffCriteria(backoffPolicy: BackoffPolicy, backoffDelay: Long, timeUnit: TimeUnit): B = self().also {
            this.backoffPolicy = backoffPolicy
            backoffDelayMillis = timeUnit.toMillis(backoffDelay).coerceIn(MIN_BACKOFF_MILLIS, MAX_BACKOFF_MILLIS)
        }
        fun setBackoffCriteria(backoffPolicy: BackoffPolicy, duration: Duration): B =
            setBackoffCriteria(backoffPolicy, duration.toMillis(), TimeUnit.MILLISECONDS)
        /** Expedited work has no quota on desktop; it simply runs. */
        open fun setExpedited(policy: OutOfQuotaPolicy): B = self()
        fun keepResultsForAtLeast(duration: Long, timeUnit: TimeUnit): B = self()

        abstract fun build(): W
    }

    companion object {
        const val DEFAULT_BACKOFF_DELAY_MILLIS = 30_000L
        const val MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000L
        const val MIN_BACKOFF_MILLIS = 10_000L
    }
}

class OneTimeWorkRequest internal constructor(b: Builder) : WorkRequest(
    b.id, b.workerClassName, b.constraints, b.inputData, b.tags.toSet(),
    b.initialDelayMillis, b.backoffPolicy, b.backoffDelayMillis,
) {
    class Builder(workerClass: Class<out ListenableWorker>) : WorkRequest.Builder<Builder, OneTimeWorkRequest>(workerClass) {
        override fun build() = OneTimeWorkRequest(this)
    }

    companion object {
        @JvmStatic fun from(workerClass: Class<out ListenableWorker>) = Builder(workerClass).build()
    }
}

class PeriodicWorkRequest internal constructor(b: Builder) : WorkRequest(
    b.id, b.workerClassName, b.constraints, b.inputData, b.tags.toSet(),
    b.initialDelayMillis, b.backoffPolicy, b.backoffDelayMillis,
) {
    internal val intervalMillis: Long = b.intervalMillis

    class Builder private constructor(workerClass: Class<out ListenableWorker>, internal val intervalMillis: Long) :
        WorkRequest.Builder<Builder, PeriodicWorkRequest>(workerClass) {
        constructor(workerClass: Class<out ListenableWorker>, repeatInterval: Long, repeatIntervalTimeUnit: TimeUnit) :
            this(workerClass, repeatIntervalTimeUnit.toMillis(repeatInterval).coerceAtLeast(MIN_PERIODIC_INTERVAL_MILLIS))
        constructor(workerClass: Class<out ListenableWorker>, repeatInterval: Duration) :
            this(workerClass, repeatInterval.toMillis().coerceAtLeast(MIN_PERIODIC_INTERVAL_MILLIS))
        constructor(
            workerClass: Class<out ListenableWorker>,
            repeatInterval: Long, repeatIntervalTimeUnit: TimeUnit,
            flexInterval: Long, flexIntervalTimeUnit: TimeUnit,
        ) : this(workerClass, repeatInterval, repeatIntervalTimeUnit)
        constructor(workerClass: Class<out ListenableWorker>, repeatInterval: Duration, flexInterval: Duration) :
            this(workerClass, repeatInterval)

        override fun build() = PeriodicWorkRequest(this)
    }

    companion object {
        const val MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L
        const val MIN_PERIODIC_FLEX_MILLIS = 5 * 60 * 1000L
    }
}

inline fun <reified W : ListenableWorker> OneTimeWorkRequestBuilder(): OneTimeWorkRequest.Builder =
    OneTimeWorkRequest.Builder(W::class.java)

inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Long, repeatIntervalTimeUnit: TimeUnit,
): PeriodicWorkRequest.Builder = PeriodicWorkRequest.Builder(W::class.java, repeatInterval, repeatIntervalTimeUnit)

inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(repeatInterval: Duration): PeriodicWorkRequest.Builder =
    PeriodicWorkRequest.Builder(W::class.java, repeatInterval)

inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Long, repeatIntervalTimeUnit: TimeUnit,
    flexTimeInterval: Long, flexTimeIntervalUnit: TimeUnit,
): PeriodicWorkRequest.Builder =
    PeriodicWorkRequest.Builder(W::class.java, repeatInterval, repeatIntervalTimeUnit, flexTimeInterval, flexTimeIntervalUnit)

inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(repeatInterval: Duration, flexTimeInterval: Duration) =
    PeriodicWorkRequest.Builder(W::class.java, repeatInterval, flexTimeInterval)
