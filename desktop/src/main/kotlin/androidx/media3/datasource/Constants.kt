package androidx.media3.datasource

/**
 * `C.LENGTH_UNSET` is a Java `int` (-1) that media3's Java code compares against `long`s via
 * implicit widening. Kotlin has no such widening, so the ports use this `Long` twin.
 */
internal const val LENGTH_UNSET_LONG: Long = -1L
