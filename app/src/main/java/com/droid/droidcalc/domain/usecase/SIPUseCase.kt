/**
 * This file defines the use case for performing Systematic Investment Plan (SIP) calculations.
 * It includes formulas for calculating future values (ordinary annuity and annuity due)
 * and the required monthly contribution to reach a target, using BigDecimal for precision.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.domain.usecase

import com.droid.droidcalc.domain.model.SIPResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.math.MathContext
import javax.inject.Inject

/**
 * Enum representing the different types of SIP calculations that can be performed.
 */
enum class SIPCalculationType {
    FUTURE_VALUE_ORDINARY_ANNUITY,
    FUTURE_VALUE_ANNUITY_DUE,
    REQUIRED_MONTHLY_CONTRIBUTION
}

/**
 * Use case responsible for performing SIP (Systematic Investment Plan) calculations.
 * It computes future values, required contributions, total investment, and interest earned.
 * All financial calculations are performed using BigDecimal for accuracy.
 *
 * @author DroidSwap
 */
class SIPUseCase @Inject constructor(){

    private val scale = 2 // Standard scale for currency
    private val calculationScale = 16 // Higher scale for intermediate calculations to maintain precision
    private val roundingMode = RoundingMode.HALF_UP
    private val mathContext = MathContext(calculationScale, roundingMode) // For pow operations

    /**
     * Calculates SIP results based on the provided inputs.
     *
     * @param initialInvestment The initial lump sum investment.
     * @param monthlyContribution The amount contributed monthly.
     * @param annualRatePercentage The annual interest rate as a percentage (e.g., 5 for 5%).
     * @param periodsInMonths The total number of periods (months) for the investment.
     * @param targetFutureValue Optional target future value to calculate required monthly contribution.
     * @return [SIPResult] containing all calculated values.
     * @throws IllegalArgumentException if inputs are invalid (e.g., negative amounts, rate <= 0, duration <= 0).
     */
    operator fun invoke(
        initialInvestment: BigDecimal,
        monthlyContribution: BigDecimal,
        annualRatePercentage: BigDecimal,
        periodsInMonths: Int,
        targetFutureValue: BigDecimal? = null
    ): SIPResult {
        // Validate inputs
        if (initialInvestment < BigDecimal.ZERO) throw IllegalArgumentException("Initial investment cannot be negative.")
        if (monthlyContribution < BigDecimal.ZERO && targetFutureValue == null) throw IllegalArgumentException("Monthly contribution cannot be negative.")
        if (annualRatePercentage <= BigDecimal.ZERO) throw IllegalArgumentException("Annual rate must be greater than 0.")
        if (periodsInMonths <= 0) throw IllegalArgumentException("Periods (months) must be greater than 0.")
        if (targetFutureValue != null && targetFutureValue <= BigDecimal.ZERO) throw IllegalArgumentException("Target future value must be greater than 0.")

        val monthlyRate = annualRatePercentage.divide(BigDecimal(100), calculationScale, roundingMode)
            .divide(BigDecimal(12), calculationScale, roundingMode)
        val n = periodsInMonths.toBigDecimal()

        // FV of initial investment: FV = PV * (1 + r)^n
        val fvInitialInvestment = initialInvestment.multiply((BigDecimal.ONE + monthlyRate).pow(periodsInMonths, mathContext))

        // Calculate Future Value of series of monthly contributions (Ordinary Annuity)
        // FV = P * [((1 + r)^n - 1) / r]
        val fvOrdinaryAnnuityContributions: BigDecimal
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) { // Handle zero interest rate
            fvOrdinaryAnnuityContributions = monthlyContribution.multiply(n)
        } else {
            val compoundFactorOrdinary = ((BigDecimal.ONE + monthlyRate).pow(periodsInMonths, mathContext) - BigDecimal.ONE)
                .divide(monthlyRate, calculationScale, roundingMode)
            fvOrdinaryAnnuityContributions = monthlyContribution.multiply(compoundFactorOrdinary)
        }
        val totalFVOrdinaryAnnuity = fvInitialInvestment + fvOrdinaryAnnuityContributions

        // Calculate Future Value of series of monthly contributions (Annuity Due)
        // FV = P * [((1 + r)^n - 1) / r] * (1 + r)
        val fvAnnuityDueContributions = fvOrdinaryAnnuityContributions.multiply(BigDecimal.ONE + monthlyRate)
        val totalFVAnnuityDue = fvInitialInvestment + fvAnnuityDueContributions

        // Calculate Required Monthly Contribution if targetFutureValue is provided
        var requiredMonthlyContribution: BigDecimal? = null
        if (targetFutureValue != null) {
            val targetFVForContributions = targetFutureValue - fvInitialInvestment
            if (targetFVForContributions > BigDecimal.ZERO) {
                if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) { // Handle zero interest rate
                     requiredMonthlyContribution = if (n > BigDecimal.ZERO) targetFVForContributions.divide(n, scale, roundingMode) else BigDecimal.ZERO
                } else {
                    val denominator = ((BigDecimal.ONE + monthlyRate).pow(periodsInMonths, mathContext) - BigDecimal.ONE)
                        .divide(monthlyRate, calculationScale, roundingMode)
                    requiredMonthlyContribution = if (denominator.compareTo(BigDecimal.ZERO) != 0) {
                        targetFVForContributions.divide(denominator, scale, roundingMode)
                    } else {
                        BigDecimal.ZERO // Or throw error, implies target can't be reached
                    }
                }
            } else {
                // Initial investment already meets or exceeds target
                requiredMonthlyContribution = BigDecimal.ZERO
            }
        }

        val actualMonthlyContribution = monthlyContribution // The one used for FV calculations
        val totalPrincipalInvested = initialInvestment + actualMonthlyContribution.multiply(n)

        val totalInterestOrdinary = totalFVOrdinaryAnnuity - totalPrincipalInvested
        val totalInterestDue = totalFVAnnuityDue - totalPrincipalInvested

        return SIPResult(
            futureValueOrdinaryAnnuity = totalFVOrdinaryAnnuity.setScale(scale, roundingMode),
            futureValueAnnuityDue = totalFVAnnuityDue.setScale(scale, roundingMode),
            requiredMonthlyContribution = requiredMonthlyContribution?.setScale(scale, roundingMode),
            totalInvestment = totalPrincipalInvested.setScale(scale, roundingMode),
            totalInterestEarnedOrdinary = totalInterestOrdinary.setScale(scale, roundingMode),
            totalInterestEarnedDue = totalInterestDue.setScale(scale, roundingMode),
            monthlyRate = monthlyRate, // Keep full precision for potential display/debugging
            numberOfPeriods = periodsInMonths,
            initialInvestment = initialInvestment.setScale(scale, roundingMode),
            monthlyContribution = actualMonthlyContribution.setScale(scale, roundingMode)
        )
    }
}
