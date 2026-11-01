package androidx.core.app

import android.app.Activity

/** Desktop shim for ActivityCompat: no runtime permission model, so there is never a rationale to show. */
object ActivityCompat {
    @JvmStatic fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean = false
    @JvmStatic fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {}
    @JvmStatic fun recreate(activity: Activity) = activity.recreate()
    @JvmStatic fun finishAffinity(activity: Activity) = activity.finish()
}
