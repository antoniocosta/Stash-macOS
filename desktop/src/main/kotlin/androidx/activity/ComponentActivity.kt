package androidx.activity

import android.app.Activity
import androidx.compose.runtime.Composable

open class ComponentActivity : Activity() {
    var composeContent: (@Composable () -> Unit)? = null
        internal set
}

fun ComponentActivity.enableEdgeToEdge() {
    // Edge-to-edge system bar insets are an Android-only concept; macOS window uses full content area.
}
