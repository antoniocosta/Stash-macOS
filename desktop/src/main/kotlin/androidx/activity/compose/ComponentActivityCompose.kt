package androidx.activity.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext

fun ComponentActivity.setContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit,
) {
    this.composeContent = content
}
