/**
 * Defines the UI state for the Split Screen (SplitCalculator).
 * This file contains the data class representing all information required to render the Split Screen.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.split.state

import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm
import java.math.BigDecimal

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
 * @property showRoundingExplanation Controls the visibility of the rounding explanation UI element.
 * @property splitHistory List of past splits fetched from Firestore for the carousel.
 * @property nextParticipantId Internal counter or mechanism to suggest unique IDs for new participants before they get a final one.
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
