/**
 * Defines the data model for a participant in a split calculation.
 * Each participant has a unique ID, a name, and optional values for different
 * splitting algorithms (fixed amount, percentage, weight, ratio).
 * An avatarSeed is included for generating a consistent visual representation (emoji/color).
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.domain.model

import java.util.UUID

data class Participant(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val fixedAmountInput: String = "", // For UI binding, convert to BigDecimal for logic
    val percentageInput: String = "", // For UI binding, convert to Double/BigDecimal for logic
    val weightInput: String = "",     // For UI binding, convert to Int for logic
    val ratioInput: String = "",      // For UI binding, convert to Int for logic
    val avatarSeed: String = name // Default seed for avatar generation, can be ID or name
)
