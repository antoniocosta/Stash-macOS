package dagger.hilt.android

/**
 * Same FQN as Hilt's marker (absent from JVM hilt-core). Desktop never
 * instantiates Android entry points (receivers/activities/services); classes
 * carrying it compile unchanged and any @Inject fields get a Dagger MembersInjector.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AndroidEntryPoint
