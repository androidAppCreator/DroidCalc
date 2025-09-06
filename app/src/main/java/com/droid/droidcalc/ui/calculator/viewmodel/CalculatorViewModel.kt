/**
 * ViewModel for the Calculator screen.
 * This class manages the UI state as defined in [CalculatorContract.State],
 * handles user interactions via [CalculatorContract.Intent], and manages side effects
 * like navigation and Snackbar messages via [NavigationEvent] and [UiEffect].
 * It adheres to MVI architectural principles, focusing on state management and effect handling.
 * Actual complex calculation logic is deferred for this iteration to ensure MVI purity.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.droidcalc.navigation.NavigationEvent
import com.droid.droidcalc.ui.calculator.CalculatorContract
import com.droid.droidcalc.ui.common.UiEffect
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
import timber.log.Timber // Added for logging intents as per original file
import java.math.MathContext

/**
 * ViewModel for the CalculatorScreen, implementing MVI with [CalculatorContract].
 * This version focuses on the MVI flow for UI interactions and side effect handling (navigation, Snackbars),
 * with calculator logic itself being highly simplified or stubbed to prioritize architectural correctness.
 * It uses the consolidated [CalculatorContract.State] and [CalculatorContract.Intent].
 *
 * @property calculateUseCase Placeholder for a future calculation engine. For now, logic is internal.
 * @property historyRepository Placeholder for a future history saving mechanism.
 * @author DroidSwap
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    // private val calculateUseCase: com.droid.droidcalc.domain.usecase.CalculateUseCase, // From original file, keep for future use
    // private val historyRepository: com.droid.droidcalc.domain.repository.HistoryRepository // From original file, keep for future use
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorContract.State())
    val uiState: StateFlow<CalculatorContract.State> = _uiState.asStateFlow()

    private val _navigationEventChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEventChannel.receiveAsFlow() // Exposed as SharedFlow

    private val _uiEffectChannel = Channel<UiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffectChannel.receiveAsFlow() // Exposed as SharedFlow

    // Internal state for building the display string. Not for complex expression evaluation.
    private var currentExpressionBuilder: StringBuilder = StringBuilder()

    /**
     * Processes the given [CalculatorContract.Intent] to update the UI state or trigger side effects.
     * @param intent The [CalculatorContract.Intent] from the UI.
     */
    fun processIntent(intent: CalculatorContract.Intent) {
        Timber.d("Processing intent: %s", intent) // Logging from original file
        viewModelScope.launch { 
            // Clear error state on most new inputs.
            if (intent !is CalculatorContract.Intent.ResultDisplayClicked && 
                intent !is CalculatorContract.Intent.ActionNavigateToSplit &&
                intent !is CalculatorContract.Intent.ActionNavigateToSIP &&
                intent !is CalculatorContract.Intent.DismissResultActions) {
                if (_uiState.value.error != null) {
                    _uiState.update { it.copy(error = null) }
                }
            }

            when (intent) {
                is CalculatorContract.Intent.NumberInput -> handleNumberInput(intent.number)
                is CalculatorContract.Intent.OperatorInput -> handleOperatorInput(intent.operator)
                is CalculatorContract.Intent.DecimalInput -> handleDecimalInput()
                is CalculatorContract.Intent.Calculate -> handleCalculate()
                is CalculatorContract.Intent.Clear -> handleClear()
                is CalculatorContract.Intent.Delete -> handleDelete()
                is CalculatorContract.Intent.ParenthesesInput -> handleParenthesesInput()
                is CalculatorContract.Intent.PercentageInput -> handlePercentageInput()
                is CalculatorContract.Intent.ResultDisplayClicked -> handleResultDisplayClicked()
                is CalculatorContract.Intent.ActionNavigateToSplit -> handleActionNavigateToSplit()
                is CalculatorContract.Intent.ActionNavigateToSIP -> handleActionNavigateToSIP()
                is CalculatorContract.Intent.DismissResultActions -> handleDismissResultActions()
                is CalculatorContract.Intent.NavigationEffectConsumed -> { /* No specific action if Channel handles one-time events */ }
            }
        }
    }

    private fun handleNumberInput(number: Char) {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed || (currentExpressionBuilder.toString() == "0" && number != '0')) {
            currentExpressionBuilder.clear()
            _uiState.update { it.copy(isResultDisplayed = false) }
        }
        if (currentExpressionBuilder.toString() == "0" && number == '0') { // Avoid "00"
             // do nothing
        } else {
            currentExpressionBuilder.append(number)
        }
        _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = currentExpressionBuilder.toString()) }
        // attemptLiveEvaluation(currentExpressionBuilder.toString()) // Keep for future when calc engine is ready
    }

    private fun handleOperatorInput(operator: Char) {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed && currentState.error == null && isActionableNumeric(currentState.displayValue)) {
            currentExpressionBuilder.clear().append(currentState.displayValue)
             _uiState.update { it.copy(isResultDisplayed = false) }
        } else if (currentState.isResultDisplayed) { // Error or non-actionable result
            currentExpressionBuilder.clear().append("0")
             _uiState.update { it.copy(isResultDisplayed = false) }
        }

        if (currentExpressionBuilder.isEmpty() && (operator == '*' || operator == '/')) {
            // Don't start with * or /
        } else if (currentExpressionBuilder.isNotEmpty() && currentExpressionBuilder.last().isOperator()) {
            currentExpressionBuilder.deleteCharAt(currentExpressionBuilder.length - 1)
            currentExpressionBuilder.append(operator)
        } else if (currentExpressionBuilder.isNotEmpty() && currentExpressionBuilder.last() == '.') {
            // Don't append operator after decimal point directly
        } else if (currentExpressionBuilder.isEmpty() && operator == '-'){
            currentExpressionBuilder.append(operator) // Allow negative numbers
        } else if (currentExpressionBuilder.isNotEmpty()){
             currentExpressionBuilder.append(operator)
        }
        _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = currentExpressionBuilder.toString()) }
    }

    private fun handleDecimalInput() {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed) {
            currentExpressionBuilder.clear().append("0.")
            _uiState.update { it.copy(isResultDisplayed = false) }
        } else {
            val lastOperatorIndex = currentExpressionBuilder.lastIndexOfAny(charArrayOf('+', '-', '*', '/', '('))
            val currentNumberSegment = if (lastOperatorIndex != -1) currentExpressionBuilder.substring(lastOperatorIndex + 1) else currentExpressionBuilder.toString()
            if (!currentNumberSegment.contains('.')) {
                if (currentExpressionBuilder.isEmpty() || currentExpressionBuilder.last().isOperator() || currentExpressionBuilder.last() == '(') {
                    currentExpressionBuilder.append("0.")
                } else {
                    currentExpressionBuilder.append('.')
                }
            }
        }
        _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = currentExpressionBuilder.toString()) }
    }

    private fun handleCalculate() {
        if (currentExpressionBuilder.isEmpty()) {
            _uiState.update { it.copy(displayValue = "0", expression = "", isResultDisplayed = true, error = null) }
            return
        }
        // Placeholder for actual calculation logic using a UseCase or an evaluation engine
        // For now, simulate a result or an error.
        try {
            // This is a VERY basic and unsafe evaluation, for placeholder ONLY
            // A proper solution requires parsing and handling operator precedence, parentheses etc.
            // Example: Using a library like "ExprEval" or a custom shunting-yard algorithm.
            // For now, let's assume the expression is simple or just use a fixed value.
            val simpleExpression = currentExpressionBuilder.toString()
            if (simpleExpression.contains("/") && simpleExpression.endsWith("0")) { // Extremely naive div by zero
                 throw ArithmeticException("Division by zero (simulated)")
            }
            // Simulate a result based on the last number if possible for demo purposes
            val parts = simpleExpression.split(Regex("[+\\-*/()]")) // Note: `-` must be escaped inside `[]`
            val simulatedResult = parts.lastOrNull { it.isNotBlank() }?.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal("123.45")
            
            _uiState.update { state ->
                state.copy(
                    displayValue = formatResult(simulatedResult), 
                    isResultDisplayed = true, 
                    error = null,
                    actionableNumericResult = formatResult(simulatedResult) // Store actionable result
                )
            }
            currentExpressionBuilder.clear().append(formatResult(simulatedResult)) // For chaining
        } catch (e: Exception) {
            _uiState.update { it.copy(displayValue = "Error", isResultDisplayed = true, error = e.message ?: "Calculation Error") }
        }
    }

    private fun handleClear() { // Corresponds to AC (All Clear) in typical calculators
        currentExpressionBuilder.clear()
        _uiState.update { CalculatorContract.State() } // Reset to initial default state
    }

    private fun handleDelete() { // Backspace
        if (!_uiState.value.isResultDisplayed && currentExpressionBuilder.isNotEmpty()) {
            currentExpressionBuilder.deleteCharAt(currentExpressionBuilder.length - 1)
            _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = if(currentExpressionBuilder.isEmpty()) "0" else currentExpressionBuilder.toString()) }
        } else if (_uiState.value.isResultDisplayed) {
             // If result is shown, DEL could clear the result and expression, similar to Clear
            handleClear()
        }
    }
    
    private fun handleParenthesesInput() {
        // Basic logic: add opening if sensible, or closing if there's an unmatched opening one.
        // This is a placeholder and needs a more robust logic for proper parenthesis matching.
        val openParenCount = currentExpressionBuilder.count { it == '(' }
        val closeParenCount = currentExpressionBuilder.count { it == ')' }

        if (currentExpressionBuilder.isEmpty() || currentExpressionBuilder.last().isOperator() || currentExpressionBuilder.last() == '(') {
            currentExpressionBuilder.append('(')
        } else if (currentExpressionBuilder.last().isDigit() && openParenCount > closeParenCount) {
            currentExpressionBuilder.append(')')
        } else if (currentExpressionBuilder.last().isDigit()) {
             // Potentially add '*' before '('
             // currentExpressionBuilder.append("*").append('(')
             _uiEffectChannel.trySend(UiEffect.ShowSnackbar("Tap operator before parenthesis"))
             return // Or simply append '('
        } else {
             currentExpressionBuilder.append('(')
        }
         _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = currentExpressionBuilder.toString()) }
    }

    private fun handlePercentageInput() {
        // This is a simplified version. A full implementation would consider context.
        // E.g., is it X% of Y, or just X converting to 0.0X?
        // For now, treat last number as N and convert to N/100.
        if (currentExpressionBuilder.isNotEmpty()) {
            try {
                // Try to extract the last number segment
                var lastNumStr = ""
                for (i in currentExpressionBuilder.length - 1 downTo 0) {
                    val char = currentExpressionBuilder[i]
                    if (char.isDigit() || char == '.') {
                        lastNumStr = char + lastNumStr
                    } else {
                        break
                    }
                }
                if (lastNumStr.isNotEmpty()) {
                    val num = BigDecimal(lastNumStr)
                    val percentageValue = num.divide(BigDecimal(100), MathContext.DECIMAL64)
                    currentExpressionBuilder.setLength(currentExpressionBuilder.length - lastNumStr.length)
                    currentExpressionBuilder.append(formatResult(percentageValue))
                    _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = currentExpressionBuilder.toString(), isResultDisplayed = false) }
                } else {
                     _uiEffectChannel.trySend(UiEffect.ShowSnackbar("Enter a number first."))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(displayValue = "Error", error = "Invalid percentage operation", isResultDisplayed = true) }
            }
        }
    }

    private fun handleResultDisplayClicked() {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed && currentState.error == null && isActionableNumeric(currentState.displayValue)) {
            _uiState.update {
                it.copy(
                    showResultActionBottomSheet = true,
                    actionableNumericResult = currentState.displayValue // Set the value that was clicked
                )
            }
        } else {
            Timber.d("Result display clicked, but not actionable or error present.")
             _uiEffectChannel.trySend(UiEffect.ShowSnackbar("No actions for current display."))
        }
    }

    private fun handleActionNavigateToSplit() {
        viewModelScope.launch {
            val resultToPass = _uiState.value.actionableNumericResult ?: _uiState.value.displayValue
            if (isActionableNumeric(resultToPass)) {
                val routeValue = BigDecimal(resultToPass).stripTrailingZeros().toPlainString()
                _navigationEventChannel.send(NavigationEvent.NavigateToRoute("split/$routeValue"))
                handleDismissResultActions() // Dismiss sheet after initiating navigation
            } else {
                _uiEffectChannel.send(UiEffect.ShowSnackbar("Cannot use current value for Split."))
                handleDismissResultActions()
            }
        }
    }

    private fun handleActionNavigateToSIP() {
        viewModelScope.launch {
            val resultToPass = _uiState.value.actionableNumericResult ?: _uiState.value.displayValue
            if (isActionableNumeric(resultToPass)) {
                 val routeValue = BigDecimal(resultToPass).stripTrailingZeros().toPlainString()
                _navigationEventChannel.send(NavigationEvent.NavigateToRoute("sip/$routeValue"))
                handleDismissResultActions() // Dismiss sheet after initiating navigation
            } else {
                _uiEffectChannel.send(UiEffect.ShowSnackbar("Cannot use current value for SIP."))
                handleDismissResultActions()
            }
        }
    }

    private fun handleDismissResultActions() {
        _uiState.update { it.copy(showResultActionBottomSheet = false, actionableNumericResult = null) }
    }

    private fun isActionableNumeric(value: String): Boolean {
        if (value.equals("Error", ignoreCase = true)) return false
        return try {
            BigDecimal(value)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    private fun Char.isOperator(): Boolean = this in listOf('+', '-', '*', '/')

    private fun formatResult(result: BigDecimal): String {
        return if (result.stripTrailingZeros().scale() <= 0) {
            result.toBigInteger().toString()
        } else {
            result.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
    }
}
