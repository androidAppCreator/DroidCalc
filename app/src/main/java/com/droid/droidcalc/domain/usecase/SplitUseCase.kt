/**
 * This file defines the use case for splitting a bill among participants using various algorithms.
 * It handles calculations with BigDecimal for precision, including tip, tax, and remainder distribution.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.domain.usecase

import com.droid.droidcalc.domain.model.Participant
import java.math.BigDecimal
import java.math.RoundingMode
import java.math.MathContext
import javax.inject.Inject // Added for Hilt

/**
 * Enum representing the different algorithms available for splitting the bill.
 * @author DroidSwap
 */
enum class SplitAlgorithm {
    EQUAL,
    FIXED_THEN_EQUAL,
    PERCENTAGE,
    WEIGHTED,
    RATIO
}

/**
 * Use case responsible for calculating the split of a total amount among participants
 * based on a selected algorithm, including tip and tax considerations.
 * This class is injectable via Hilt.
 *
 * @author DroidSwap
 */
class SplitUseCase @Inject constructor() { // Added @Inject constructor for Hilt

    private val twoDecimalScale = 2
    private val defaultRoundingMode = RoundingMode.HALF_UP
    private val precisionContext = MathContext.DECIMAL128 // High precision for intermediate calculations

    /**
     * Calculates the split amount for each participant.
     *
     * @param totalBill The total bill amount before tip and tax.
     * @param tipAmount The total tip amount.
     * @param taxAmount The total tax amount.
     * @param participants A list of [Participant] objects involved in the split.
     * @param algorithm The [SplitAlgorithm] to be used for calculation.
     * @return A map where the key is the participant ID and the value is their calculated share.
     *         Returns an empty map if participants list is empty or an error occurs.
     * @throws IllegalArgumentException if inputs are invalid (e.g., negative amounts, empty participants for certain algos).
     * @author DroidSwap
     */
    operator fun invoke(
        totalBill: BigDecimal,
        tipAmount: BigDecimal,
        taxAmount: BigDecimal,
        participants: List<Participant>,
        algorithm: SplitAlgorithm
    ): Map<String, BigDecimal> {
        if (participants.isEmpty()) {
            return emptyMap()
        }
        if (totalBill < BigDecimal.ZERO || tipAmount < BigDecimal.ZERO || taxAmount < BigDecimal.ZERO) {
            throw IllegalArgumentException("Amounts cannot be negative.")
        }

        val totalAmountToSplit = totalBill + tipAmount + taxAmount
        if (totalAmountToSplit <= BigDecimal.ZERO && algorithm != SplitAlgorithm.EQUAL) {
             // Allow splitting zero equally (results in zero for everyone)
            if (totalAmountToSplit < BigDecimal.ZERO) throw IllegalArgumentException("Total amount to split cannot be negative.")
            return participants.associate { it.id to BigDecimal.ZERO.setScale(twoDecimalScale, defaultRoundingMode) }
        }

        val calculatedShares = when (algorithm) {
            SplitAlgorithm.EQUAL -> calculateEqualSplit(totalAmountToSplit, participants)
            SplitAlgorithm.FIXED_THEN_EQUAL -> calculateFixedThenEqualSplit(totalAmountToSplit, participants)
            SplitAlgorithm.PERCENTAGE -> calculatePercentageSplit(totalAmountToSplit, participants)
            SplitAlgorithm.WEIGHTED -> calculateWeightedSplit(totalAmountToSplit, participants, isRatio = false)
            SplitAlgorithm.RATIO -> calculateWeightedSplit(totalAmountToSplit, participants, isRatio = true)
        }
        
        return distributeRemainder(calculatedShares, totalAmountToSplit, participants)
    }

    private fun calculateEqualSplit(
        amount: BigDecimal,
        participants: List<Participant>
    ): Map<String, BigDecimal> {
        if (participants.isEmpty()) return emptyMap()
        val sharePerParticipant = amount.divide(BigDecimal(participants.size), precisionContext)
        return participants.associate { it.id to sharePerParticipant }
    }

    private fun calculateFixedThenEqualSplit(
        amount: BigDecimal,
        participants: List<Participant>
    ): Map<String, BigDecimal> {
        var remainingAmount = amount
        val fixedShares = mutableMapOf<String, BigDecimal>()
        val participantsForEqualSplit = mutableListOf<Participant>()

        participants.forEach { p ->
            val fixedVal = p.fixedAmountInput.toBigDecimalOrNull()
            if (fixedVal != null && fixedVal > BigDecimal.ZERO) {
                val actualFixed = fixedVal.min(remainingAmount)
                fixedShares[p.id] = actualFixed
                remainingAmount -= actualFixed
                if (remainingAmount < BigDecimal.ZERO) remainingAmount = BigDecimal.ZERO
            } else {
                participantsForEqualSplit.add(p)
            }
        }

        val finalShares = fixedShares.toMutableMap()
        if (participantsForEqualSplit.isNotEmpty() && remainingAmount > BigDecimal.ZERO) {
            val equalShare = remainingAmount.divide(BigDecimal(participantsForEqualSplit.size), precisionContext)
            participantsForEqualSplit.forEach {
                finalShares[it.id] = (finalShares[it.id] ?: BigDecimal.ZERO) + equalShare
            }
        } else {
            participantsForEqualSplit.forEach {
                 finalShares[it.id] = (finalShares[it.id] ?: BigDecimal.ZERO)
            }
        }
        participants.forEach { p ->
            if (!finalShares.containsKey(p.id)) {
                finalShares[p.id] = BigDecimal.ZERO
            }
        }

        return finalShares
    }

    private fun calculatePercentageSplit(
        amount: BigDecimal,
        participants: List<Participant>
    ): Map<String, BigDecimal> {
        val totalPercentage = participants.sumOf { it.percentageInput.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        if (totalPercentage.compareTo(BigDecimal(100)) != 0 && amount > BigDecimal.ZERO && totalPercentage.compareTo(BigDecimal.ZERO) != 0) {
            // Allow sum not equal to 100, will distribute proportionally if totalPercentage > 0
        } else if (totalPercentage.compareTo(BigDecimal.ZERO) == 0 && amount > BigDecimal.ZERO) {
             throw IllegalArgumentException("Total percentage cannot be zero if amount is to be split by percentage.")
        } else if (totalPercentage.compareTo(BigDecimal(100)) != 0 && totalPercentage.compareTo(BigDecimal.ZERO) != 0) {
            // This condition is for cases where amount might be zero, but sum of percentages is not 100.
            // Depending on desired behavior, could warn or proceed (as it will result in zero shares anyway if amount is zero).
        }


        return participants.associate { p ->
            val percentage = p.percentageInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val share = if (totalPercentage > BigDecimal.ZERO) {
                amount.multiply(percentage, precisionContext).divide(totalPercentage, precisionContext)
            } else BigDecimal.ZERO
            p.id to share
        }
    }

    private fun calculateWeightedSplit(
        amount: BigDecimal,
        participants: List<Participant>,
        isRatio: Boolean
    ): Map<String, BigDecimal> {
        val totalWeightOrRatio = participants.sumOf { participant ->
            val inputValue = if (isRatio) participant.ratioInput else participant.weightInput
            inputValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }

        if (totalWeightOrRatio.compareTo(BigDecimal.ZERO) == 0 && amount > BigDecimal.ZERO) {
            throw IllegalArgumentException("Total weight/ratio cannot be zero if amount is to be split.")
        }

        return participants.associate { p ->
            val weightOrRatioValue = (if (isRatio) p.ratioInput else p.weightInput).toBigDecimalOrNull() ?: BigDecimal.ZERO
            val share = if (totalWeightOrRatio > BigDecimal.ZERO) {
                amount.multiply(weightOrRatioValue, precisionContext).divide(totalWeightOrRatio, precisionContext)
            } else BigDecimal.ZERO
            p.id to share
        }
    }

    private fun distributeRemainder(
        initialShares: Map<String, BigDecimal>,
        totalAmountToSplit: BigDecimal,
        participants: List<Participant> 
    ): Map<String, BigDecimal> {
        val roundedShares = initialShares.mapValues {
            it.value.setScale(twoDecimalScale, defaultRoundingMode)
        }.toMutableMap()

        var currentSum = roundedShares.values.fold(BigDecimal.ZERO, BigDecimal::add)
        var remainder = totalAmountToSplit.setScale(twoDecimalScale, defaultRoundingMode) - currentSum
        
        val penny = BigDecimal("0.01").setScale(twoDecimalScale, defaultRoundingMode)

        val sortedParticipantsByFraction = participants
            .filter { initialShares.containsKey(it.id) } // Ensure participant is in initialShares
            .sortedByDescending { participant ->
                val initialShare = initialShares[participant.id] ?: BigDecimal.ZERO
                initialShare.remainder(BigDecimal.ONE) 
            }
        
        var participantIndex = 0
        val maxIterations = sortedParticipantsByFraction.size * 2 // Safety break

        while (remainder.abs().compareTo(penny.divide(BigDecimal(2), precisionContext)) >= 0 && participantIndex < maxIterations) {
             if (sortedParticipantsByFraction.isEmpty()) break // No participants to distribute to

            val participantToAdjust = sortedParticipantsByFraction[participantIndex % sortedParticipantsByFraction.size]
            
            if (remainder > BigDecimal.ZERO) {
                roundedShares[participantToAdjust.id] = (roundedShares[participantToAdjust.id] ?: BigDecimal.ZERO) + penny
                remainder -= penny
            } else if (remainder < BigDecimal.ZERO) {
                val currentShare = roundedShares[participantToAdjust.id] ?: BigDecimal.ZERO
                if (currentShare >= penny) {
                    roundedShares[participantToAdjust.id] = currentShare - penny
                    remainder += penny
                } else {
                    // Cannot subtract full penny, try to subtract what's left or skip
                    // This can happen if a share is already very small or zero.
                    // For simplicity, we only adjust if a full penny can be subtracted.
                    // More complex logic could try to adjust by smaller amounts or prioritize differently.
                }
            }
            participantIndex++
        }
        return roundedShares.mapValues { it.value.setScale(twoDecimalScale, defaultRoundingMode) }
    }
}
