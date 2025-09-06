package com.droid.droidcalc.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox // Correct Material 3 import
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
// import androidx.compose.ui.platform.LocalContext // Removed as it was marked unused and not planned for use
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droid.droidcalc.R
import com.droid.droidcalc.data.db.HistoryEntity
import com.droid.droidcalc.ui.history.intent.HistoryIntent
import com.droid.droidcalc.ui.history.state.HistoryState
import com.droid.droidcalc.ui.history.viewmodel.HistoryViewModel
import com.droid.droidcalc.ui.theme.CalcTheme
import com.droid.droidcalc.ui.theme.MotionTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Padding constants used throughout the History screen.
 * @author DroidSwap
 */
private object HistoryScreenPaddings {
    val Screen = 16.dp
    val InterItem = 8.dp
    val IntraItem = 4.dp
    val SwipeToDismissItemVertical = 1.dp
    val SwipeDismissBackgroundHorizontal = 20.dp
    val SyncButtonHorizontal = 16.dp
    val SyncButtonVertical = 8.dp
}

/**
 * Dimension constants used throughout the History screen.
 * @author DroidSwap
 */
private object HistoryScreenDimens {
    val HistoryItemCardElevation = 2.dp
    val SyncButtonProgressSize = 20.dp
    val SyncButtonProgressStrokeWidth = 2.dp
    val SwipeDismissIconScaleDefault = 0.75f
    val SwipeDismissIconScaleDismissed = 1.0f
}

/**
 * Text-related constants used on the History screen.
 * @author DroidSwap
 */
private object HistoryScreenTexts {
    const val ExpressionMaxLines = 2
    const val DateFormatPattern = "MMM dd, yyyy, HH:mm"
}

/**
 * Animation-related constants used on the History screen.
 * @author DroidSwap
 */
private object HistoryScreenAnimationValues {
    const val DismissBackgroundDefaultAlpha = 0.5f
}

/**
 * Main composable for the History screen.
 * This screen displays a list of past calculations, allows users to delete entries,
 * clear all history, and manually trigger a sync with Firestore.
 * It observes state from [HistoryViewModel] and dispatches user intents.
 *
 * @param viewModel The [HistoryViewModel] instance for this screen, injected by Hilt.
 * @param snackbarHostState The [SnackbarHostState] from the Scaffold to show messages.
 * @author DroidSwap
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            // viewModel.processIntent(HistoryIntent.ClearError) // Example of how to clear error
        }
    }

    HistoryScreenContent(
        uiState = uiState,
        onIntent = viewModel::processIntent
    )
}

/**
 * The stateless content composable for the History screen.
 * Defines the UI structure based on the provided [HistoryState] and dispatches [HistoryIntent]s.
 *
 * @param uiState The current [HistoryState] to render.
 * @param onIntent Callback to dispatch a [HistoryIntent] to the ViewModel.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HistoryScreenContent(
    uiState: HistoryState,
    onIntent: (HistoryIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HistoryScreenPaddings.Screen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = HistoryScreenPaddings.InterItem),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.calculation_history_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            SyncButton(isLoading = uiState.isLoading) {
                onIntent(HistoryIntent.SyncHistory)
            }
        }

        if (uiState.isLoading && uiState.historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_history_message), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HistoryScreenPaddings.InterItem)
            ) {
                items(uiState.historyItems, key = { it.id }) { item ->
                    val currentItem by rememberUpdatedState(item)

                    // Use new API
                    val dismissState = rememberSwipeToDismissBoxState(
                        initialValue = SwipeToDismissBoxValue.Settled,
                        confirmValueChange = { newValue ->
                            if (newValue == SwipeToDismissBoxValue.EndToStart ||
                                newValue == SwipeToDismissBoxValue.StartToEnd
                            ) {
                                onIntent(HistoryIntent.DeleteHistoryItem(currentItem.id))
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier
                            .padding(vertical = HistoryScreenPaddings.SwipeToDismissItemVertical)
                            .animateItem(
                                fadeInSpec = null,
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                fadeOutSpec = tween(durationMillis = MotionTokens.MediumDuration)
                            ),
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = { SwipeDismissBackground(dismissState = dismissState) }
                    ) {
                        HistoryItemCard(historyEntity = item)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HistoryScreenPaddings.Screen),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = { onIntent(HistoryIntent.ClearAllHistory) },
                enabled = uiState.historyItems.isNotEmpty() && !uiState.isLoading
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_all_history_desc), modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.clear_all_button))
            }
        }
    }
}

/**
 * Composable for the "Sync Now" button with an inline progress indicator.
 *
 * @param isLoading True if sync is in progress, false otherwise.
 * @param onClick Lambda to be invoked when the button is clicked.
 * @author DroidSwap
 */
@Composable
private fun SyncButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        contentPadding = PaddingValues(horizontal = HistoryScreenPaddings.SyncButtonHorizontal, vertical = HistoryScreenPaddings.SyncButtonVertical)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(HistoryScreenDimens.SyncButtonProgressSize),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = HistoryScreenDimens.SyncButtonProgressStrokeWidth
            )
        } else {
            Icon(
                Icons.Filled.Info,
                contentDescription = stringResource(R.string.sync_history_desc),
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.sync_now_button))
    }
}

/**
 * Composable for rendering a single history item in a Card.
 *
 * @param historyEntity The [HistoryEntity] to display.
 * @author DroidSwap
 */
@Composable
private fun HistoryItemCard(historyEntity: HistoryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = HistoryScreenDimens.HistoryItemCardElevation)
    ) {
        Column(
            modifier = Modifier
                .padding(HistoryScreenPaddings.Screen)
                .fillMaxWidth()
        ) {
            Text(
                text = historyEntity.expression,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = HistoryScreenTexts.ExpressionMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "= ${historyEntity.result}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = HistoryScreenPaddings.IntraItem)
            )
            Text(
                text = formatDate(historyEntity.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = HistoryScreenPaddings.InterItem)
            )
        }
    }
}

/**
 * Composable for the background shown during swipe-to-dismiss action.
 * This composable is used as the `backgroundContent` for [SwipeToDismissBox].
 *
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDismissBackground(dismissState: SwipeToDismissBoxState) {
    val color by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled ->
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = HistoryScreenAnimationValues.DismissBackgroundDefaultAlpha
                )
            SwipeToDismissBoxValue.StartToEnd,
            SwipeToDismissBoxValue.EndToStart ->
                MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(durationMillis = MotionTokens.ShortDuration),
        label = "swipeDismissBackgroundColor"
    )

    val alignment = when (dismissState.targetValue) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    val icon = Icons.Default.Delete

    val scale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
            HistoryScreenDimens.SwipeDismissIconScaleDefault
        } else {
            HistoryScreenDimens.SwipeDismissIconScaleDismissed
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipeDismissIconScale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = HistoryScreenPaddings.SwipeDismissBackgroundHorizontal),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.delete_item_desc),
            modifier = Modifier.scale(scale),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * Helper function to format a timestamp into a human-readable date string.
 *
 * @param timestamp The timestamp in milliseconds since epoch.
 * @return A formatted date string (e.g., "MMM dd, yyyy, HH:mm").
 * @author DroidSwap
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat(HistoryScreenTexts.DateFormatPattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Preview composable for [HistoryScreenContent] with sample data.
 * @author DroidSwap
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun HistoryScreenContentPreview() {
    CalcTheme {
        val sampleHistory = listOf(
            HistoryEntity(1, "2+2", "4", System.currentTimeMillis() - 100000),
            HistoryEntity(2, "10/2*5", "25", System.currentTimeMillis() - 200000),
            HistoryEntity(3, "12345*67890", "838102050", System.currentTimeMillis() - 300000)
        )
        HistoryScreenContent(
            uiState = HistoryState(historyItems = sampleHistory),
            onIntent = {}
        )
    }
}

/**
 * Preview composable for [HistoryScreenContent] in a loading state.
 * @author DroidSwap
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun HistoryScreenContentLoadingPreview() {
    CalcTheme {
        HistoryScreenContent(
            uiState = HistoryState(isLoading = true),
            onIntent = {}
        )
    }
}

/**
 * Preview composable for [HistoryScreenContent] when no history is available.
 * @author DroidSwap
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun HistoryScreenContentEmptyPreview() {
    CalcTheme {
        HistoryScreenContent(
            uiState = HistoryState(historyItems = emptyList()),
            onIntent = {}
        )
    }
}
