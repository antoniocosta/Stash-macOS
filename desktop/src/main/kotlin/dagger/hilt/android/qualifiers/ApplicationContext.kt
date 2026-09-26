package dagger.hilt.android.qualifiers

import javax.inject.Qualifier

/**
 * Same FQN and semantics as Hilt's qualifier (absent from the JVM hilt-core
 * artifact). Bound to [android.content.DesktopContext] by DesktopPlatformModule.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class ApplicationContext
