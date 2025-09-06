package com.droid.droidcalc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the [HistoryEntity] table.
 * This interface defines the methods for interacting with the calculation history data in the Room database.
 * All database operations performed through this DAO should be executed on a background thread
 * (e.g., using Kotlin Coroutines with a specific dispatcher) to avoid blocking the main UI thread.
 *
 * @author DroidSwap
 */
@Dao
interface HistoryDao {

    /**
     * Inserts a single calculation history entry into the database.
     * If a conflict occurs (e.g., an entry with the same primary key already exists, though unlikely with autoGenerate),
     * the existing entry will be replaced.
     *
     * @param historyEntity The [HistoryEntity] to insert.
     * @return The row ID of the newly inserted entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(historyEntity: HistoryEntity): Long

    /**
     * Inserts a list of calculation history entries into the database.
     * If a conflict occurs for any entry, the existing entry will be replaced.
     * This is useful for batch operations, such as syncing data from Firestore.
     *
     * @param historyEntities A list of [HistoryEntity] objects to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(historyEntities: List<HistoryEntity>)

    /**
     * Retrieves all calculation history entries from the database, ordered by timestamp in descending order (newest first).
     * This method returns a [Flow], allowing observers to receive updates automatically when the data changes.
     *
     * @return A [Flow] emitting a list of all [HistoryEntity] objects.
     */
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    /**
     * Retrieves the most recent [limit] number of calculation history entries, ordered by timestamp in descending order.
     * This method is useful for displaying a limited number of recent items, e.g., for syncing with Firestore.
     *
     * @param limit The maximum number of history entries to retrieve.
     * @return A list of the most recent [HistoryEntity] objects, up to the specified limit.
     */
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<HistoryEntity>

    /**
     * Deletes a specific calculation history entry from the database by its ID.
     *
     * @param id The ID of the [HistoryEntity] to delete.
     * @return The number of rows affected by the delete operation (should be 1 if successful, 0 if not found).
     */
    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes all entries from the calculation_history table.
     * This operation is irreversible and should be used with caution.
     */
    @Query("DELETE FROM calculation_history")
    suspend fun clearAllHistory()

    /**
     * Deletes entries older than the N newest entries, keeping only the specified number of recent items.
     * This is useful for maintaining a fixed-size history log (e.g., last 20 items as per Firestore sync requirement).
     * The subquery finds the timestamp of the Nth newest record, and then all records older than that are deleted.
     * This is a common strategy for pruning old data.
     *
     * @param countToKeep The number of newest entries to retain in the database.
     */
    @Query("DELETE FROM calculation_history WHERE timestamp NOT IN (SELECT timestamp FROM calculation_history ORDER BY timestamp DESC LIMIT :countToKeep)")
    suspend fun pruneOldHistory(countToKeep: Int)
}
