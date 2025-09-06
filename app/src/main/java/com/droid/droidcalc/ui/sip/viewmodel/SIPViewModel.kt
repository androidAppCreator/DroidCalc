/**
 * ViewModel for the SIP (Systematic Investment Plan) Calculator screen.
 * This class manages the UI state as defined in [SIPContract.SIPUiState],
 * handles user interactions via [SIPContract.SIPUiIntent], produces [SIPContract.UiEffect] for side effects,
 * and orchestrates SIP calculations using the [SIPUseCase]. It adheres to MVI architectural principles.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.sip.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.droidcalc.domain.usecase.SIPUseCase
import com.droid.droidcalc.ui.sip.SIPContract
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

/**
 * ViewModel for the SIPScreen, implementing MVI with [SIPContract].
 * It manages UI state ([SIPContract.SIPUiState]), processes user actions ([SIPContract.SIPUiIntent]),
 * and emits one-time side effects ([SIPContract.UiEffect]) like Snackbar messages.
 *
 * @param savedStateHandle Handle to access navigation arguments (e.g., initial investment from CalculatorScreen).
 * @param sipUseCase The use case for performing SIP calculations.
 * @author DroidSwap
 */
@HiltViewModel
class SIPViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sipUseCase: SIPUseCase
    // TODO: Inject Repository for Firestore scenario saving when implemented
) : ViewModel() {

    private val _uiState = MutableStateFlow(SIPContract.SIPUiState())
    val uiState: StateFlow<SIPContract.SIPUiState> = _uiState.asStateFlow()

    private val _uiEffectChannel = Channel<SIPContract.UiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val initialAmountString = savedStateHandle.get<String>("initialAmount") // Key from AppNavGraph
            if (initialAmountString != null) {
                val parsedAmount = initialAmountString.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
                if (parsedAmount != null) {
                    _uiState.update { it.copy(initialInvestmentInput = parsedAmount.toPlainString()) }
                } else if (initialAmountString.isNotBlank()){
                    // Invalid amount from nav args: show Snackbar and inline error
                    val errorMessage = "Invalid initial amount from previous screen. Please verify."
                    _uiEffectChannel.send(SIPContract.UiEffect.ShowSnackbar(errorMessage))
                    _uiState.update { it.copy(
                        initialInvestmentInput = initialAmountString, 
                        errorMessages = it.errorMessages + ("initialInvestment" to "Invalid amount.")
                    )}
                }
            }
        }
    }

    /**
     * Processes the given [SIPContract.SIPUiIntent] to update the UI state or trigger actions/effects.
     * This is the sole entry point for all user interactions from the UI.
     *
     * @param intent The [SIPContract.SIPUiIntent] representing the user's action.
     * @author DroidSwap
     */
    fun processIntent(intent: SIPContract.SIPUiIntent) {
        // Launching a new coroutine for each intent to ensure sequential processing if needed, 
        // though most handlers are quick state updates.
        viewModelScope.launch {
            when (intent) {
                is SIPContract.SIPUiIntent.UpdateInitialInvestment -> handleUpdateInitialInvestment(intent.amount)
                is SIPContract.SIPUiIntent.UpdateMonthlyContribution -> handleUpdateMonthlyContribution(intent.amount)
                is SIPContract.SIPUiIntent.UpdateAnnualRate -> handleUpdateAnnualRate(intent.rate)
                is SIPContract.SIPUiIntent.UpdateDuration -> handleUpdateDuration(intent.value)
                is SIPContract.SIPUiIntent.ToggleDurationMode -> handleToggleDurationMode(intent.mode)
                is SIPContract.SIPUiIntent.UpdateTargetGoal -> handleUpdateTargetGoal(intent.amount)
                is SIPContract.SIPUiIntent.ToggleAnnuityDue -> handleToggleAnnuityDue(intent.isDue)
                is SIPContract.SIPUiIntent.CalculateSIP -> handleCalculateSIP()
                is SIPContract.SIPUiIntent.ResetInputs -> handleResetInputs()
                is SIPContract.SIPUiIntent.SaveScenario -> { /* TODO: Implement Save Scenario. Send UiEffect for confirmation/error. */ }
                is SIPContract.SIPUiIntent.ClearError -> handleClearError(intent.errorKey)
                is SIPContract.SIPUiIntent.ShowFinancialTip -> { /* TODO: Implement Show Tip. Manage via UiState or UiEffect. */ }
                is SIPContract.SIPUiIntent.DismissFinancialTip -> { /* TODO: Implement Dismiss Tip. */ }
            }
        }
    }

    private fun handleUpdateInitialInvestment(amount: String) {
        _uiState.update { it.copy(initialInvestmentInput = amount, errorMessages = it.errorMessages.minus("initialInvestment")) }
    }

    private fun handleUpdateMonthlyContribution(amount: String) {
        _uiState.update { it.copy(monthlyContributionInput = amount, errorMessages = it.errorMessages.minus("monthlyContribution")) }
    }

    private fun handleUpdateAnnualRate(rate: String) {
        _uiState.update { it.copy(annualRateInput = rate, errorMessages = it.errorMessages.minus("annualRate")) }
    }

    private fun handleUpdateDuration(value: String) {
        _uiState.update { it.copy(durationInput = value, errorMessages = it.errorMessages.minus("duration")) }
    }

    private fun handleToggleDurationMode(mode: SIPContract.DurationMode) {
        _uiState.update { it.copy(durationMode = mode, durationInput = "", errorMessages = it.errorMessages.minus("duration")) }
    }

    private fun handleUpdateTargetGoal(amount: String) {
        _uiState.update { it.copy(targetGoalInput = amount, errorMessages = it.errorMessages.minus("targetGoal")) }
    }

    private fun handleToggleAnnuityDue(isDue: Boolean) {
        _uiState.update { it.copy(isAnnuityDue = isDue, calculationResult = null) } // Clear result as it depends on this
    }

    private fun handleClearError(errorKey: String) {
        _uiState.update { it.copy(errorMessages = it.errorMessages.minus(errorKey)) }
    }

    private fun handleResetInputs() {
        _uiState.update {
            SIPContract.SIPUiState(
                initialInvestmentInput = if (savedStateHandle.get<String>("initialAmount") != null) it.initialInvestmentInput else "",
                errorMessages = emptyMap(),
                calculationResult = null,
                isChartVisible = false
                // Reset other fields to default as per SIPUiState definition
            )
        }
        // Consider sending a UiEffect if a confirmation Snackbar for reset is desired.
    }

    private suspend fun handleCalculateSIP() { // Made suspend to use _uiEffectChannel.send
        _uiState.update { it.copy(isLoading = true, calculationResult = null, errorMessages = emptyMap()) }

        val currentState = _uiState.value
        val currentErrors = mutableMapOf<String, String>()

        val initialInvestment = currentState.initialInvestmentInput.toBigDecimalOrNull()
        val monthlyContribution = currentState.monthlyContributionInput.toBigDecimalOrNull()
        val annualRatePercent = currentState.annualRateInput.toBigDecimalOrNull()
        val durationValue = currentState.durationInput.toIntOrNull()
        val targetGoal = currentState.targetGoalInput.toBigDecimalOrNull()

        // --- Input Validation ---
        if (initialInvestment == null || initialInvestment < BigDecimal.ZERO) {
            if (currentState.initialInvestmentInput.isNotBlank()) currentErrors["initialInvestment"] = "Invalid initial investment."
        }
        if (monthlyContribution == null || monthlyContribution < BigDecimal.ZERO) {
            if (currentState.monthlyContributionInput.isNotBlank()) currentErrors["monthlyContribution"] = "Invalid monthly contribution."
        }
        if (annualRatePercent == null || annualRatePercent <= BigDecimal.ZERO || annualRatePercent > BigDecimal(100)) {
            if (currentState.annualRateInput.isNotBlank()) currentErrors["annualRate"] = "Rate must be positive & realistic (<=100%)."
        }
        if (durationValue == null || durationValue <= 0) {
            if (currentState.durationInput.isNotBlank()) currentErrors["duration"] = "Duration must be a positive number."
        }
        if (targetGoal != null && targetGoal <= BigDecimal.ZERO) {
            if (currentState.targetGoalInput.isNotBlank()) currentErrors["targetGoal"] = "Target goal must be positive."
        }

        if (currentErrors.isNotEmpty()) {
            _uiState.update { it.copy(isLoading = false, errorMessages = currentErrors) }
            return
        }

        val finalInitialInvestment = initialInvestment ?: BigDecimal.ZERO
        val finalMonthlyContribution = monthlyContribution ?: BigDecimal.ZERO
        val finalAnnualRate = annualRatePercent!! 
        var finalPeriodsInMonths = durationValue!! 
        if (currentState.durationMode == SIPContract.DurationMode.YEARS) {
            finalPeriodsInMonths *= 12
        }

        try {
            val result = sipUseCase(
                initialInvestment = finalInitialInvestment.setScale(2, RoundingMode.HALF_UP),
                monthlyContribution = finalMonthlyContribution.setScale(2, RoundingMode.HALF_UP),
                annualRatePercentage = finalAnnualRate,
                periodsInMonths = finalPeriodsInMonths,
                targetFutureValue = targetGoal?.setScale(2, RoundingMode.HALF_UP)
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    calculationResult = result,
                    isChartVisible = true,
                    errorMessages = emptyMap() // Clear previous errors on success
                )
            }
        } catch (e: IllegalArgumentException) {
            val errorMessage = e.message ?: "Invalid input for SIP calculation."
            _uiEffectChannel.send(SIPContract.UiEffect.ShowSnackbar(errorMessage))
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessages = mapOf("calculation" to errorMessage),
                    isChartVisible = false
                )
            }
        } catch (e: Exception) {
            val errorMessage = "An unexpected error occurred during calculation."
            _uiEffectChannel.send(SIPContract.UiEffect.ShowSnackbar(errorMessage))
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessages = mapOf("calculation" to errorMessage),
                    isChartVisible = false
                )
            }
        }
    }
}

/** 
 * Helper extension to parse a String to BigDecimal, returning null if blank or invalid format.
 * @return BigDecimal representation of the string, or null.
 * @author DroidSwap
 */
private fun String.toBigDecimalOrNull(): BigDecimal? {
    return if (this.isBlank()) null else try { BigDecimal(this.trim()) } catch (e: NumberFormatException) { null }
}
