/**
 * This file defines a contract for common UI effects that can be emitted by ViewModels
 * across the DroidCalc application. These effects represent one-time events that the UI
 * should react to, such as displaying a Snackbar message.
 * This promotes adherence to MVI principles for handling side effects.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.common

/**
 * Defines a sealed interface for common UI effects that ViewModels can emit.
 * These are typically one-shot events that the UI (e.g., a Composable screen or an Activity)
 * should handle.
 *
 * @author DroidSwap
 */
sealed interface UiEffect {

    /**
     * Represents an effect to show a Snackbar message.
     *
     * @property message The primary message to be displayed in the Snackbar.
     * @property actionLabel Optional label for an action button on the Snackbar.
     * @property duration The duration for which the Snackbar should be displayed. Defaults to Short.
     * @property withDismissAction Indicates if the Snackbar should have a dismiss action. Not typically needed if actionLabel is present.
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        // val duration: androidx.compose.material3.SnackbarDuration = androidx.compose.material3.SnackbarDuration.Short, // Requires Material3 import, manage in UI layer
        // val withDismissAction: Boolean = false
    ) : UiEffect

    // Add other common UI effects as needed, for example:
    // data class ShowToast(val message: String) : UiEffect
    // data object PlaySound(val soundId: Int) : UiEffect
    // data class Vibrate(val pattern: LongArray) : UiEffect
}
