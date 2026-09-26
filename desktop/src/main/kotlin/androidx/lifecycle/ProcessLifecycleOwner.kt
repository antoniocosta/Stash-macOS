package androidx.lifecycle

object ProcessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this).apply {
        currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle
        get() = registry

    @JvmStatic
    fun get(): LifecycleOwner = this
}
