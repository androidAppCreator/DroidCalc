package com.droid.droidcalc.domain.repository

import com.droid.droidcalc.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the DroidCalc history repository.
 * This repository is responsible for managing calculation history data, 
 * abstracting the data sources (local Room database and remote Firestore) from the domain layer.
 * It defines operations for saving, retrieving, and syncing history entries.
 *
 * @author DroidSwap
 */
interface HistoryRepository {

    /**
     * Retrieves all calculation history entries as a [Flow].
     * The returned Flow will emit a new list of [HistoryEntity] objects whenever the underlying data changes.
     * This is typically sourced from the local database and kept updated by sync mechanisms.
     *
     * @return A [Flow] emitting a list of [HistoryEntity] objects.
     */
    fun getHistory(): Flow<List<HistoryEntity>>

    /**
     * Adds a new calculation history entry.
     * This operation should save the entry to the local database and potentially trigger a sync with Firestore.
     *
     * @param expression The mathematical expression that was calculated.
     * @param result The result of the calculation.
     */
    suspend fun addHistoryEntry(expression: String, result: String)

    /**
     * Deletes a specific history entry by its local database ID.
     * This operation should also propagate the deletion to Firestore if applicable.
     *
     * @param id The unique ID of the history entry to delete.
     */
    suspend fun deleteHistoryEntry(id: Long)

    /**
     * Clears all calculation history from both the local database and Firestore.
     * This is a destructive operation and should be used with caution.
     */
    suspend fun clearAllHistory()

    /**
     * Triggers a synchronization of the calculation history between the local database and Firestore.
     * This method handles fetching remote data, merging with local data according to the defined strategy
     * (newest by timestamp, keep last 20), and updating both data sources.
     * Anonymous Firebase authentication will be handled if no user is signed in.
     */
    suspend fun syncHistoryWithFirestore()
}
