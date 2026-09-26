package androidx.navigation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore

/**
 * Desktop replacement for `androidx.navigation.NavHostController` (from `navigation-runtime-desktop`).
 *
 * Upstream `StashNavHost` navigates to `SettingsRoute` from `HomeScreen` / `SyncScreen` via
 * `navController.navigate(SettingsRoute) { launchSingleTop = true }` without `popUpTo(HomeRoute) { saveState = true }`.
 * When `SettingsRoute` (itself a `TopLevelDestination`) sits directly above `HomeRoute` while
 * `backStackMap` has no entry for `HomeRoute.id`, a subsequent bottom-bar click on `Home` calls
 * `popUpTo(HomeRoute.id) { inclusive = false; saveState = true }` followed by `restoreState = true`,
 * which in `NavControllerImpl.executePopOperations` saves the popped `SettingsRoute` under `HomeRoute.id`
 * and immediately restores `SettingsRoute` on top of `HomeRoute`.
 *
 * Ensuring `backStackMap[homeDestId] = null` whenever another top-level tab sits directly above
 * `HomeRoute` keeps `SettingsRoute` saved under its own destination ID rather than `HomeRoute.id`.
 */
open class NavHostController : NavController() {

    init {
        installTopLevelBackStackGuard()
    }

    final override fun setLifecycleOwner(owner: LifecycleOwner) {
        super.setLifecycleOwner(owner)
    }

    final override fun setViewModelStore(viewModelStore: ViewModelStore) {
        super.setViewModelStore(viewModelStore)
    }

    @Suppress("UNCHECKED_CAST")
    private fun installTopLevelBackStackGuard() {
        val backStackMap: MutableMap<Int, String?> = runCatching {
            val implField = NavController::class.java.getDeclaredField("impl").apply { isAccessible = true }
            val impl = implField.get(this)
            val mapField = impl.javaClass.getDeclaredField("backStackMap").apply { isAccessible = true }
            mapField.get(impl) as MutableMap<Int, String?>
        }.getOrNull() ?: return

        val nonHomeTopLevelRoutes = setOf(
            "com.stash.app.navigation.LibraryRoute",
            "com.stash.app.navigation.SearchRoute",
            "com.stash.app.navigation.SyncRoute",
            "com.stash.app.navigation.SettingsRoute",
        )
        val homeRouteName = "com.stash.app.navigation.HomeRoute"

        addOnDestinationChangedListener { controller, _, _ ->
            runCatching {
                val stack = controller.currentBackStack.value
                val homeIdx = stack.indexOfFirst { it.destination.route == homeRouteName }
                if (homeIdx >= 0) {
                    val homeDestId = stack[homeIdx].destination.id
                    val nextEntry = stack.getOrNull(homeIdx + 1)
                    if (nextEntry != null && nextEntry.destination.route in nonHomeTopLevelRoutes) {
                        backStackMap[homeDestId] = null
                    }
                }
            }
        }
    }
}
