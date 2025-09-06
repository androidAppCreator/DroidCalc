/**
 * ViewModel for the Split Screen (SplitCalculator).
 * This class manages the UI state, handles user interactions via sealed intents as defined
 * in [SplitContract], and orchestrates calculations for splitting a bill among participants,
 * adhering to MVI architectural principles.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.split.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm
import com.droid.droidcalc.domain.usecase.SplitUseCase
import com.droid.droidcalc.ui.split.SplitContract // Ensure this import is correct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

private const val MIN_PARTICIPANTS = 2
private const val MAX_PARTICIPANTS = 10

/**
 * ViewModel for the SplitScreen, implementing MVI with [SplitContract].
 * It handles user interactions, manages UI state, and triggers side effects like snackbar messages.
 *
 * @param savedStateHandle Handle to access navigation arguments (e.g., initial total from CalculatorScreen).
 * @param splitUseCase The use case for performing split calculations.
 * @author DroidSwap
 */
@HiltViewModel
class SplitViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val splitUseCase: SplitUseCase
    // TODO: Inject Repository for Firestore history when implementing history feature
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitContract.SplitScreenState())
    val uiState: StateFlow<SplitContract.SplitScreenState> = _uiState.asStateFlow()

    private val _uiEffectChannel = Channel<SplitContract.UiEffect>(Channel.BUFFERED) // For side effects like Snackbars
    val uiEffect = _uiEffectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val totalAmountString = savedStateHandle.get<String>("total") 
            val initialTotal = totalAmountString?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            _uiState.update {
                it.copy(
                    initialTotalAmount = initialTotal.setScale(2, RoundingMode.HALF_UP),
                    participants = if (initialTotal > BigDecimal.ZERO && MIN_PARTICIPANTS > 1) {
                        List(MIN_PARTICIPANTS) { i -> Participant(name = "Participant ${i + 1}") }
                    } else {
                        listOf(Participant(name = "Participant 1"))
                    }
                )
            }
            // processIntent(SplitContract.SplitIntent.LoadHistory) // Example: Load history on init
        }
    }

    /**
     * Processes the given [SplitContract.SplitIntent] to update the UI state or trigger actions.
     * This is the sole entry point for all user interactions from the UI.
     *
     * @param intent The [SplitContract.SplitIntent] representing the user's action.
     * @author DroidSwap
     */
    fun processIntent(intent: SplitContract.SplitIntent) {
        viewModelScope.launch {
            when (intent) {
                is SplitContract.SplitIntent.UpdateTipAmount -> handleUpdateTipAmount(intent.amount)
                is SplitContract.SplitIntent.UpdateTaxAmount -> handleUpdateTaxAmount(intent.amount)
                is SplitContract.SplitIntent.SelectAlgorithm -> handleSelectAlgorithm(intent.algorithm)
                is SplitContract.SplitIntent.AddParticipant -> handleAddParticipant()
                is SplitContract.SplitIntent.RemoveParticipant -> handleRemoveParticipant(intent.participantId)
                is SplitContract.SplitIntent.UpdateParticipantField -> handleUpdateParticipantField(intent.participantId, intent.updateAction)
                is SplitContract.SplitIntent.CalculateSplit -> handleCalculateSplit()
                is SplitContract.SplitIntent.ToggleRoundingExplanation -> handleToggleRoundingExplanation(intent.show)
                is SplitContract.SplitIntent.ClearError -> handleClearError(intent.errorKey)
                is SplitContract.SplitIntent.LoadHistory -> { /* TODO: Implement history loading */ }
            }
        }
    }

    private fun handleUpdateTipAmount(tip: String) {
        _uiState.update { it.copy(tipAmountInput = tip, errorMessages = it.errorMessages.minus("tip")) }
    }

    private fun handleUpdateTaxAmount(tax: String) {
        _uiState.update { it.copy(taxAmountInput = tax, errorMessages = it.errorMessages.minus("tax")) }
    }

    private fun handleSelectAlgorithm(algorithm: SplitAlgorithm) {
        _uiState.update { it.copy(selectedAlgorithm = algorithm, splitResult = null, errorMessages = it.errorMessages.minus("percentageSum")) } 
    }

    private fun handleAddParticipant() {
        _uiState.update { currentState ->
            if (currentState.participants.size < MAX_PARTICIPANTS) {
                val newParticipantNumber = currentState.participants.size + 1
                // Participant name should ideally come from a string resource with formatting if needed
                val newParticipant = Participant(name = "Participant $newParticipantNumber")
                currentState.copy(participants = currentState.participants + newParticipant, errorMessages = currentState.errorMessages.minus("participants_limit"))
            } else {
                // Message should be from string resources
                _uiEffectChannel.trySend(SplitContract.UiEffect.ShowSnackbar("Maximum $MAX_PARTICIPANTS participants allowed."))
                currentState // No state change if limit reached, only effect
            }
        }
    }

    private fun handleRemoveParticipant(participantId: String) {
        _uiState.update { currentState ->
            if (currentState.participants.size > MIN_PARTICIPANTS) {
                currentState.copy(participants = currentState.participants.filter { it.id != participantId }, errorMessages = currentState.errorMessages.minus("participants_limit"))
            } else {
                 // Message should be from string resources
                _uiEffectChannel.trySend(SplitContract.UiEffect.ShowSnackbar("Minimum $MIN_PARTICIPANTS participants required."))
                currentState // No state change if limit reached, only effect
            }
        }
    }

    private fun handleUpdateParticipantField(participantId: String, updateAction: (Participant) -> Participant) {
        _uiState.update { currentState ->
            currentState.copy(participants = currentState.participants.map {
                if (it.id == participantId) updateAction(it) else it
            })
        }
    }

    private fun handleClearError(errorKey: String) {
        _uiState.update { it.copy(errorMessages = it.errorMessages.minus(errorKey)) }
    }

    private fun handleToggleRoundingExplanation(show: Boolean) {
        _uiState.update { it.copy(showRoundingExplanation = show) }
    }

    private fun handleCalculateSplit() {
        _uiState.update { it.copy(isLoading = true, splitResult = null, errorMessages = emptyMap()) } // Clear previous field errors

        val currentState = _uiState.value
        val billAmount = currentState.initialTotalAmount
        val tip = currentState.tipAmountInput.toBigDecimalOrNullOrBlank() ?: BigDecimal.ZERO
        val tax = currentState.taxAmountInput.toBigDecimalOrNullOrBlank() ?: BigDecimal.ZERO

        val currentFieldErrors = mutableMapOf<String, String>()
        // Field-specific validations - these messages are for inline display
        if (currentState.tipAmountInput.isNotBlank() && currentState.tipAmountInput.toBigDecimalOrNullOrBlank() == null) currentFieldErrors["tip"] = "Invalid tip amount"
        else if (tip < BigDecimal.ZERO) currentFieldErrors["tip"] = "Tip cannot be negative"
        
        if (currentState.taxAmountInput.isNotBlank() && currentState.taxAmountInput.toBigDecimalOrNullOrBlank() == null) currentFieldErrors["tax"] = "Invalid tax amount"
        else if (tax < BigDecimal.ZERO) currentFieldErrors["tax"] = "Tax cannot be negative"

        val totalAmountForPercentageCheck = billAmount + tip + tax
        if (currentState.selectedAlgorithm == SplitAlgorithm.PERCENTAGE && totalAmountForPercentageCheck > BigDecimal.ZERO) {
            val totalPercentage = currentState.participants.sumOf {
                it.percentageInput.toBigDecimalOrNullOrBlank() ?: BigDecimal.ZERO
            }
            if (totalPercentage.compareTo(BigDecimal(100)) != 0) {
                currentFieldErrors["percentageSum"] = "Percentages must sum to 100%."
            }
        }

        currentState.participants.forEachIndexed { index, p ->
            val participantErrorKeyPrefix = "p${index}"
            when (currentState.selectedAlgorithm) {
                SplitAlgorithm.FIXED_THEN_EQUAL -> {
                    p.fixedAmountInput.toBigDecimalOrNullOrBlank()?.let {
                        if (it < BigDecimal.ZERO) currentFieldErrors["${participantErrorKeyPrefix}_fixed"] = "Fixed amount cannot be negative."
                    } ?: "Invalid fixed amount."
                }
                SplitAlgorithm.PERCENTAGE -> {
                    p.percentageInput.toBigDecimalOrNullOrBlank()?.let {
                        if (it < BigDecimal.ZERO) currentFieldErrors["${participantErrorKeyPrefix}_percentage"] = "Percentage cannot be negative."
                    } ?: "Invalid percentage."
                }
                SplitAlgorithm.WEIGHTED, SplitAlgorithm.RATIO -> {
                    val inputField = if (currentState.selectedAlgorithm == SplitAlgorithm.WEIGHTED) p.weightInput else p.ratioInput
                    val fieldName = currentState.selectedAlgorithm.name.lowercase()
                    inputField.toBigDecimalOrNullOrBlank()?.let {
                        if (it <= BigDecimal.ZERO) currentFieldErrors["${participantErrorKeyPrefix}_${fieldName}"] = "${fieldName.capitalizeWord()} must be positive."
                    } ?: "Invalid ${fieldName}."
                }
                SplitAlgorithm.EQUAL -> { /* No participant-specific fields to validate here */ }
            }
        }

        if (currentFieldErrors.isNotEmpty()) {
            _uiState.update { it.copy(isLoading = false, errorMessages = currentFieldErrors) }
            return
        }

        try {
            val result = splitUseCase(
                totalBill = billAmount,
                tipAmount = tip,
                taxAmount = tax,
                participants = currentState.participants,
                algorithm = currentState.selectedAlgorithm
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    splitResult = result,
                    finalTotalToSplit = billAmount + tip + tax,
                    errorMessages = emptyMap() 
                )
            }
            // TODO: Save split to history if successful (e.g., via another intent and repository call)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(isLoading = false) } // Clear loading state
            // Message should be from string resources
            _uiEffectChannel.trySend(SplitContract.UiEffect.ShowSnackbar(e.message ?: "Calculation error due to invalid inputs."))
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) } // Clear loading state
            // Message should be from string resources
            _uiEffectChannel.trySend(SplitContract.UiEffect.ShowSnackbar("An unexpected error occurred during calculation."))
        }
    }
}

/** 
 * Helper extension to parse a String to BigDecimal, returning null if the string is blank or not a valid number.
 * This is useful for optional input fields where blank means zero or no input.
 *
 * @return The [BigDecimal] representation of the string, or null if blank or invalid.
 * @author DroidSwap
 */
private fun String.toBigDecimalOrNullOrBlank(): BigDecimal? {
    return if (this.isBlank()) null else this.toBigDecimalOrNull()
}

/** 
 * Helper extension to capitalize the first letter of a word.
 * For example, "word" becomes "Word".
 *
 * @return The string with its first character capitalized.
 * @author DroidSwap
 */
private fun String.capitalizeWord(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

