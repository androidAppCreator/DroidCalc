package com.droid.droidcalc.ui.history.intent

/**
 * Defines the user intents (actions or events) for the History screen.
 * This sealed class encapsulates all possible interactions a user can perform
 * with the history UI, which are then processed by the [com.droid.droidcalc.ui.history.viewmodel.HistoryViewModel].
 *
 * @author DroidSwap
 */
sealed class HistoryIntent {

    /**
     * Represents the user's request to load or refresh the calculation history.
     * This might be triggered on screen entry or by a pull-to-refresh action.
     */
    data object LoadHistory : HistoryIntent()

    /**
     * Represents the user's request to delete a specific history item.
     *
     * @property itemId The unique ID of the [com.droid.droidcalc.data.db.HistoryEntity] to be deleted.
     */
    data class DeleteHistoryItem(val itemId: Long) : HistoryIntent()

    /**
     * Represents the user's request to clear all calculation history.
     */
    data object ClearAllHistory : HistoryIntent()

    /**
     * Represents the user's request to manually trigger a synchronization of the history
     * with the remote data source (Firestore).
     */
    data object SyncHistory : HistoryIntent()

    // Add other intents as needed, e.g., selecting an item to re-use in calculator.
}
