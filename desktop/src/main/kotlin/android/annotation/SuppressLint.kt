package android.annotation

/** Android Lint suppression annotation; no effect on the JVM. */
@Retention(AnnotationRetention.SOURCE)
annotation class SuppressLint(vararg val value: String)
