/**
 * Defines the contract for the Split Screen, encapsulating its UI State, Intents, and UI Effects.
 * This follows the MVI (Model-View-Intent) architecture pattern, serving as the single source of truth
 * for the split bill feature's state, user actions, and one-time side effects.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.split

import androidx.compose.ui.graphics.Color
import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm // Added import for SplitAlgorithm
import com.droid.droidcalc.ui.split.model.NewParticipantData
import java.math.BigDecimal

/**
 * Defines the contract for the Split Screen, including its state, user intents, and UI effects.
 * Adheres to MVI principles, ensuring a unidirectional data flow and a single source of truth for UI state.
 * Updated to reflect the new UI design and re-introduction of algorithm selection.
 */
object SplitContract {

    /**
     * Represents a single item in the split history carousel (for future use).
     *
     * @property id Unique identifier for the history item.
     * @property displayTitle A title for the history item.
     * @property algorithmName The name of the algorithm used (still relevant for history).
     * @property participantCount The number of participants.
     * @property timestamp The time the split was saved.
     */
    data class SplitHistoryItemDisplay(
        val id: String,
        val displayTitle: String,
        val algorithmName: String, // Kept for historical data display
        val participantCount: Int,
        val timestamp: Long
    )

    /**
     * Represents auxiliary information about the merchant for display in the header card.
     * Enhanced to include fields potentially needed for the new header UI.
     *
     * @property name The name of the merchant (e.g., "Burger Gembel").
     * @property date The date of the transaction (e.g., "22 Jun 2023").
     * @property iconUrl Optional URL for the merchant's avatar/icon (e.g., burger icon).
     * @property donationNote Optional note about any donations (e.g., "Burger Gembel sends 2.99 USD for nature conservation").
     * @property customIllustrationUrl Optional URL or resource ID for custom illustrations (e.g., windmills).
     */
    data class MerchantInfo(
        val name: String,
        val date: String,
        val iconUrl: String? = null,
        val donationNote: String? = null,
        val customIllustrationUrl: String? = null // For UI elements like windmills
    )

    /**
     * Represents the immutable UI state for the SplitScreen.
     *
     * @property initialTotalAmount The initial total amount potentially passed from navigation or a previous step.
     * @property totalAmountInput String representation of the total amount, primarily for display in the header.
     * @property participants The list of [Participant] objects involved in the split.
     * @property selectedAlgorithm The currently selected [SplitAlgorithm] for calculating the split.
     * @property splitResult A map of participant ID to their calculated monetary share.
     * @property participantPercentages A map of participant ID to their share represented as a percentage string (e.g., "20%").
     * @property participantCardColors A map of participant ID to the specific [Color] for their card background.
     * @property finalTotalToSplit The total amount that was actually split (can be same as initialTotalAmount or adjusted).
     * @property isLoading Indicates if a background operation (like calculation) is in progress.
     * @property errorMessages A map of error messages for UI fields or general errors.
     * @property splitHistory List of past splits (for future use).
     * @property merchantInfo Optional information about the merchant for the header card.
     * @property amountLeftToSplit Optional display of how much amount is left if the split is not perfectly balanced (for header card).
     */
    data class SplitScreenState(
        val initialTotalAmount: BigDecimal = BigDecimal.ZERO,
        val totalAmountInput: String = "", // Primarily for display now
        val participants: List<Participant> = emptyList(),
        val selectedAlgorithm: SplitAlgorithm = SplitAlgorithm.EQUAL, // Re-added selectedAlgorithm
        val splitResult: Map<String, BigDecimal>? = null,
        val participantPercentages: Map<String, String> = emptyMap(),
        val participantCardColors: Map<String, Color> = emptyMap(),
        val finalTotalToSplit: BigDecimal? = null,
        val isLoading: Boolean = false,
        val errorMessages: Map<String, String> = emptyMap(),
        val splitHistory: List<SplitHistoryItemDisplay> = emptyList(),
        val merchantInfo: MerchantInfo? = null,
        val amountLeftToSplit: BigDecimal? = null
    )

    /**
     * Defines the user intents (actions) that can be triggered from the Split Screen UI.
     */
    sealed class SplitIntent {
        /** Intent to update the total bill amount (likely from a previous screen or dialog). */
        data class UpdateTotalAmountInput(val input: String) : SplitIntent()

        /** Intent to select a specific split algorithm. */
        data class SelectAlgorithm(val algorithm: SplitAlgorithm) : SplitIntent() // Re-added SelectAlgorithm intent

        /** Intent to add a new participant with data from the AddFriendSheet. */
        data class AddNewParticipant(val data: NewParticipantData) : SplitIntent()
        /** Intent to remove a participant by their ID. */
        data class RemoveParticipant(val participantId: String) : SplitIntent()
        /** Intent to update a field of a specific participant (e.g., toggling isPaid). */
        data class UpdateParticipantField(val participantId: String, val updateAction: (Participant) -> Participant) : SplitIntent()
        
        /** Intent to trigger the split calculation or proceed to the next step. */
        data object CalculateSplit : SplitIntent()
        
        /** Intent to clear a specific error message by its key. */
        data class ClearError(val errorKey: String) : SplitIntent()
        /** Intent to load split history (for future use). */
        data object LoadHistory : SplitIntent() 
        /** Intent to show a temporary snackbar message. */
        data class ShowSnackbarMessage(val message: String) : SplitIntent()
        /** Intent to initiate the share flow. */
        data object ShareBillAction : SplitIntent()
    }

    /**
     * Defines one-time UI effects that can be triggered by the ViewModel.
     */
    sealed interface UiEffect {
        /**
         * Effect to show a transient Snackbar message to the user.
         * @property message The message to be displayed.
         */
        data class ShowSnackbar(val message: String) : UiEffect
    }
}
