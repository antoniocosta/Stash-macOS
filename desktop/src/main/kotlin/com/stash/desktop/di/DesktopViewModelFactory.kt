package com.stash.desktop.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Hilt's HiltViewModelFactory equivalent: for each ViewModel, a fresh `@ViewModelScoped` Dagger
 * subcomponent is created with the SavedStateHandle bound, and the `@HiltViewModel` class is
 * resolved from its `Map<Class, Provider<ViewModel>>`. [creator] is installed once at startup by Main.
 */
object DesktopViewModelFactory : ViewModelProvider.Factory {
    @Volatile
    var creator: ((modelClass: Class<out ViewModel>, handle: SavedStateHandle) -> ViewModel)? = null

    override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
        val create = checkNotNull(creator) { "DesktopViewModelFactory.creator not installed" }
        @Suppress("UNCHECKED_CAST")
        return create(modelClass.java, extras.createSavedStateHandle()) as T
    }
}
