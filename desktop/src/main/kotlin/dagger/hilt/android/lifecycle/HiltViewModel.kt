package dagger.hilt.android.lifecycle

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HiltViewModel(
    val assistedFactory: KClass<*> = Any::class,
)
