package androidx.activity.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/** activity-compose's BackHandler (Android-only artifact), delegating to Compose Multiplatform's back dispatcher (Esc on desktop). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) =
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)
