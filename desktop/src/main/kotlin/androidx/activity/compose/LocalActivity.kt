package androidx.activity.compose

import android.app.Activity
import androidx.compose.runtime.compositionLocalOf

/** activity-compose's LocalActivity; Main.kt provides the window's activity. */
val LocalActivity = compositionLocalOf<Activity?> { null }
