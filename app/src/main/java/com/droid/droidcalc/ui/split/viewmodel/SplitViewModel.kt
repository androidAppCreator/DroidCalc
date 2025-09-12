package com.droid.droidcalc.ui.split.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm
import com.droid.droidcalc.domain.usecase.SplitUseCase
import com.droid.droidcalc.navigation.CalculationResultHolder
import com.droid.droidcalc.ui.split.SplitContract
import com.droid.droidcalc.ui.split.model.NewParticipantData
// TODO: Replace example theme colors with MaterialTheme colors or a more robust palette solution from a central Theme file
// import com.droid.droidcalc.ui.theme.Pink40
// import com.droid.droidcalc.ui.theme.Purple40
// import com.droid.droidcalc.ui.theme.PurpleGrey40
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Named

// Constants for participant limits.
private const val MIN_PARTICIPANTS = 1
private const val MAX_PARTICIPANTS = 20

// Error key for total amount validation.
private const val TOTAL_AMOUNT_ERROR_KEY = "totalAmount"

// Error key for participant related errors (e.g. not enough participants)
private const val PARTICIPANT_ERROR_KEY = "participants"

// Error key for general calculation errors from the use case or unexpected exceptions.
private const val CALCULATION_ERROR_KEY = "calculation"

// Error key for participant specific input errors, e.g. percentage sum not 100%
private const val PARTICIPANT_INPUT_ERROR_KEY_PREFIX = "p_input_"

/**
 * ViewModel for the SplitScreen, responsible for managing the UI state based on the new design,
 * processing user intents, and orchestrating interactions between the UI layer and domain layer.
 * Adheres to MVI principles by exposing a single [SplitContract.SplitScreenState] and handling
 * [SplitContract.SplitIntent]s to ensure unidirectional data flow.
 *
 * @property splitUseCase The primary use case for performing bill splitting calculations.
 * @property calculationResultHolder Service to retrieve calculation results passed from other screens.
 * @property cardColorPalette A list of predefined colors used for participant cards.
 */
@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitUseCase: SplitUseCase,
    private val calculationResultHolder: CalculationResultHolder,
    // TODO: Define a more robust and theme-aligned color palette solution, possibly injected or from MaterialTheme.
    @Named("CardColorPalette") private val cardColorPalette: List<Color>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitContract.SplitScreenState()) // Initializes with default state from contract
    val uiState: StateFlow<SplitContract.SplitScreenState> = _uiState.asStateFlow()

    private val _uiEffectChannel = Channel<SplitContract.UiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffectChannel.receiveAsFlow()

    init {
        observeSharedCalculatorResult()
        initializeDefaultParticipants()
        // TODO: Load merchant info from a repository or allow user input in a future update.
        _uiState.update {
            it.copy(merchantInfo = SplitContract.MerchantInfo(
                name = "Burger Gembel", // Placeholder name
                date = "22 Jun 2023",   // Placeholder date
                donationNote = "Burger Gembel sends 2.99 USD for nature conservation" // Placeholder note
            ))
        }
    }

    /**
     * Observes [CalculationResultHolder] for a total amount passed from another screen (e.g., CalculatorScreen).
     * Updates [SplitContract.SplitScreenState.initialTotalAmount] and related fields if a new, unconsumed result is found.
     * This observation is tied to the [viewModelScope] and automatically cancels when the ViewModel is cleared.
     */
    private fun observeSharedCalculatorResult() {
        calculationResultHolder.calculatorResult
            .onEach { result ->
                if (result != null && !calculationResultHolder.resultConsumed.value) {
                    val scaledResult = result.setScale(2, RoundingMode.HALF_UP)
                    _uiState.update {
                        it.copy(
                            initialTotalAmount = scaledResult,
                            totalAmountInput = scaledResult.toPlainString(),
                            errorMessages = it.errorMessages.minus(TOTAL_AMOUNT_ERROR_KEY),
                            splitResult = null, // Clear previous results
                            participantPercentages = emptyMap(), // Clear percentages
                            finalTotalToSplit = scaledResult // This becomes the new basis for splitting
                        )
                    }
                    // If participants list is empty and a valid amount is received, add defaults and update UI attributes.
                    if (_uiState.value.participants.isEmpty() && scaledResult > BigDecimal.ZERO) {
                        val defaultParticipants = List(MIN_PARTICIPANTS.coerceAtLeast(1)) { i -> createDefaultParticipant(i) }
                        val (percentages, colors) = generateParticipantUIAttributes(defaultParticipants, scaledResult, emptyMap())
                        _uiState.update {
                            it.copy(
                                participants = defaultParticipants,
                                participantPercentages = percentages,
                                participantCardColors = colors
                            )
                        }
                    } else if (scaledResult > BigDecimal.ZERO) {
                        // If participants exist, recalculate their UI attributes based on the new total.
                        val (percentages, colors) = generateParticipantUIAttributes(
                            _uiState.value.participants, 
                            scaledResult, 
                            _uiState.value.splitResult ?: emptyMap()
                        )
                         _uiState.update {
                             it.copy(
                                 participantPercentages = percentages,
                                 participantCardColors = colors
                             )
                         }
                    }
                    calculationResultHolder.consumeCalculatorResult() // Mark result as consumed
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Initializes the participant list with a default set if it's currently empty.
     * Ensures the screen has a baseline state for user interaction, respecting [MIN_PARTICIPANTS].
     * Also generates initial UI attributes (percentages and colors) for these participants.
     */
    private fun initializeDefaultParticipants() {
        if (_uiState.value.participants.isEmpty()) {
            val defaultParticipants = List(MIN_PARTICIPANTS.coerceAtLeast(1)) { index ->
                createDefaultParticipant(index)
            }
            val currentTotal = _uiState.value.finalTotalToSplit ?: _uiState.value.initialTotalAmount
            val (percentages, colors) = generateParticipantUIAttributes(defaultParticipants, currentTotal, emptyMap())

            _uiState.update {
                it.copy(
                    participants = defaultParticipants,
                    participantPercentages = percentages,
                    participantCardColors = colors
                )
            }
        }
    }

    /**
     * Central handler for all [SplitContract.SplitIntent]s dispatched from the UI.
     * Ensures all user actions are processed within the [viewModelScope].
     * @param intent The [SplitContract.SplitIntent] representing the user's action or event.
     */
    fun processIntent(intent: SplitContract.SplitIntent) {
        viewModelScope.launch {
            when (intent) {
                is SplitContract.SplitIntent.UpdateTotalAmountInput -> handleUpdateTotalAmountInput(intent.input)
                is SplitContract.SplitIntent.SelectAlgorithm -> handleSelectAlgorithm(intent.algorithm)
                is SplitContract.SplitIntent.AddNewParticipant -> handleAddNewParticipant(intent.data)
                is SplitContract.SplitIntent.RemoveParticipant -> handleRemoveParticipant(intent.participantId)
                is SplitContract.SplitIntent.UpdateParticipantField -> handleUpdateParticipantField(intent.participantId, intent.updateAction)
                is SplitContract.SplitIntent.CalculateSplit -> handleCalculateSplit()
                is SplitContract.SplitIntent.ClearError -> handleClearError(intent.errorKey)
                is SplitContract.SplitIntent.LoadHistory -> { /* TODO: Implement history loading in a future update */ }
                is SplitContract.SplitIntent.ShowSnackbarMessage -> sendUiEffect(SplitContract.UiEffect.ShowSnackbar(intent.message))
                is SplitContract.SplitIntent.ShareBillAction -> handleShareBillAction()
            }
        }
    }

    // --- Intent Handler Implementations ---

    /** 
     * Handles updates to the total bill amount (typically from [CalculationResultHolder]).
     * Validates the input string and updates the state, including recalculating participant UI attributes.
     * @param input The new total amount as a string.
     */
    private fun handleUpdateTotalAmountInput(input: String) {
        _uiState.update { currentState ->
            val currentErrors = currentState.errorMessages.toMutableMap()
            currentErrors.remove(TOTAL_AMOUNT_ERROR_KEY)
            var newInitialTotal = currentState.initialTotalAmount

            if (input.isNotBlank()) {
                val parsedAmount = input.toBigDecimalOrNull()
                if (parsedAmount == null) {
                    currentErrors[TOTAL_AMOUNT_ERROR_KEY] = "Invalid total amount format."
                } else if (parsedAmount < BigDecimal.ZERO) {
                    currentErrors[TOTAL_AMOUNT_ERROR_KEY] = "Total amount cannot be negative."
                } else {
                    newInitialTotal = parsedAmount.setScale(2, RoundingMode.HALF_UP)
                }
            } else {
                 // If input is blank, it implies clearing or no value. Default to zero or maintain current state.
                 newInitialTotal = BigDecimal.ZERO // Or: currentState.initialTotalAmount depending on desired behavior
            }
            // Recalculate participant UI attributes based on the new total.
            val (percentages, colors) = generateParticipantUIAttributes(currentState.participants, newInitialTotal, currentState.splitResult ?: emptyMap())
            currentState.copy(
                initialTotalAmount = newInitialTotal,
                totalAmountInput = input, 
                errorMessages = currentErrors,
                finalTotalToSplit = newInitialTotal, // Update the effective total for splitting
                participantPercentages = percentages,
                participantCardColors = colors
            )
        }
    }

    /**
     * Handles selection of a new [SplitAlgorithm].
     * Updates the state with the chosen algorithm and clears previous split results and percentages,
     * as these are algorithm-dependent.
     * @param algorithm The newly selected [SplitAlgorithm].
     */
    private fun handleSelectAlgorithm(algorithm: SplitAlgorithm) {
        _uiState.update { 
            it.copy(
                selectedAlgorithm = algorithm, 
                splitResult = null, // Clear previous monetary results
                participantPercentages = emptyMap(), // Clear previous percentage results
                 // Clear any algorithm-specific errors, e.g., if percentage sum was an issue
                errorMessages = it.errorMessages.filterNot { entry -> entry.key.startsWith(PARTICIPANT_INPUT_ERROR_KEY_PREFIX) }
            ) 
        }
        // TODO: Depending on UI for algorithm-specific inputs (e.g., percentage fields per participant),
        // a call to re-validate or update participant UI attributes might be needed here.
    }

    /** 
     * Handles adding a new participant using data from [NewParticipantData] (e.g., from an "Add Friend" sheet).
     * Ensures the maximum participant limit ([MAX_PARTICIPANTS]) is not exceeded.
     * Updates participant lists and their UI attributes.
     * @param data The [NewParticipantData] containing information for the new participant.
     */
    private fun handleAddNewParticipant(data: NewParticipantData) {
        _uiState.update { currentState ->
            if (currentState.participants.size < MAX_PARTICIPANTS) {
                val newParticipant = Participant(
                    name = data.name.trim(),
                    // Mark as "You" if no other participant is "You" and name matches (case-insensitive)
                    isYou = currentState.participants.none { it.isYou } && data.name.equals("You", ignoreCase = true),
                    avatarSeed = data.name.trim() // Use trimmed name for consistent avatar generation
                )
                val updatedParticipants = currentState.participants + newParticipant
                val (percentages, colors) = generateParticipantUIAttributes(updatedParticipants, currentState.finalTotalToSplit ?: currentState.initialTotalAmount, currentState.splitResult ?: emptyMap())
                currentState.copy(
                    participants = updatedParticipants,
                    participantPercentages = percentages,
                    participantCardColors = colors
                )
            } else {
                sendUiEffect(SplitContract.UiEffect.ShowSnackbar("Maximum $MAX_PARTICIPANTS participants allowed."))
                currentState // Return current state if limit exceeded
            }
        }
    }

    /** 
     * Handles removing a participant by their ID.
     * Ensures the minimum participant count ([MIN_PARTICIPANTS]) is maintained.
     * Updates participant lists and their UI attributes (percentages, colors, split results).
     * @param participantId The ID of the participant to remove.
     */
    private fun handleRemoveParticipant(participantId: String) {
        _uiState.update { currentState ->
            if (currentState.participants.size > MIN_PARTICIPANTS) {
                val updatedParticipants = currentState.participants.filter { it.id != participantId }
                val updatedSplitResult = currentState.splitResult?.minus(participantId) // Remove from monetary results
                // Recalculate UI attributes for the remaining participants
                val (percentages, colors) = generateParticipantUIAttributes(updatedParticipants, currentState.finalTotalToSplit ?: currentState.initialTotalAmount, updatedSplitResult ?: emptyMap())
                currentState.copy(
                    participants = updatedParticipants,
                    splitResult = updatedSplitResult,
                    participantPercentages = percentages,
                    participantCardColors = colors
                )
            } else {
                sendUiEffect(SplitContract.UiEffect.ShowSnackbar("Minimum $MIN_PARTICIPANTS participant required."))
                currentState // Return current state if min limit reached
            }
        }
    }
    
    /** 
     * Handles updates to a specific field of a participant (e.g., toggling `isPaid`, or future inputs like fixed amount).
     * The [updateAction] lambda defines how the participant object should be modified.
     * @param participantId The ID of the participant to update.
     * @param updateAction A lambda function that takes the current [Participant] state and returns the modified state.
     */
    private fun handleUpdateParticipantField(participantId: String, updateAction: (Participant) -> Participant) {
        _uiState.update { currentState ->
            val updatedParticipants = currentState.participants.map {
                if (it.id == participantId) updateAction(it) else it
            }
            // Determine if UI attributes need full recalculation or just color update.
            // For `isPaid` toggle, usually only color might change if it's dependent on `isPaid` status.
            // If participant-specific inputs (fixed amount, percentage) are changed, then percentages may need full recalc.
            // For now, assume a simple color refresh might be needed if `isYou` status changes affecting color selection.
            val (_, colors) = generateParticipantUIAttributes(updatedParticipants, currentState.finalTotalToSplit ?: currentState.initialTotalAmount, currentState.splitResult ?: emptyMap(), false) 

            currentState.copy(
                participants = updatedParticipants,
                participantCardColors = colors // Update colors if the change might affect them
            )
        }
    }

    /** 
     * Clears a specific error message from the UI state by its key.
     * @param errorKey The key of the error message to clear.
     */
    private fun handleClearError(errorKey: String) {
        _uiState.update { it.copy(errorMessages = it.errorMessages.minus(errorKey)) }
    }

    /**
     * Handles the primary action of calculating the split.
     * Validates necessary conditions (e.g., positive total amount, sufficient participants).
     * Calls the [splitUseCase] with the current state (including selected algorithm).
     * Updates the UI state with monetary shares, percentage shares, and card colors upon success,
     * or with error messages upon failure.
     */
    private fun handleCalculateSplit() {
        _uiState.update { it.copy(isLoading = true, errorMessages = it.errorMessages.filterNot { entry -> entry.key == CALCULATION_ERROR_KEY || entry.key == TOTAL_AMOUNT_ERROR_KEY || entry.key == PARTICIPANT_ERROR_KEY }) } 

        val currentState = _uiState.value
        val billAmountForCalc = currentState.finalTotalToSplit ?: currentState.initialTotalAmount

        // Basic validations before calling the use case
        if (billAmountForCalc <= BigDecimal.ZERO) {
            _uiState.update { it.copy(isLoading = false, errorMessages = it.errorMessages + (TOTAL_AMOUNT_ERROR_KEY to "Total amount must be positive.")) }
            return
        }
        if (currentState.participants.isEmpty()) {
             _uiState.update { it.copy(isLoading = false, errorMessages = it.errorMessages + (PARTICIPANT_ERROR_KEY to "Add at least one participant.")) }
            return
        }
        
        // TODO: Add algorithm-specific input validation here if needed (e.g., for Percentage sum, fixed amounts per participant)
        // This was previously in a `validateParticipantFields` helper. If participant inputs (fixedAmount, percentage) are to be edited in UI,
        // that validation logic will be critical here, adding errors to `PARTICIPANT_INPUT_ERROR_KEY_PREFIX + fieldName`.

        try {
            // Call the use case with the current total, participants, and selected algorithm.
            // Assuming tip & tax are included in `billAmountForCalc` or handled by a prior step/screen.
            val result = splitUseCase(
                totalBill = billAmountForCalc, 
                tipAmount = BigDecimal.ZERO, // Placeholder, as tip/tax inputs are not on the new main split UI
                taxAmount = BigDecimal.ZERO,  // Placeholder
                participants = currentState.participants,
                algorithm = currentState.selectedAlgorithm // Use the algorithm from the state
            )
            
            // Generate percentage strings and card colors based on the calculation result.
            val (percentages, colors) = generateParticipantUIAttributes(currentState.participants, billAmountForCalc, result)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    splitResult = result,
                    participantPercentages = percentages,
                    participantCardColors = colors,
                    errorMessages = it.errorMessages.filterNot { entry -> entry.key.startsWith(PARTICIPANT_INPUT_ERROR_KEY_PREFIX) } // Clear participant input errors on success
                )
            }
        } catch (e: IllegalArgumentException) {
            // Handle domain-specific errors from the use case (e.g., invalid split configuration).
            val errorMessage = e.message ?: "A problem occurred during calculation."
            _uiState.update { it.copy(isLoading = false, errorMessages = it.errorMessages + (CALCULATION_ERROR_KEY to errorMessage)) }
            sendUiEffect(SplitContract.UiEffect.ShowSnackbar(errorMessage))
        } catch (e: Exception) {
            // Handle unexpected errors during calculation.
            val errorMessage = "An unexpected error occurred during calculation."
            _uiState.update { it.copy(isLoading = false, errorMessages = it.errorMessages + (CALCULATION_ERROR_KEY to errorMessage)) }
            sendUiEffect(SplitContract.UiEffect.ShowSnackbar(errorMessage))
        }
    }
    
    /**
     * Handles the action to share bill details.
     * Prepares a summary string of the split and (currently) shows it in a Snackbar for demonstration.
     * TODO: This should ideally trigger a system share intent via a UiEffect in a production app.
     */
    private fun handleShareBillAction() {
        val state = _uiState.value
        val billSummary = StringBuilder()
        billSummary.append("Split Bill Summary:\n")
        state.merchantInfo?.let {
            billSummary.append("Merchant: ${it.name} on ${it.date}\n")
        }
        state.finalTotalToSplit?.let {
            billSummary.append("Total: ${it.setScale(2, RoundingMode.HALF_UP).toPlainString()} USD\n") // Format currency for display
        }
        billSummary.append("Participants:\n")
        state.participants.forEach { p ->
            val share = state.splitResult?.get(p.id)?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "N/A"
            val percentage = state.participantPercentages[p.id] ?: "N/A"
            billSummary.append("- ${p.name}: $share USD ($percentage)\n")
        }
        // Send a snackbar with a snippet of the summary (for demo purposes).
        sendUiEffect(SplitContract.UiEffect.ShowSnackbar("Share: ${billSummary.toString().take(150)}..."))
    }


    // --- Helper and Utility Methods ---

    /**
     * Generates percentage strings and card colors for a list of participants based on the split result.
     * @param participants The list of current [Participant]s.
     * @param totalToSplit The total amount that was split.
     * @param splitResult A map of participant ID to their calculated monetary share from the use case.
     * @param calculatePercentages If true (default), percentages are recalculated from `splitResult` and `totalToSplit`.
     *                           If false, existing percentages from the current UI state are preserved where possible.
     * @return A Pair containing two maps: 
     *         1. Participant ID to their calculated percentage string (e.g., "25%").
     *         2. Participant ID to their assigned [Color] for the UI card.
     */
    private fun generateParticipantUIAttributes(
        participants: List<Participant>,
        totalToSplit: BigDecimal,
        splitResult: Map<String, BigDecimal>,
        calculatePercentages: Boolean = true
    ): Pair<Map<String, String>, Map<String, Color>> {
        val percentages = mutableMapOf<String, String>()
        val colors = mutableMapOf<String, Color>()
        val availableColors = cardColorPalette.toMutableList() // Create a mutable copy for removal

        participants.forEachIndexed { index, participant ->
            // Calculate or preserve percentage share string
            if (calculatePercentages) {
                val share = splitResult[participant.id] ?: BigDecimal.ZERO
                val percentageValue = if (totalToSplit > BigDecimal.ZERO && share >= BigDecimal.ZERO) {
                    // Ensure share is not negative, though use case should handle this.
                    share.multiply(BigDecimal(100)).divide(totalToSplit, 0, RoundingMode.HALF_UP) // Round to nearest whole percentage
                } else {
                    BigDecimal.ZERO
                }
                percentages[participant.id] = "${percentageValue.toInt()}%"
            } else {
                // Preserve existing percentage if not recalculating (e.g., for color-only update)
                _uiState.value.participantPercentages[participant.id]?.let {
                    percentages[participant.id] = it
                }
            }
            
            // Assign card color
            // TODO: Enhance color assignment logic. Could be based on MaterialTheme, `isYou` status, or participant hash.
            val colorForParticipant = if (participant.isYou) {
                // Consider a distinct, theme-aligned color for the "You" participant.
                cardColorPalette.firstOrNull() ?: Color.LightGray // Default to first palette color or Gray
            } else {
                // Cycle through `availableColors` or `cardColorPalette` for other participants.
                if (availableColors.size > 1 && participant.isYou) { // Ensure "You" doesn't consume from general pool if it has a unique color logic
                    availableColors.getOrElse(index % availableColors.size) { Color.DarkGray } 
                } else if (availableColors.isNotEmpty()) {
                     availableColors.removeAt(0)
                } else { // Fallback if palette is exhausted (more participants than colors)
                     cardColorPalette.getOrElse(index % cardColorPalette.size) { Color.Gray }
                }
            }
            colors[participant.id] = colorForParticipant
        }
        return Pair(percentages, colors)
    }

    /**
     * Creates a default [Participant] object, typically used when adding participants generically.
     * The first default participant added is marked as "You" if no other participant already is.
     * @param index The index of this participant relative to the current participant list size, used for default naming.
     * @return A new [Participant] instance with default values.
     */
    private fun createDefaultParticipant(index: Int): Participant {
        val isCurrentUser = _uiState.value.participants.none { it.isYou } && index == 0
        val defaultName = if (isCurrentUser) "You" else "Friend ${index + 1}" // Changed from "Participant"
        return Participant(
            name = defaultName,
            isYou = isCurrentUser,
            avatarSeed = defaultName // Use name for consistent avatar generation
        )
    }

    /**
     * Sends a [SplitContract.UiEffect] to the UI layer via the [_uiEffectChannel].
     * This is a helper function to ensure effects are sent from within the [viewModelScope].
     * @param effect The [SplitContract.UiEffect] to be sent.
     */
    private fun sendUiEffect(effect: SplitContract.UiEffect) {
        viewModelScope.launch {
            _uiEffectChannel.send(effect)
        }
    }
}
