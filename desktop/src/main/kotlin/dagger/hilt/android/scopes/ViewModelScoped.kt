package dagger.hilt.android.scopes

import javax.inject.Scope

/** Hilt's ViewModel scope (hilt-android is Android-only). The desktop DI creates one `@ViewModelScoped` Dagger subcomponent per ViewModel, as Hilt's ViewModelComponent does. */
@Scope
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ViewModelScoped
