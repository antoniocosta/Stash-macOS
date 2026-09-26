package android.content.pm

/** Desktop shim for android.content.pm.ResolveInfo; desktop resolutions always carry an activity. */
class ResolveInfo(@JvmField var activityInfo: ActivityInfo)
