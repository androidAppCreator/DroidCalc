package com.droid.droidcalc.data.repository

import com.droid.droidcalc.data.db.HistoryDao
import com.droid.droidcalc.data.db.HistoryEntity
import com.droid.droidcalc.domain.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the [HistoryRepository] interface.
 * This class manages calculation history data, coordinating between the local Room database
 * (via [HistoryDao]) and a remote Firebase Firestore instance.
 * It handles data storage, retrieval, deletion, and synchronization, including anonymous Firebase authentication.
 *
 * @property historyDao The Data Access Object for local history entities.
 * @property firestore The Firebase Firestore instance for remote data storage.
 * @property firebaseAuth The Firebase Authentication instance for anonymous sign-in.
 * @property ioDispatcher The CoroutineDispatcher for performing IO-bound operations.
 * @author DroidSwap
 */
@Singleton // Hilt will manage this as a singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HistoryRepository {

    private companion object {
        const val FIRESTORE_COLLECTION_PATH = "calculation_history"
        const val FIRESTORE_USER_ID_FIELD = "userId"
        const val FIRESTORE_EXPRESSION_FIELD = "expression"
        const val FIRESTORE_RESULT_FIELD = "result"
        const val FIRESTORE_TIMESTAMP_FIELD = "timestamp"
        const val HISTORY_LIMIT = 20 // Keep last 20 history items
    }

    /**
     * Retrieves all calculation history entries from the local database as a [Flow].
     *
     * @return A [Flow] emitting a list of [HistoryEntity] objects.
     */
    override fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    /**
     * Adds a new calculation history entry to both local Room database and Firestore.
     * Ensures the history in both sources is pruned to the [HISTORY_LIMIT].
     *
     * @param expression The mathematical expression calculated.
     * @param result The result of the calculation.
     */
    override suspend fun addHistoryEntry(expression: String, result: String) = withContext(ioDispatcher) {
        val timestamp = System.currentTimeMillis()
        val newEntry = HistoryEntity(expression = expression, result = result, timestamp = timestamp)

        try {
            // Save to Room
            historyDao.insert(newEntry)
            historyDao.pruneOldHistory(HISTORY_LIMIT) // Keep Room history to limit
            Timber.d("Added to Room: %s = %s", expression, result)

            // Save to Firestore
            val userId = getCurrentUserIdOrSignInAnonymously()
            if (userId != null) {
                val firestoreEntry = hashMapOf(
                    FIRESTORE_USER_ID_FIELD to userId,
                    FIRESTORE_EXPRESSION_FIELD to expression,
                    FIRESTORE_RESULT_FIELD to result,
                    FIRESTORE_TIMESTAMP_FIELD to timestamp
                )
                firestore.collection(FIRESTORE_COLLECTION_PATH).add(firestoreEntry).await()
                pruneFirestoreHistory(userId) // Keep Firestore history to limit
                Timber.d("Added to Firestore: %s = %s for user %s", expression, result, userId)
            } else {
                Timber.w("Could not get user ID for Firestore, entry not saved remotely.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding history entry for expression: %s", expression)
            // Optionally, rethrow or handle specific exceptions for UI feedback
        }
    }

    /**
     * Deletes a specific history entry by its local ID from Room and attempts to delete from Firestore.
     *
     * @param id The local ID of the history entry to delete.
     */
    override suspend fun deleteHistoryEntry(id: Long) = withContext(ioDispatcher) {
        try {
            // Find the entry in Room to get details for Firestore deletion if needed
            // For simplicity, we assume ID is sufficient if Firestore docs were mapped to Room IDs or used Firestore IDs in Room.
            // As IDs are different, we'd typically delete by content/timestamp if we don't store Firestore doc ID in Room.
            // Here, we just delete from Room. A more robust solution would involve querying Firestore.
            val affectedRows = historyDao.deleteById(id)
            if (affectedRows > 0) {
                Timber.d("Deleted history entry with id: %s from Room", id)
                // TODO: Implement robust Firestore deletion. This might involve querying by timestamp & expression
                // if we don't store Firestore document IDs in the Room entity.
                // For now, rely on periodic full sync to eventually remove it if it was synced.
            } else {
                Timber.w("No history entry found with id: %s in Room to delete", id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting history entry with id: %s", id)
        }
    }

    /**
     * Clears all calculation history from the local Room database and attempts to clear from Firestore for the current user.
     */
    override suspend fun clearAllHistory() = withContext(ioDispatcher) {
        try {
            historyDao.clearAllHistory()
            Timber.d("Cleared all history from Room.")

            val userId = getCurrentUserIdOrSignInAnonymously()
            if (userId != null) {
                val querySnapshot = firestore.collection(FIRESTORE_COLLECTION_PATH)
                    .whereEqualTo(FIRESTORE_USER_ID_FIELD, userId)
                    .get()
                    .await()
                firestore.runBatch { batch ->
                    querySnapshot.documents.forEach { batch.delete(it.reference) }
                }.await()
                Timber.d("Cleared all history from Firestore for user %s", userId)
            } else {
                Timber.w("Could not get user ID for Firestore, remote history not cleared.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing all history.")
        }
    }

    /**
     * Synchronizes local Room history with Firestore.
     * Fetches remote data, merges with local data, keeps the newest [HISTORY_LIMIT] items,
     * and updates both data sources.
     */
    override suspend fun syncHistoryWithFirestore() = withContext(ioDispatcher) {
        Timber.d("Starting history sync with Firestore...")
        try {
            val userId = getCurrentUserIdOrSignInAnonymously()
            if (userId == null) {
                Timber.w("Sync failed: Could not obtain user ID.")
                return@withContext
            }

            // 1. Fetch from Firestore (last HISTORY_LIMIT items for the user)
            val firestoreEntries = fetchFirestoreHistory(userId)
            Timber.d("Fetched %d entries from Firestore.", firestoreEntries.size)

            // 2. Fetch from Room (all items, will be pruned later)
            val localEntries = historyDao.getRecentHistory(HISTORY_LIMIT * 2) // Fetch more initially to ensure overlap
            Timber.d("Fetched %d entries from Room.", localEntries.size)

            // 3. Merge: Combine, remove duplicates (preferring Firestore for conflict, or based on timestamp),
            //    sort by timestamp descending, take top HISTORY_LIMIT.
            val allEntries = (firestoreEntries + localEntries)
                .distinctBy { it.expression to it.result } // Simple distinct, could be more robust
                .sortedByDescending { it.timestamp }
                .take(HISTORY_LIMIT)
            Timber.d("Merged and limited to %d entries.", allEntries.size)

            // 4. Update Room: Clear existing and insert merged list.
            historyDao.clearAllHistory()
            historyDao.insertAll(allEntries)
            Timber.d("Updated Room with merged entries.")

            // 5. Update Firestore: Clear user's existing Firestore entries and upload the merged list.
            //    This ensures consistency and respects the HISTORY_LIMIT.
            clearFirestoreHistoryForUser(userId) // Clear before re-uploading
            uploadToFirestore(userId, allEntries)
            Timber.d("Updated Firestore with merged entries for user %s.", userId)

            Timber.i("History sync with Firestore completed successfully.")

        } catch (e: Exception) {
            Timber.e(e, "Error during history sync with Firestore.")
            // Optionally, rethrow or handle specific exceptions for UI feedback
        }
    }

    /**
     * Gets the current Firebase user's ID. If no user is signed in, attempts to sign in anonymously.
     *
     * @return The Firebase user ID, or null if sign-in fails.
     */
    private suspend fun getCurrentUserIdOrSignInAnonymously(): String? {
        return firebaseAuth.currentUser?.uid ?: try {
            Timber.d("No Firebase user found, attempting anonymous sign-in...")
            val authResult = firebaseAuth.signInAnonymously().await()
            Timber.i("Anonymous sign-in successful. User ID: %s", authResult.user?.uid)
            authResult.user?.uid
        } catch (e: Exception) {
            Timber.e(e, "Anonymous sign-in failed.")
            null
        }
    }

    /**
     * Fetches the last [HISTORY_LIMIT] history entries from Firestore for the given user ID.
     *
     * @param userId The ID of the user whose history to fetch.
     * @return A list of [HistoryEntity] objects from Firestore.
     */
    private suspend fun fetchFirestoreHistory(userId: String): List<HistoryEntity> {
        return try {
            val querySnapshot = firestore.collection(FIRESTORE_COLLECTION_PATH)
                .whereEqualTo(FIRESTORE_USER_ID_FIELD, userId)
                .orderBy(FIRESTORE_TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .limit(HISTORY_LIMIT.toLong())
                .get()
                .await()
            querySnapshot.documents.mapNotNull { doc ->
                // Construct HistoryEntity, assuming 'id' from Firestore doc ID or a specific field if you stored it
                // For simplicity, we generate a new local ID if needed, or rely on Room's autoGenerate on insert.
                // The crucial parts are expression, result, and timestamp for merging.
                HistoryEntity(
                    // id = 0, // Let Room auto-generate on insert if not mapping Firestore doc ID directly
                    expression = doc.getString(FIRESTORE_EXPRESSION_FIELD) ?: "",
                    result = doc.getString(FIRESTORE_RESULT_FIELD) ?: "",
                    timestamp = doc.getLong(FIRESTORE_TIMESTAMP_FIELD) ?: 0L
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching history from Firestore for user: %s", userId)
            emptyList()
        }
    }

    /**
     * Clears all Firestore history entries for a specific user.
     *
     * @param userId The ID of the user whose Firestore history to clear.
     */
    private suspend fun clearFirestoreHistoryForUser(userId: String) {
        try {
            val querySnapshot = firestore.collection(FIRESTORE_COLLECTION_PATH)
                .whereEqualTo(FIRESTORE_USER_ID_FIELD, userId)
                .get()
                .await()
            firestore.runBatch { batch ->
                querySnapshot.documents.forEach { batch.delete(it.reference) }
            }.await()
            Timber.d("Cleared Firestore history for user %s before re-upload.", userId)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing Firestore history for user: %s", userId)
        }
    }

    /**
     * Uploads a list of [HistoryEntity] objects to Firestore for the given user ID.
     *
     * @param userId The ID of the user.
     * @param entries The list of [HistoryEntity] to upload.
     */
    private suspend fun uploadToFirestore(userId: String, entries: List<HistoryEntity>) {
        try {
            firestore.runBatch { batch ->
                entries.forEach { entity ->
                    val docRef = firestore.collection(FIRESTORE_COLLECTION_PATH).document() // Create new doc for each
                    val firestoreEntry = hashMapOf(
                        FIRESTORE_USER_ID_FIELD to userId,
                        FIRESTORE_EXPRESSION_FIELD to entity.expression,
                        FIRESTORE_RESULT_FIELD to entity.result,
                        FIRESTORE_TIMESTAMP_FIELD to entity.timestamp
                    )
                    batch.set(docRef, firestoreEntry)
                }
            }.await()
            Timber.d("Uploaded %d entries to Firestore for user %s.", entries.size, userId)
        } catch (e: Exception) {
            Timber.e(e, "Error uploading history to Firestore for user: %s", userId)
        }
    }

    /**
     * Prunes Firestore history for the given user to keep only the latest [HISTORY_LIMIT] entries.
     *
     * @param userId The ID of the user whose Firestore history to prune.
     */
    private suspend fun pruneFirestoreHistory(userId: String) {
        try {
            val querySnapshot = firestore.collection(FIRESTORE_COLLECTION_PATH)
                .whereEqualTo(FIRESTORE_USER_ID_FIELD, userId)
                .orderBy(FIRESTORE_TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .get()
                .await()

            if (querySnapshot.size() > HISTORY_LIMIT) {
                val batch = firestore.batch()
                querySnapshot.documents.drop(HISTORY_LIMIT).forEach {
                    batch.delete(it.reference)
                }
                batch.commit().await()
                Timber.d("Pruned Firestore history for user %s. Kept %d, deleted %d.", userId, HISTORY_LIMIT, querySnapshot.size() - HISTORY_LIMIT)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pruning Firestore history for user: %s", userId)
        }
    }
}
