/**
 * This file defines the contract for navigation events within the DroidCalc application.
 * It provides a standardized way for ViewModels to request navigation actions,
 * which are then handled by a component that has access to the NavController.
 * This promotes decoupling and adherence to MVI principles for side effects like navigation.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.navigation

/**
 * Defines a contract for navigation events that can be emitted by ViewModels.
 * UI layers (like an Activity or a top-level Composable managing navigation) can collect these events
 * to perform actual navigation actions.
 *
 * @author DroidSwap
 */
sealed interface NavigationEvent {
    /**
     * Represents an event to navigate to a specific route.
     *
     * @property route The destination route string (e.g., "settings", "profile/{userId}").
     * @property popUpToRoute The route to pop up to before navigating. Null means no pop-up action.
     * @property inclusive Whether the popUpToRoute itself should be popped.
     * @property isLaunchSingleTop Whether this navigation action should launch as single top.
     */
    data class NavigateToRoute(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
        val isLaunchSingleTop: Boolean = false
    ) : NavigationEvent

    /**
     * Represents an event to navigate back in the current back stack.
     */
    data object NavigateBack : NavigationEvent

    // Add other common navigation actions as needed, e.g.:
    // data class NavigateToRouteWithArgs(val route: String, val args: Bundle) : NavigationEvent
    // data object PopUpToRoot : NavigationEvent
}
