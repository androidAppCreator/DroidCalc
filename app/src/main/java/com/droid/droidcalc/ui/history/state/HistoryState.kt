package com.droid.droidcalc.ui.history.state

import com.droid.droidcalc.data.db.HistoryEntity

/**
 * Represents the immutable state for the History screen.
 * This data class holds all necessary information to render the history UI,
 * including the list of history entries and the current synchronization status.
 *
 * @property historyItems A list of [HistoryEntity] objects representing the calculation history.
 * @property isLoading Indicates if a data loading or synchronization operation is currently in progress.
 * @property error An optional error message to display if an operation failed.
 * @author DroidSwap
 */
data class HistoryState(
    val historyItems: List<HistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
