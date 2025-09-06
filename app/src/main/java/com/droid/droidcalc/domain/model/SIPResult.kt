/**
 * Defines the data model for the results of a Systematic Investment Plan (SIP) calculation.
 * It holds various calculated financial metrics related to the SIP.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.domain.model

import java.math.BigDecimal

/**
 * Data class representing the comprehensive results of a SIP calculation.
 *
 * @property futureValueOrdinaryAnnuity The calculated future value assuming an ordinary annuity (payments at the end of periods).
 * @property futureValueAnnuityDue The calculated future value assuming an annuity due (payments at the beginning of periods).
 * @property requiredMonthlyContribution The monthly contribution needed to reach a target future value (if a target was specified).
 * @property totalInvestment The total amount invested over the SIP duration.
 * @property totalInterestEarnedOrdinary The total interest earned for an ordinary annuity.
 * @property totalInterestEarnedDue The total interest earned for an annuity due.
 * @property monthlyRate The monthly interest rate used in calculations.
 * @property numberOfPeriods The total number of investment periods (months).
 * @property initialInvestment The initial investment amount (lump sum).
 * @property monthlyContribution The regular monthly contribution amount.
 * @author DroidSwap
 */
data class SIPResult(
    val futureValueOrdinaryAnnuity: BigDecimal,
    val futureValueAnnuityDue: BigDecimal,
    val requiredMonthlyContribution: BigDecimal? = null,
    val totalInvestment: BigDecimal,
    val totalInterestEarnedOrdinary: BigDecimal,
    val totalInterestEarnedDue: BigDecimal,
    val monthlyRate: BigDecimal,
    val numberOfPeriods: Int,
    val initialInvestment: BigDecimal,
    val monthlyContribution: BigDecimal
)
