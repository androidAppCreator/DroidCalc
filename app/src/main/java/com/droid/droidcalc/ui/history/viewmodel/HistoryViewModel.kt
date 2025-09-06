package com.droid.droidcalc.ui.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.droidcalc.domain.repository.HistoryRepository
import com.droid.droidcalc.ui.history.intent.HistoryIntent
import com.droid.droidcalc.ui.history.state.HistoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 * This class is responsible for handling user intents related to the calculation history,
 * interacting with the [HistoryRepository] to fetch, delete, or sync history data,
 * and exposing the UI state ([HistoryState]) to be observed by the UI layer.
 * It follows the MVI (Model-View-Intent) architecture pattern.
 *
 * @property historyRepository The repository for managing calculation history.
 * @author DroidSwap
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryState())
    /**
     * The [StateFlow] emitting the current [HistoryState] of the history screen.
     * UI composables should collect this flow to react to state changes.
     */
    val uiState: StateFlow<HistoryState> = _uiState.asStateFlow()

    init {
        // Load history when the ViewModel is created
        processIntent(HistoryIntent.LoadHistory)
    }

    /**
     * Processes the given [HistoryIntent] to update the history screen's state.
     * This function is the entry point for all user interactions from the UI related to history.
     *
     * @param intent The [HistoryIntent] representing the user's action.
     */
    fun processIntent(intent: HistoryIntent) {
        Timber.d("Processing intent: %s", intent)
        when (intent) {
            HistoryIntent.LoadHistory -> loadHistory()
            is HistoryIntent.DeleteHistoryItem -> deleteHistoryItem(intent.itemId)
            HistoryIntent.ClearAllHistory -> clearAllHistory()
            HistoryIntent.SyncHistory -> syncHistory()
        }
    }

    /**
     * Loads the calculation history from the repository and updates the UI state.
     * Collects the Flow from the repository to get real-time updates.
     */
    private fun loadHistory() {
        viewModelScope.launch {
            historyRepository.getHistory()
                .onStart { 
                    _uiState.update { it.copy(isLoading = true, error = null) } 
                    Timber.d("Loading history...")
                }
                .catch { e ->
                    Timber.e(e, "Error loading history from repository.")
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load history") }
                }
                .collect { historyItems ->
                    _uiState.update {
                        it.copy(historyItems = historyItems, isLoading = false, error = null)
                    }
                    Timber.d("History loaded: %d items", historyItems.size)
                }
        }
    }

    /**
     * Deletes a specific history item by its ID.
     *
     * @param itemId The ID of the history item to delete.
     */
    private fun deleteHistoryItem(itemId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) } // Optional: show loading for delete
                historyRepository.deleteHistoryEntry(itemId)
                // The flow from getHistory() will automatically update the list if deletion is successful.
                // No need to manually update _uiState.historyItems here if getHistory() emits on change.
                Timber.i("Deleted history item with ID: %s", itemId)
                // If getHistory() doesn't immediately reflect the change, might need a manual refresh or update.
                // For robustness, could explicitly re-fetch or assume Flow updates.
            } catch (e: Exception) {
                Timber.e(e, "Error deleting history item with ID: %s", itemId)
                _uiState.update { it.copy(error = e.message ?: "Failed to delete item", isLoading = false) }
            } finally {
                 // isLoading might be set to false by the collecting flow if data updates quickly
                // _uiState.update { it.copy(isLoading = false) } // Ensure loading is false
            }
        }
    }

    /**
     * Clears all calculation history.
     */
    private fun clearAllHistory() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                historyRepository.clearAllHistory()
                // Flow will update the list to be empty.
                Timber.i("Cleared all history.")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing all history.")
                _uiState.update { it.copy(error = e.message ?: "Failed to clear history", isLoading = false) }
            } finally {
                // _uiState.update { it.copy(isLoading = false) } // Ensure loading is false
            }
        }
    }

    /**
     * Triggers a synchronization of the history with Firestore.
     */
    private fun syncHistory() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                Timber.d("Manual sync triggered.")
                historyRepository.syncHistoryWithFirestore()
                // The history Flow should automatically update with synced data.
                // If not, a manual refresh call to loadHistory() might be needed, 
                // but ideally the repository flow handles this.
                Timber.i("History synchronization successful.")
            } catch (e: Exception) {
                Timber.e(e, "Error during manual history synchronization.")
                _uiState.update { it.copy(error = e.message ?: "Sync failed", isLoading = false) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
