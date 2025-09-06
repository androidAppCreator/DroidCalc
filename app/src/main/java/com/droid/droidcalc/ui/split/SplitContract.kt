/**
 * Defines the contract for the Split Screen, encapsulating its UI State, Intents, and UI Effects.
 * This follows the MVI (Model-View-Intent) architecture pattern, serving as the single source of truth
 * for the split bill feature's state, user actions, and one-time side effects.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.split

import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm
import java.math.BigDecimal

/**
 * Defines the contract for the Split Screen, including its state, user intents, and UI effects.
 * @author DroidSwap
 */
object SplitContract {

    /**
     * Represents a single item in the split history carousel.
     * This is a simplified representation for now; can be expanded with more details from Firestore.
     *
     * @property id Unique identifier for the history item (e.g., Firestore document ID).
     * @property displayTitle A title for the history item, e.g., "Split of $100.00".
     * @property algorithmName The name of the algorithm used.
     * @property participantCount The number of participants in that split.
     * @property timestamp The time the split was saved.
     * @author DroidSwap
     */
    data class SplitHistoryItemDisplay(
        val id: String,
        val displayTitle: String,
        val algorithmName: String,
        val participantCount: Int,
        val timestamp: Long // For sorting or display
    )

    /**
     * Represents the immutable UI state for the SplitScreen.
     *
     * @property initialTotalAmount The initial total amount passed from the CalculatorScreen (read-only on this screen).
     * @property tipAmountInput User input for the tip amount (as a string for TextField binding).
     * @property taxAmountInput User input for the tax amount (as a string for TextField binding).
     * @property participants The list of [Participant] objects involved in the split.
     * @property selectedAlgorithm The currently selected [SplitAlgorithm].
     * @property splitResult A map where the key is the participant ID and the value is their calculated share (as BigDecimal).
     * @property finalTotalToSplit The total amount including bill, tip, and tax, after parsing inputs.
     * @property isLoading Indicates if a calculation or data fetching operation is in progress.
     * @property errorMessages A map of error messages, where the key could be a field identifier or a general error type.
     *                         These are typically for inline field validation errors.
     * @property showRoundingExplanation Controls the visibility of the rounding explanation UI element.
     * @property splitHistory List of past splits fetched from Firestore for the carousel.
     * @author DroidSwap
     */
    data class SplitScreenState(
        val initialTotalAmount: BigDecimal = BigDecimal.ZERO,
        val tipAmountInput: String = "",
        val taxAmountInput: String = "",
        val participants: List<Participant> = listOf(Participant(name = "Participant 1")), // Start with one participant
        val selectedAlgorithm: SplitAlgorithm = SplitAlgorithm.EQUAL,
        val splitResult: Map<String, BigDecimal>? = null, // Participant ID to their share
        val finalTotalToSplit: BigDecimal? = null,
        val isLoading: Boolean = false,
        val errorMessages: Map<String, String> = emptyMap(), // e.g., "tipAmountError" to "Invalid tip"
        val showRoundingExplanation: Boolean = false,
        val splitHistory: List<SplitHistoryItemDisplay> = emptyList()
    )

    /**
     * Defines the user intents (actions) that can be triggered from the Split Screen UI.
     * These are processed by the [com.droid.droidcalc.ui.split.viewmodel.SplitViewModel].
     * @author DroidSwap
     */
    sealed class SplitIntent {
        /** Intent to update the tip amount input string. */
        data class UpdateTipAmount(val amount: String) : SplitIntent()
        /** Intent to update the tax amount input string. */
        data class UpdateTaxAmount(val amount: String) : SplitIntent()
        /** Intent to select a specific split algorithm. */
        data class SelectAlgorithm(val algorithm: SplitAlgorithm) : SplitIntent()
        /** Intent to add a new participant. */
        data object AddParticipant : SplitIntent()
        /** Intent to remove a participant by their ID. */
        data class RemoveParticipant(val participantId: String) : SplitIntent()
        /** Intent to update a field of a specific participant. */
        data class UpdateParticipantField(val participantId: String, val updateAction: (Participant) -> Participant) : SplitIntent()
        /** Intent to trigger the split calculation. */
        data object CalculateSplit : SplitIntent()
        /** Intent to toggle the visibility of the rounding explanation. */
        data class ToggleRoundingExplanation(val show: Boolean) : SplitIntent()
        /** Intent to clear a specific error message by its key. */
        data class ClearError(val errorKey: String) : SplitIntent()
        /** Intent to load split history (example for future use). */
        data object LoadHistory : SplitIntent() // Example
    }

    /**
     * Defines one-time UI effects that can be triggered by the ViewModel.
     * These are typically handled by the UI layer to show transient messages or trigger navigation.
     * @author DroidSwap
     */
    sealed interface UiEffect {
        /**
         * Effect to show a transient Snackbar message to the user.
         * @property message The message to be displayed. Ideally, this should be a string resource ID
         *                   to support localization, but for simplicity, a direct String is used here.
         *                   The ViewModel should resolve this from string resources if possible.
         * @author DroidSwap
         */
        data class ShowSnackbar(val message: String) : UiEffect
        // Future effects like NavigateTo can be added here.
    }
}
