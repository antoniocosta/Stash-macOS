package com.google.android.gms.tasks

/**
 * Desktop shim for Play Services' Task. There are no Play Services on macOS: tasks handed out by
 * the shims here are already complete (normally with the failure a device without Play Services
 * reports), and listeners run at once.
 */
abstract class Task<T> {
    abstract val isComplete: Boolean
    abstract val isSuccessful: Boolean
    abstract val isCanceled: Boolean
    abstract val result: T
    abstract val exception: Exception?
    abstract fun addOnSuccessListener(listener: OnSuccessListener<in T>): Task<T>
    abstract fun addOnFailureListener(listener: OnFailureListener): Task<T>
    abstract fun addOnCanceledListener(listener: OnCanceledListener): Task<T>
}

fun interface OnSuccessListener<T> { fun onSuccess(result: T) }
fun interface OnFailureListener { fun onFailure(e: Exception) }
fun interface OnCanceledListener { fun onCanceled() }

class RuntimeExecutionException(cause: Throwable) : RuntimeException(cause)

object Tasks {
    @JvmStatic
    fun <T> forResult(result: T): Task<T> = CompletedTask(result, null)

    @JvmStatic
    fun <T> forException(e: Exception): Task<T> = CompletedTask(null, e)
}

private class CompletedTask<T>(private val value: T?, override val exception: Exception?) : Task<T>() {
    override val isComplete get() = true
    override val isSuccessful get() = exception == null
    override val isCanceled get() = false

    @Suppress("UNCHECKED_CAST")
    override val result: T get() = exception?.let { throw RuntimeExecutionException(it) } ?: value as T

    override fun addOnSuccessListener(listener: OnSuccessListener<in T>) = apply { if (exception == null) listener.onSuccess(result) }
    override fun addOnFailureListener(listener: OnFailureListener) = apply { exception?.let(listener::onFailure) }
    override fun addOnCanceledListener(listener: OnCanceledListener) = this
}
