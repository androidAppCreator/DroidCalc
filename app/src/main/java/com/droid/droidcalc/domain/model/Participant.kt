package com.droid.droidcalc.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Defines the data model for a participant in a bill split calculation.
 * Each participant has a unique ID, a name, and various input fields for different
 * splitting algorithms (e.g., fixed amount, percentage). It also tracks UI-specific
 * states like `isYou` and `isPaid`, and holds the calculated `share`.
 *
 * This data class is a central piece of the domain model for the split feature,
 * representing the state and properties of individual participants.
 *
 * @property id Unique identifier for the participant, typically a UUID.
 * @property name The display name of the participant, entered by the user.
 * @property fixedAmountInput String representation of the fixed amount this participant contributes/is assigned,
 *                            used for UI binding and converted to [BigDecimal] for calculations.
 * @property percentageInput String representation of the percentage this participant contributes/is assigned,
 *                           used for UI binding and converted to [BigDecimal] for calculations.
 * @property weightInput String representation of the weight assigned to this participant for weighted splits,
 *                       used for UI binding and converted to [BigDecimal] or Int for calculations.
 * @property ratioInput String representation of the ratio part assigned to this participant for ratio-based splits,
 *                      used for UI binding and converted to [BigDecimal] or Int for calculations.
 * @property isYou Boolean flag indicating if this participant represents the current user of the app.
 *                 This helps in customizing UI elements or behavior specific to "You".
 * @property isPaid Boolean flag indicating if this participant's share has been marked as paid.
 *                  This is typically controlled by a checkbox in the UI.
 * @property share The calculated monetary share for this participant after the split logic is applied.
 *                 Null if the share has not yet been calculated or is not applicable.
 * @property totalToSplitForPercentageCalc The total amount that was used as the basis for calculating this participant's share
 *                                         when the percentage algorithm was active. This is primarily for display purposes
 *                                         in the UI to show context for the percentage (e.g., "50% of $100.00").
 * @property avatarSeed A string used to generate a consistent visual representation (e.g., an avatar with a specific
 *                      color or initials). Defaults to the participant's name if not specified otherwise.
 */
data class Participant(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val fixedAmountInput: String = "",
    val percentageInput: String = "",
    val weightInput: String = "",
    val ratioInput: String = "",
    val isYou: Boolean = false,
    val isPaid: Boolean = false,
    val share: BigDecimal? = null, // To store the calculated share from SplitResult
    val totalToSplitForPercentageCalc: BigDecimal? = null, // Context for percentage display
    val avatarSeed: String = name // Default seed for avatar generation, can be ID or name. Updated if name changes.
) {
    // Secondary constructor to allow avatarSeed to default to name upon initialization, useful if name is set later.
    // However, the primary constructor already handles this if name is provided at construction.
    // If name is mutable and avatarSeed should react, a custom getter or a copy mechanism would be needed.
    // For simplicity with data classes, avatarSeed is set at construction based on the initial name.
    // If name is updated, the ViewModel should handle creating a new Participant instance with updated avatarSeed if necessary.
}
