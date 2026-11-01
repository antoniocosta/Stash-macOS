package android.provider

import android.content.ContentResolver

/** Desktop shim for android.provider.Settings: the actions and global values upstream reads. */
object Settings {
    const val ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ACTION_MANAGE_UNKNOWN_APP_SOURCES = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
    const val ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
    const val ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
    const val ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    const val EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE"

    object Global {
        const val ANIMATOR_DURATION_SCALE = "animator_duration_scale"
        const val TRANSITION_ANIMATION_SCALE = "transition_animation_scale"

        /** macOS has no animator scale; "Reduce motion" is not visible to the JVM, so animations run at 1x. */
        @JvmStatic
        fun getFloat(cr: ContentResolver?, name: String, def: Float): Float = when (name) {
            ANIMATOR_DURATION_SCALE, TRANSITION_ANIMATION_SCALE -> 1f
            else -> def
        }

        @JvmStatic
        fun getInt(cr: ContentResolver?, name: String, def: Int): Int = def
    }

    object Secure {
        @JvmStatic
        fun getString(cr: ContentResolver?, name: String): String? = null
    }
}
