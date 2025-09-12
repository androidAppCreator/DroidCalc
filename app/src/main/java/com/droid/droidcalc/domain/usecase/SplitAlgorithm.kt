package com.droid.droidcalc.domain.usecase

import androidx.annotation.StringRes
import com.droid.droidcalc.R // Ensure R class is correctly imported

/**
 * Defines the various algorithms available for splitting a bill among participants.
 * Each algorithm has a corresponding display name string resource ID for localization
 * and UI presentation.
 *
 * This enum is central to the splitting logic, allowing the ViewModel and UseCase
 * to determine how calculations should be performed based on user selection.
 *
 * @property displayNameResId The string resource ID for the user-friendly display name of the algorithm.
 */
enum class SplitAlgorithm(@StringRes val displayNameResId: Int) {
    /**
     * Splits the bill equally among all active (non-fixed-amount) participants after fixed amounts are deducted.
     * If no fixed amounts, splits the total equally.
     */
    EQUAL(R.string.split_algo_equal),

    /**
     * Each participant is assigned a specific fixed amount. Any remaining amount of the total bill
     * is then typically split equally among participants who were not assigned a fixed amount, or it indicates
     * an over/under assignment. The primary focus here is that each participant *can* have a fixed amount.
     * The remaining amount handling logic is part of the SplitUseCase.
     */
    FIXED_THEN_EQUAL(R.string.split_algo_fixed_then_equal),

    /**
     * Splits the bill based on a percentage assigned to each participant.
     * The sum of percentages for all participants must typically equal 100%.
     */
    PERCENTAGE(R.string.split_algo_percentage),

    /**
     * Splits the bill based on weights assigned to each participant.
     * The share is proportional to the weight (e.g., someone with weight 2 gets twice as much as someone with weight 1).
     */
    WEIGHTED(R.string.split_algo_weighted),

    /**
     * Splits the bill based on ratio parts assigned to each participant.
     * Similar to weighted, but often expressed as parts of a whole (e.g., 2:3:5).
     */
    RATIO(R.string.split_algo_ratio);

    // The `entries` property (for Kotlin 1.9+) or `values()` method (older Kotlin)
    // can be used to iterate over all enum constants.
    // No need for a custom `getDisplayName(Context)` method if using stringResource directly in Composables.
}
