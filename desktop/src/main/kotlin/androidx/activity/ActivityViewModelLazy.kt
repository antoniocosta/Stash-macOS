package androidx.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stash.desktop.di.DesktopViewModelFactory

/** activity-ktx's `by viewModels()`: the ViewModel from this activity's store, made by the desktop Hilt factory. */
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null,
): Lazy<VM> = lazy {
    ViewModelProvider.create(viewModelStore, factoryProducer?.invoke() ?: DesktopViewModelFactory)[VM::class]
}
