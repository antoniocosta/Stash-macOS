package android

/** Desktop shim for android.Manifest: permission names (every permission is granted on desktop). */
object Manifest {
    object permission {
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        const val CAMERA = "android.permission.CAMERA"
        const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
        const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
        const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    }
}
