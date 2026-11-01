package android.app.usage

/** Desktop shim for UsageStatsManager: a running Mac app is always "active". */
class UsageStatsManager internal constructor() {
    val appStandbyBucket: Int get() = STANDBY_BUCKET_ACTIVE

    companion object {
        const val STANDBY_BUCKET_ACTIVE = 10
        const val STANDBY_BUCKET_WORKING_SET = 20
        const val STANDBY_BUCKET_FREQUENT = 30
        const val STANDBY_BUCKET_RARE = 40
        const val STANDBY_BUCKET_RESTRICTED = 45
    }
}
