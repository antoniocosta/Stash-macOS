package androidx.hilt.navigation.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stash.desktop.di.DesktopViewModelFactory

/**
 * hilt-navigation-compose's `hiltViewModel()` (Android-only artifact). Same contract: the ViewModel is
 * scoped to [viewModelStoreOwner] (the NavBackStackEntry inside a NavHost) and created by the Dagger
 * graph through [DesktopViewModelFactory], with a SavedStateHandle built from the owner's CreationExtras.
 */
@Composable
inline fun <reified VM : ViewModel> hiltViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM = viewModel(viewModelStoreOwner = viewModelStoreOwner, key = key, factory = DesktopViewModelFactory)
