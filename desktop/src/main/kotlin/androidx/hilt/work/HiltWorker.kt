package androidx.hilt.work

/**
 * Marker annotation with the same FQN as Hilt's. On desktop the worker's
 * @AssistedInject constructor is reached via a Dagger @AssistedFactory declared
 * in com.stash.desktop.di (the part hilt-work's compiler generates on Android).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class HiltWorker
