/**
 * Defines the contract for the SIP (Systematic Investment Plan) Calculator screen,
 * encapsulating its UI State, Intents, UI Effects, and other related definitions.
 * This follows the MVI (Model-View-Intent) architecture pattern.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.sip

import com.droid.droidcalc.domain.model.SIPResult
import java.math.BigDecimal

/**
 * Defines the contract for the SIP Calculator screen, including its state, user intents, and UI effects.
 * @author DroidSwap
 */
object SIPContract {

    /**
     * Enum to define the mode for duration input (Years or Months).
     * @author DroidSwap
     */
    enum class DurationMode {
        YEARS,
        MONTHS
    }

    /**
     * Represents the immutable UI state for the SIP Calculator screen.
     * It holds all data necessary to render the UI and its components according to the detailed feature requirements.
     *
     * @property initialInvestmentInput User input for the initial lump sum investment (as a string).
     * @property monthlyContributionInput User input for the regular monthly contribution (P) (as a string).
     * @property annualRateInput User input for the annual interest rate (percentage, as a string).
     * @property durationInput User input for the investment duration (value depends on [durationMode], as a string).
     * @property durationMode The selected mode for the duration input ([DurationMode.YEARS] or [DurationMode.MONTHS]).
     * @property targetGoalInput Optional user input for the target future value (as a string).
     * @property isAnnuityDue Boolean flag indicating if contributions are made at the start (true) or end (false) of the period.
     * @property calculationResult The [SIPResult] object from the domain layer, holding all calculated financial metrics.
     * @property isLoading Indicates if a calculation or data fetching operation is in progress.
     * @property errorMessages A map of error messages, where the key is a field identifier (e.g., "annualRate") or general error type,
     *                         intended for inline display next to specific fields.
     * @property isChartVisible Flag to control the visibility of timeline projection chart (placeholder for actual chart data).
     * @property showScenarioComparison Flag to control UI state for scenario comparison (placeholder).
     * @property activeBottomSheetTipId Identifier for any currently active interactive financial tip (placeholder).
     * @author DroidSwap
     */
    data class SIPUiState(
        val initialInvestmentInput: String = "",
        val monthlyContributionInput: String = "",
        val annualRateInput: String = "",
        val durationInput: String = "",
        val durationMode: DurationMode = DurationMode.YEARS,
        val targetGoalInput: String = "",
        val isAnnuityDue: Boolean = false, // false for Ordinary Annuity, true for Annuity Due
        val calculationResult: SIPResult? = null,
        val isLoading: Boolean = false,
        val errorMessages: Map<String, String> = emptyMap(),
        val isChartVisible: Boolean = false, // Placeholder for chart visibility
        val showScenarioComparison: Boolean = false, // Placeholder
        val activeBottomSheetTipId: String? = null // Placeholder
    )

    /**
     * Defines the user intents (actions) that can be triggered from the SIP Calculator UI.
     * These intents are processed by the [com.droid.droidcalc.ui.sip.viewmodel.SIPViewModel] to update the state or perform business logic.
     * @author DroidSwap
     */
    sealed class SIPUiIntent {
        /** Intent for changes to the initial investment input string. */
        data class UpdateInitialInvestment(val amount: String) : SIPUiIntent()
        /** Intent for changes to the monthly contribution input string. */
        data class UpdateMonthlyContribution(val amount: String) : SIPUiIntent()
        /** Intent for changes to the annual interest rate input string. */
        data class UpdateAnnualRate(val rate: String) : SIPUiIntent()
        /** Intent for changes to the duration value input string. */
        data class UpdateDuration(val value: String) : SIPUiIntent()
        /** Intent to toggle the duration input mode between Years and Months. */
        data class ToggleDurationMode(val mode: DurationMode) : SIPUiIntent()
        /** Intent for changes to the target goal input string. */
        data class UpdateTargetGoal(val amount: String) : SIPUiIntent()
        /** Intent to toggle the contribution timing (Annuity Due vs. Ordinary Annuity). */
        data class ToggleAnnuityDue(val isDue: Boolean) : SIPUiIntent()
        /** Intent to trigger the SIP calculation logic. */
        data object CalculateSIP : SIPUiIntent()
        /** Intent to reset all input fields to their default states. */
        data object ResetInputs : SIPUiIntent()
        /** Intent to save the current SIP scenario (placeholder for future implementation). */
        data object SaveScenario : SIPUiIntent()
        /** Intent to clear a specific error message from the UI state by its key (primarily for inline errors). */
        data class ClearError(val errorKey: String) : SIPUiIntent()
        /** Intent to show an interactive financial tip (placeholder). */
        data class ShowFinancialTip(val tipId: String) : SIPUiIntent()
        /** Intent to dismiss any currently shown financial tip/bottom sheet (placeholder). */
        data object DismissFinancialTip : SIPUiIntent()
    }

    /**
     * Defines one-time UI effects that can be triggered by the ViewModel for the SIP Screen.
     * These are typically handled by the UI layer to show transient messages (e.g., Snackbars) or trigger navigation.
     * @author DroidSwap
     */
    sealed interface UiEffect {
        /**
         * Effect to show a transient Snackbar message to the user.
         * @property message The message to be displayed. Ideally, this should be a string resource ID
         *                   resolved by the ViewModel to support localization.
         * @author DroidSwap
         */
        data class ShowSnackbar(val message: String) : UiEffect
        // Future effects like NavigateTo or ShowDialog can be added here.
    }
}
