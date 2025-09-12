/**
 * ViewModel for the Calculator screen.
 * This class manages the UI state as defined in [CalculatorContract.State],
 * handles user interactions via [CalculatorContract.Intent], and manages side effects
 * like navigation and Snackbar messages via [NavigationEvent] and [UiEffect].
 * It adheres to MVI architectural principles, focusing on state management and effect handling.
 *
 * It now uses a [CalculationResultHolder] to pass the calculation result to the Split screen,
 * instead of relying on route arguments or direct ViewModel-to-ViewModel injection.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.droidcalc.navigation.CalculationResultHolder
import com.droid.droidcalc.navigation.NavigationEvent
import com.droid.droidcalc.navigation.Screen
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
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject
import timber.log.Timber

/**
 * ViewModel for the CalculatorScreen, implementing MVI with [CalculatorContract].
 * Handles UI interactions and state management for the calculator.
 *
 * When a calculation result is clicked, it sets the result in [CalculationResultHolder]
 * and navigates to the Split screen without using route arguments.
 *
 * @property calculationResultHolder A Hilt-injected service to hold and share the calculation result.
 * @author DroidSwap
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculationResultHolder: CalculationResultHolder // Injected via Hilt
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorContract.State())
    val uiState: StateFlow<CalculatorContract.State> = _uiState.asStateFlow()

    private val _navigationEventChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEventChannel.receiveAsFlow()

    private val _uiEffectChannel = Channel<UiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffectChannel.receiveAsFlow()

    private var currentExpressionBuilder: StringBuilder = StringBuilder()

    /**
     * Processes incoming [CalculatorContract.Intent]s from the UI.
     * @param intent The user action or event.
     */
    fun processIntent(intent: CalculatorContract.Intent) {
        Timber.d("Processing intent: %s", intent)
        viewModelScope.launch {
            if (intent !is CalculatorContract.Intent.ResultDisplayClicked &&
                intent !is CalculatorContract.Intent.ActionNavigateToSplit && // Kept for clarity, though deprecated
                intent !is CalculatorContract.Intent.DismissResultActions // Kept for clarity, though deprecated
            ) {
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
                is CalculatorContract.Intent.ActionNavigateToSplit -> { /* Deprecated action */ }
                is CalculatorContract.Intent.ActionNavigateToSIP -> { /* SIP route removed, deprecated action */ }
                is CalculatorContract.Intent.DismissResultActions -> { /* Bottom sheet removed, deprecated action */ }
                is CalculatorContract.Intent.NavigationEffectConsumed -> { /* No specific action, event consumed by UI */ }
            }
        }
    }

    private fun handleNumberInput(number: Char) {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed || (currentExpressionBuilder.toString() == "0" && number != '0')) {
            currentExpressionBuilder.clear()
            _uiState.update { it.copy(isResultDisplayed = false, liveEvaluation = "") }
        }

        if (currentExpressionBuilder.toString() == "0" && number == '0') {
            // Prevent multiple leading zeros if expression is already "0"
        } else {
            currentExpressionBuilder.append(number)
        }
        val currentInput = currentExpressionBuilder.toString()
        _uiState.update { it.copy(expression = currentInput, displayValue = currentInput) }
        attemptLiveEvaluation()
    }

    private fun handleOperatorInput(operator: Char) {
        val currentState = _uiState.value
        // If a result is displayed and it's a valid number, use it as the start of the new expression.
        if (currentState.isResultDisplayed && currentState.error == null && isActionableNumeric(currentState.displayValue)) {
            currentExpressionBuilder.clear().append(currentState.displayValue)
            _uiState.update { it.copy(isResultDisplayed = false, liveEvaluation = "") }
        } else if (currentState.isResultDisplayed) {
            // If result is displayed but it was an error or not actionable, start fresh with "0".
            currentExpressionBuilder.clear().append("0")
            _uiState.update { it.copy(isResultDisplayed = false, liveEvaluation = "") }
        }

        when {
            currentExpressionBuilder.isEmpty() && (operator == '*' || operator == '/') -> { /* Disallow leading * or / */ }
            currentExpressionBuilder.isNotEmpty() && currentExpressionBuilder.last().isOperator() -> {
                // Replace the last operator if a new one is entered consecutively.
                currentExpressionBuilder.deleteCharAt(currentExpressionBuilder.length - 1)
                currentExpressionBuilder.append(operator)
            }
            currentExpressionBuilder.isNotEmpty() && currentExpressionBuilder.last() == '.' -> { /* Disallow operator immediately after a decimal point */ }
            currentExpressionBuilder.isEmpty() && operator == '-' -> currentExpressionBuilder.append(operator) // Allow leading minus
            currentExpressionBuilder.isNotEmpty() -> currentExpressionBuilder.append(operator)
            // Allow leading plus (often implicit but can be explicit), or leading minus (already handled)
            currentExpressionBuilder.isEmpty() && (operator == '+') -> currentExpressionBuilder.append(operator)
        }
        val currentInput = currentExpressionBuilder.toString()
        _uiState.update { it.copy(expression = currentInput, displayValue = currentInput, liveEvaluation = "") } // Clear live eval when operator is added
        attemptLiveEvaluation() // Re-attempt live evaluation
    }

    private fun handleDecimalInput() {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed) {
            // If a result is shown, start a new number with "0."
            currentExpressionBuilder.clear().append("0.")
            _uiState.update { it.copy(isResultDisplayed = false, liveEvaluation = "") }
        } else {
            // Find the start of the current number segment to check for existing decimal.
            val lastOperatorIndex = currentExpressionBuilder.lastIndexOfAny(charArrayOf('+', '-', '*', '/','('))
            val currentNumberSegment = if (lastOperatorIndex != -1) {
                currentExpressionBuilder.substring(lastOperatorIndex + 1)
            } else {
                currentExpressionBuilder.toString()
            }
            // Add decimal point only if the current number segment doesn't already have one.
            if (!currentNumberSegment.contains('.')) {
                if (currentExpressionBuilder.isEmpty() || currentExpressionBuilder.last().isOperator() || currentExpressionBuilder.last() == '(') {
                    // If expression is empty or ends with an operator/open-paren, start with "0."
                    currentExpressionBuilder.append("0.")
                } else {
                    currentExpressionBuilder.append('.')
                }
            }
        }
        val currentInput = currentExpressionBuilder.toString()
        _uiState.update { it.copy(expression = currentInput, displayValue = currentInput, liveEvaluation = "") } // Clear live eval on decimal input
    }

    private fun attemptLiveEvaluation() {
        val currentExpr = currentExpressionBuilder.toString()
        // Avoid live evaluation for empty, operator-ending, or decimal-ending expressions.
        if (currentExpr.isEmpty() || currentExpr.lastOrNull()?.isOperator() == true || currentExpr.endsWith(".")) {
            _uiState.update { it.copy(liveEvaluation = "") }
            return
        }
        try {
             // Regex to check if the expression ends with a digit or is a valid number pattern for evaluation.
             if (Regex(".*\\d$").matches(currentExpr) || currentExpr.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                val liveResult = evaluateExpression(currentExpr)
                _uiState.update { it.copy(liveEvaluation = formatResult(liveResult)) }
            } else {
                // If not a pattern that typically yields a result (e.g. "2+"), clear live eval.
                _uiState.update { it.copy(liveEvaluation = "") }
            }
        } catch (e: Exception) {
            // Silently ignore exceptions during live evaluation as it's for preview purposes.
            _uiState.update { it.copy(liveEvaluation = "") }
        }
    }

    private fun handleCalculate() {
        if (currentExpressionBuilder.isEmpty()) {
            _uiState.update { it.copy(displayValue = "0", expression = "", liveEvaluation = "", isResultDisplayed = true, error = null) }
            return
        }
        val expressionToEvaluate = currentExpressionBuilder.toString()
        Timber.d("Attempting to calculate expression: %s", expressionToEvaluate)
        try {
            val result = evaluateExpression(expressionToEvaluate)
            val formattedResult = formatResult(result)
            Timber.d("Calculation result: %s, Formatted: %s", result, formattedResult)
            _uiState.update {
                it.copy(
                    displayValue = formattedResult,
                    liveEvaluation = "", // Clear live evaluation on final calculation
                    isResultDisplayed = true,
                    error = null,
                    actionableNumericResult = formattedResult // Store for potential actions like navigation
                )
            }
            // Set the current expression to the result for chained calculations.
            currentExpressionBuilder.clear().append(formattedResult)
        } catch (e: ArithmeticException) {
            Timber.e(e, "Calculation error for expression: %s", expressionToEvaluate)
            _uiState.update { it.copy(displayValue = "Error", liveEvaluation = "", isResultDisplayed = true, error = e.message ?: "Division by zero") }
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid expression: %s", expressionToEvaluate)
            _uiState.update { it.copy(displayValue = "Error", liveEvaluation = "", isResultDisplayed = true, error = e.message ?: "Invalid expression") }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected calculation error for expression: %s", expressionToEvaluate)
            _uiState.update { it.copy(displayValue = "Error", liveEvaluation = "", isResultDisplayed = true, error = "Calculation Error") }
        }
    }

    private fun evaluateExpression(expression: String): BigDecimal {
        fun getTokens(expr: String): List<Any> {
            val tokens = mutableListOf<Any>()
            var i = 0
            while (i < expr.length) {
                val char = expr[i]
                when {
                    char.isDigit() || (char == '.') || 
                    (char == '-' && (tokens.isEmpty() || tokens.last().isOperator() || tokens.last() == '(' )) -> {
                        val numStr = StringBuilder()
                        // Handle unary minus
                        if (char == '-') {
                            numStr.append(char)
                            i++
                            // Ensure something follows the unary minus
                            if (i >= expr.length || (!expr[i].isDigit() && expr[i] != '.')) {
                                throw IllegalArgumentException("Invalid unary minus: '$expr'")
                            }
                        }
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                            numStr.append(expr[i])
                            i++
                        }
                        try {
                            tokens.add(BigDecimal(numStr.toString()))
                        } catch (e: NumberFormatException) {
                            throw IllegalArgumentException("Invalid number format: '$numStr' in '$expr'", e)
                        }
                        continue // Token processed, continue to next part of expression
                    }
                    char.isOperator() || char == '(' || char == ')' -> tokens.add(char)
                    char.isWhitespace() -> { /* Ignore whitespace */ }
                    else -> throw IllegalArgumentException("Invalid character '$char' in expression '$expr'")
                }
                i++
            }
            if (tokens.isNotEmpty() && tokens.last().isOperator()) {
                 // Allow expression like "5*-" if followed by a number implicitly handled by parser
                 // but not if it's the absolute end.
                 if (tokens.last() != ')' && tokens.last() != '(') {
                    // A more sophisticated parser would handle operator precedence and unary minus better
                    // For this basic version, ending with a binary operator is an error.
                 }
            }
            return tokens
        }

        val tokens = getTokens(expression)
        if (tokens.isEmpty()) return BigDecimal.ZERO
        if (tokens.size == 1 && tokens.first() is BigDecimal) return tokens.first() as BigDecimal
        // Basic Shunting-yard would be better here, this is a simplified precedence.

        // Pass 1: Multiplication and Division
        val mulDivPass = mutableListOf<Any>()
        var j = 0
        while (j < tokens.size) {
            val token = tokens[j]
            if (token is Char && (token == '*' || token == '/')) {
                if (mulDivPass.isEmpty() || mulDivPass.last() !is BigDecimal) throw IllegalArgumentException("Missing operand for $token in $expression")
                val left = mulDivPass.removeAt(mulDivPass.size - 1) as BigDecimal
                j++
                if (j >= tokens.size || tokens[j] !is BigDecimal) throw IllegalArgumentException("Missing operand for $token in $expression")
                val right = tokens[j] as BigDecimal
                mulDivPass.add(
                    if (token == '*') left.multiply(right)
                    else {
                        if (right.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                        left.divide(right, MathContext.DECIMAL64) // Consider precision
                    }
                )
            } else {
                mulDivPass.add(token)
            }
            j++
        }

        // Pass 2: Addition and Subtraction
        if (mulDivPass.isEmpty()) return BigDecimal.ZERO // Should not happen if tokens was not empty
        var result: BigDecimal
        var currentIdx = 0
        // Handle potential leading unary minus if not caught by getTokens specific logic for it
        if (mulDivPass.first() is Char && mulDivPass.first() as Char == '-' && mulDivPass.size > 1 && mulDivPass[1] is BigDecimal) {
            result = (mulDivPass[1] as BigDecimal).negate()
            currentIdx = 2
        } else if (mulDivPass.first() is BigDecimal) {
            result = mulDivPass.first() as BigDecimal
            currentIdx = 1
        } else {
            throw IllegalArgumentException("Expression must start with a number or valid unary minus: '$expression'")
        }

        while (currentIdx < mulDivPass.size) {
            val operator = mulDivPass[currentIdx] as? Char 
                ?: throw IllegalArgumentException("Expected operator, found ${mulDivPass[currentIdx]} in $expression at index $currentIdx")
            currentIdx++
            if (currentIdx >= mulDivPass.size || mulDivPass[currentIdx] !is BigDecimal) 
                throw IllegalArgumentException("Missing operand for $operator in $expression")
            val rightOperand = mulDivPass[currentIdx] as BigDecimal
            result = if (operator == '+') result.add(rightOperand) else result.subtract(rightOperand)
            currentIdx++
        }
        return result
    }

    private fun handleClear() {
        currentExpressionBuilder.clear()
        _uiState.update { CalculatorContract.State() } // Reset to initial default state
    }

    private fun handleDelete() {
        val currentState = _uiState.value
        if (!currentState.isResultDisplayed && currentExpressionBuilder.isNotEmpty()) {
            currentExpressionBuilder.deleteCharAt(currentExpressionBuilder.length - 1)
            val currentInput = currentExpressionBuilder.toString()
            _uiState.update { it.copy(expression = currentInput, displayValue = if (currentInput.isEmpty()) "0" else currentInput) }
            attemptLiveEvaluation()
        } else if (currentState.isResultDisplayed) {
            // If a result is displayed, pressing delete should clear the state, like pressing 'C'.
            handleClear()
        }
    }

    private fun handleParenthesesInput() {
        viewModelScope.launch { _uiEffectChannel.send(UiEffect.ShowSnackbar("Parentheses are not yet supported.")) }
        // Basic append logic for demo, real implementation needs parsing adjustment
        // val openParenCount = currentExpressionBuilder.count { it == '(' }
        // val closeParenCount = currentExpressionBuilder.count { it == ')' }
        // if (currentExpressionBuilder.isEmpty() || currentExpressionBuilder.last().isOperator() || currentExpressionBuilder.last() == '(') {
        //     currentExpressionBuilder.append('(')
        // } else if (currentExpressionBuilder.isNotEmpty() && (currentExpressionBuilder.last().isDigit() || currentExpressionBuilder.last() == ')') && openParenCount > closeParenCount) {
        //     currentExpressionBuilder.append(')')
        // } else {
        //      // Potentially add multiplication if a number precedes '('
        //      // currentExpressionBuilder.append('*') 
        //     currentExpressionBuilder.append('(')
        // }
        // val currentInput = currentExpressionBuilder.toString()
        //_uiState.update { it.copy(expression = currentInput, displayValue = currentInput, liveEvaluation = "") }
    }

    private fun handlePercentageInput() {
        viewModelScope.launch { _uiEffectChannel.send(UiEffect.ShowSnackbar("Percentage is not yet fully implemented.")) }
        // This requires careful consideration of context (e.g., percentage of what?).
        // A simple approach might be to take the current number and divide by 100.
        // if (currentExpressionBuilder.isNotEmpty()) {
        //    try {
        //        val currentNumber = BigDecimal(currentExpressionBuilder.toString()) // This is too naive, needs parsing of last number
        //        val percentageValue = currentNumber.divide(BigDecimal(100), MathContext.DECIMAL64)
        //        currentExpressionBuilder.clear().append(formatResult(percentageValue))
        //        _uiState.update { it.copy(expression = currentExpressionBuilder.toString(), displayValue = currentExpressionBuilder.toString()) }
        //        attemptLiveEvaluation()
        //    } catch (e: Exception) {
        //        Timber.e(e, "Error calculating percentage")
        //       viewModelScope.launch { _uiEffectChannel.send(UiEffect.ShowSnackbar("Error applying percentage.")) }
        //    }
        // }
    }

    /**
     * Handles click on the result display area.
     * If the current display shows an actionable numeric result (and no error),
     * it sets the result in [CalculationResultHolder] and navigates to the Split screen.
     */
    private fun handleResultDisplayClicked() {
        val currentState = _uiState.value
        if (currentState.isResultDisplayed && currentState.error == null) {
            val resultString = currentState.actionableNumericResult ?: currentState.displayValue
            if (isActionableNumeric(resultString)) {
                try {
                    val resultToPass = BigDecimal(resultString)
                    calculationResultHolder.setCalculatorResult(resultToPass) // Set result in Shared Holder
                    viewModelScope.launch {
                        _navigationEventChannel.send(
                            NavigationEvent.NavigateToRoute(
                                route = Screen.SplitCalculator.route, // Navigate to base route for Split screen
                                popUpToRoute = Screen.Calculator.route,
                                inclusive = false, // Keep Calculator screen in back stack
                                isLaunchSingleTop = true
                            )
                        )
                        // Clear actionable result from state as it's now handled by the shared holder
                        _uiState.update { it.copy(actionableNumericResult = null) }
                    }
                } catch (e: NumberFormatException) {
                    Timber.e(e, "Error converting result '$resultString' to BigDecimal.")
                    viewModelScope.launch { _uiEffectChannel.send(UiEffect.ShowSnackbar("Cannot use invalid result: $resultString")) }
                }
            } else {
                 Timber.d("Result display clicked, but result '$resultString' is not actionable numeric.")
                viewModelScope.launch { _uiEffectChannel.send(UiEffect.ShowSnackbar("No valid numeric result to use.")) }
            }
        } else {
            Timber.d("Result display clicked, but not a final result or an error is present.")
            viewModelScope.launch {
                if (currentState.error != null) {
                    _uiEffectChannel.send(UiEffect.ShowSnackbar("Cannot use error result."))
                } else if (!currentState.isResultDisplayed) {
                    _uiEffectChannel.send(UiEffect.ShowSnackbar("Calculate a result first."))
                }
            }
        }
    }

    /**
     * Checks if a string value represents an actionable numeric value (not "Error" and parseable to BigDecimal).
     * @param value The string value to check.
     * @return True if actionable numeric, false otherwise.
     */
    private fun isActionableNumeric(value: String): Boolean {
        if (value.equals("Error", ignoreCase = true) || value.isBlank()) return false
        return try {
            BigDecimal(value) // Check if it can be parsed to BigDecimal
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Extension function to check if a character is a supported operator.
     * @return True if the character is one of '+', '-', '*', '/'.
     */
    private fun Char.isOperator(): Boolean = this in setOf('+', '-', '*', '/')

    /**
     * Extension function to check if an [Any] type is a supported operator character.
     * @return True if the object is a Char and one of '+', '-', '*', '/'.
     */
    private fun Any?.isOperator(): Boolean = this is Char && this.isOperator()

    /**
     * Formats a [BigDecimal] result for display.
     * Removes trailing zeros and uses plain string representation.
     * Limits scale to 8 decimal places with half-up rounding if necessary.
     *
     * @param result The [BigDecimal] to format.
     * @return A string representation of the formatted result.
     */
    private fun formatResult(result: BigDecimal): String {
        val strippedResult = result.stripTrailingZeros()
        return if (strippedResult.scale() <= 0) {
            // If no decimal part (e.g., 1.0 or 1), convert to BigInteger string.
            strippedResult.toBigInteger().toString()
        } else {
            // If there is a decimal part, ensure a max scale for display and use plain string.
            strippedResult.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
    }
}
