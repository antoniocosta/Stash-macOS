package androidx.activity

import android.app.Activity
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Desktop shim for androidx.activity.ComponentActivity: a [LifecycleOwner] (so upstream's
 * `lifecycleScope` / `repeatOnLifecycle` work) whose state `Main.kt` drives from the window —
 * CREATED in [onCreate], RESUMED while the window is shown ([onResume]/[onPause]), DESTROYED in
 * [onDestroy] — and a [ViewModelStoreOwner].
 */
open class ComponentActivity : Activity(), LifecycleOwner, ViewModelStoreOwner {
    var composeContent: (@Composable () -> Unit)? = null
        internal set

    private val registry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: Lifecycle get() = registry

    private var store = ViewModelStore()

    override val viewModelStore: ViewModelStore get() = store

    /**
     * Desktop: Android's non-configuration instance handover — the instance that replaces
     * [previous] in a recreate keeps its ViewModels.
     */
    internal fun retainNonConfigurationState(previous: ComponentActivity) {
        store = previous.store
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registry.currentState = Lifecycle.State.CREATED
    }

    override fun onResume() {
        super.onResume()
        registry.currentState = Lifecycle.State.RESUMED
    }

    override fun onPause() {
        super.onPause()
        if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) registry.currentState = Lifecycle.State.STARTED
    }

    override fun onDestroy() {
        if (registry.currentState != Lifecycle.State.INITIALIZED) registry.currentState = Lifecycle.State.DESTROYED
        if (!isChangingConfigurations) viewModelStore.clear()
        super.onDestroy()
    }
}

fun ComponentActivity.enableEdgeToEdge() {
    // Edge-to-edge system bar insets are an Android-only concept; macOS window uses full content area.
}
