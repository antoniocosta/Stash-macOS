package coil3

/**
 * Desktop superset of coil3.PlatformContext (with a protected rather than private/sealed
 * constructor so android.content.Context can extend it directly, matching Android where
 * PlatformContext is a typealias for android.content.Context).
 */
abstract class PlatformContext protected constructor() {
    companion object {
        @JvmField
        val INSTANCE: PlatformContext = object : PlatformContext() {}
    }
}
