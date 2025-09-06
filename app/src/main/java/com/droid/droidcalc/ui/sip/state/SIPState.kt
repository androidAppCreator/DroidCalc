/**
 * Defines the UI state for the SIP (Systematic Investment Plan) Calculator Screen.
 * This file contains the data class representing all information required to render the SIP Screen.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.sip.state

import com.droid.droidcalc.domain.model.SIPResult
import com.droid.droidcalc.domain.usecase.SIPCalculationType
import java.math.BigDecimal

/**
 * Represents the immutable UI state for the SIPScreen.
 *
 * @property initialInvestmentInput User input for the initial investment amount (as a string).
 *                                  This can be pre-filled from calculator result via navigation.
 * @property monthlyContributionInput User input for the monthly contribution (as a string).
 * @property annualRateInput User input for the annual interest rate (percentage, as a string).
 * @property periodsInMonthsInput User input for the investment duration in months (as a string).
 * @property targetFutureValueInput Optional user input for the target future value (as a string).
 * @property selectedCalculationType The type of SIP calculation to perform or focus on.
 *                                   Defaults to calculating Future Value (Ordinary Annuity).
 * @property calculationResult The [SIPResult] object holding the outcomes of the calculation. Null if not calculated or error.
 * @property isLoading Indicates if a calculation is in progress.
 * @property errorMessages A map of error messages, where the key could be an input field identifier or a general error type.
 *                         e.g., "annualRate" to "Rate must be positive".
 * @property isChartVisible Flag to control the visibility of the animated line chart.
 * @author DroidSwap
 */
data class SIPScreenState(
    val initialInvestmentInput: String = "",
    val monthlyContributionInput: String = "",
    val annualRateInput: String = "",
    val periodsInMonthsInput: String = "",
    val targetFutureValueInput: String = "",
    val selectedCalculationType: SIPCalculationType = SIPCalculationType.FUTURE_VALUE_ORDINARY_ANNUITY,
    val calculationResult: SIPResult? = null,
    val isLoading: Boolean = false,
    val errorMessages: Map<String, String> = emptyMap(),
    val isChartVisible: Boolean = false // Initially false, can be set to true after first successful calculation
)
