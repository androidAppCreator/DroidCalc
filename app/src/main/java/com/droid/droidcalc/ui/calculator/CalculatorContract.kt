/**
 * This file defines the contract for the Calculator screen, encapsulating its UI State and Intents.
 * It adheres to the MVI (Model-View-Intent) architecture pattern, serving as the single source of truth
 * for the calculator feature's state and user actions.
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.calculator

/**
 * Defines the contract for the Calculator screen, including its state and user intents.
 * @author DroidSwap
 */
object CalculatorContract {

    /**
     * Represents the immutable state of the Calculator screen.
     * This data class holds all the necessary information to render the UI,
     * including the current expression, the displayed result, any error messages,
     * and state for handling interactions with the result display.
     *
     * @property expression The current mathematical expression entered by the user (e.g., "2+3*4").
     * @property displayValue The value currently shown in the main display. This could be the ongoing expression,
     *                        the calculated result, or an error message.
     * @property error Any error message to be displayed to the user (e.g., "Division by zero"). Null if no error.
     * @property isResultDisplayed True if the `displayValue` currently shows a final calculated result, false otherwise.
     *                             This helps in UI decisions, like whether to clear the expression on next input.
     * @property showResultActionBottomSheet True if the bottom sheet offering actions for a numeric result should be shown.
     * @property actionableNumericResult The specific numeric string from `displayValue` that was clicked and is eligible for actions.
     *                                   Null if no actionable result is currently selected.
     *                                   This will be used by the ViewModel to derive navigation arguments if needed.
     * @author DroidSwap
     */
    data class State(
        val expression: String = "",
        val displayValue: String = "0", // Initial display value
        val liveEvaluation: String = "",     // Live calculation of 'expression', e.g., "8" for "2+2*3"
        val error: String? = null,
        val isResultDisplayed: Boolean = false,
        val showResultActionBottomSheet: Boolean = false,
        val actionableNumericResult: String? = null
        // Note: navigateToRoute was part of the old CalculatorState, but navigation will now be handled by NavigationEvent side effects.
    )

    /**
     * Defines the user intents (actions or events) for the Calculator screen.
     * This sealed class encapsulates all possible interactions a user can perform
     * with the calculator UI, which are then processed by the [com.droid.droidcalc.ui.calculator.viewmodel.CalculatorViewModel].
     *
     * @author DroidSwap
     */
    sealed class Intent {

        /**
         * Represents the user tapping a number button (0-9).
         * @property number The digit that was pressed.
         */
        data class NumberInput(val number: Char) : Intent()

        /**
         * Represents the user tapping an operator button (+, -, *, /).
         * @property operator The operator character that was pressed.
         */
        data class OperatorInput(val operator: Char) : Intent()

        /**
         * Represents the user tapping the decimal point button.
         */
        data object DecimalInput : Intent()

        /**
         * Represents the user tapping the equals (=) button to calculate the result.
         */
        data object Calculate : Intent()

        /**
         * Represents the user tapping the clear (C or AC) button.
         * This should clear the current expression and result.
         */
        data object Clear : Intent()

        /**
         * Represents the user tapping the backspace or delete button.
         * This should remove the last character from the current expression.
         */
        data object Delete : Intent()

        /**
         * Represents the user tapping the parentheses button (now a single "( )" button).
         * The ViewModel will decide whether to add an opening or closing parenthesis based on context.
         */
        data object ParenthesesInput : Intent()
        
        /**
         * Represents the user tapping the percentage (%) button.
         * The ViewModel will typically convert the current number to its percentage value (e.g., N/100).
         */
        data object PercentageInput : Intent()

        // --- Intents for Result Interaction ---

        /**
         * Represents the user tapping on the result display area.
         * The ViewModel will determine if the result is actionable (e.g., numeric).
         */
        data object ResultDisplayClicked : Intent()

        /**
         * Represents the user selecting the "Split Amount" action from the bottom sheet.
         * The ViewModel will use its current state (`actionableNumericResult` or `displayValue`)
         * to get the value for navigation.
         */
        data object ActionNavigateToSplit : Intent()

        /**
         * Represents the user selecting the "SIP Calculator" action from the bottom sheet.
         * The ViewModel will use its current state (`actionableNumericResult` or `displayValue`)
         * to get the value for navigation.
         */
        data object ActionNavigateToSIP : Intent()

        /**
         * Represents the user dismissing the result actions bottom sheet.
         */
        data object DismissResultActions : Intent()

        /**
         * Signals that a navigation effect (triggered by ViewModel) has been consumed by the UI.
         * This allows the ViewModel to clear any internal navigation trigger flags if needed,
         * though one-time events via SharedFlow (Channel) often don't require this.
         */
        data object NavigationEffectConsumed : Intent() // This might become less relevant if nav events are pure side effects.
    }
}
