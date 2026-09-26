package androidx.hilt.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Desktop replacement for `androidx.hilt.work.HiltWorkerFactory`.
 * Dispatches worker class names to Dagger-backed `@AssistedFactory` creators
 * registered at startup.
 */
@Singleton
class HiltWorkerFactory @Inject constructor() : WorkerFactory() {

    fun interface ChildWorkerFactory {
        fun create(appContext: Context, params: WorkerParameters): ListenableWorker
    }

    private val factories = ConcurrentHashMap<String, ChildWorkerFactory>()

    fun register(workerClassName: String, factory: ChildWorkerFactory) {
        factories[workerClassName] = factory
    }

    inline fun <reified T : ListenableWorker> register(factory: ChildWorkerFactory) {
        register(T::class.java.name, factory)
    }

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = factories[workerClassName]?.create(appContext, workerParameters)
}
