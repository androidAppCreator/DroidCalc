package com.droid.droidcalc.ui.split.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droid.droidcalc.R
import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm
import com.droid.droidcalc.ui.split.SplitContract
import com.droid.droidcalc.ui.split.viewmodel.SplitViewModel
import com.droid.droidcalc.ui.theme.CalcTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// Constants for padding and spacing within SplitScreen, aligned with new UI.
private object SplitScreenDimens {
    val ScreenPadding = 16.dp
    val SectionSpacing = 16.dp
    val ItemSpacing = 8.dp
    val StickyButtonHeight = 72.dp // Height for the area reserved for the sticky button
    val BottomNavHeight = 80.dp // Approximate height for bottom navigation bar if present
    val ParticipantCardVerticalSpacing = 8.dp
}

/**
 * The main composable entry point for the Split Bill Screen feature, updated to the new UI design.
 * This screen observes UI state from [SplitViewModel] and dispatches user intents for processing.
 * It adheres to MVI principles by remaining stateless and delegating logic to the ViewModel.
 *
 * @param viewModel The [SplitViewModel] instance for this screen, typically injected by Hilt.
 * @param onNavigateBack Callback invoked when a back navigation action is requested.
 * @param onShareClick Callback invoked when the share action is triggered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    viewModel: SplitViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onShareClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showAddFriendSheet by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SplitContract.UiEffect.ShowSnackbar -> {
                    keyboardController?.hide()
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SplitScreenTopAppBar(
                onNavigateBack = onNavigateBack,
                onShareClick = onShareClick // Pass the onShareClick directly
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SplitScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onIntent = viewModel::processIntent,
            onAddFriendClick = { showAddFriendSheet = true }
        )
    }

    if (showAddFriendSheet) {
        AddFriendSheet(
            onDismissRequest = { showAddFriendSheet = false },
            onAddParticipant = { newParticipantData, addAnother ->
                viewModel.processIntent(
                    SplitContract.SplitIntent.AddNewParticipant(newParticipantData)
                )
                if (!addAnother) {
                    showAddFriendSheet = false
                }
            }
        )
    }
}

/**
 * Composable for the Top App Bar of the Split Screen, matching the new UI.
 * Title is "Split the bill".
 *
 * @param onNavigateBack Lambda invoked when the navigation icon (back arrow) is clicked.
 * @param onShareClick Lambda invoked when the share action icon (upload icon) is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitScreenTopAppBar(
    onNavigateBack: () -> Unit,
    onShareClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.split_screen_title_new_ui), // "Split the bill"
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal) // As per UI
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.split_back_action_desc)
                )
            }
        },
        actions = {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.split_share_action_desc_new_ui)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Main content layout for the SplitScreen, updated for the new UI design.
 * It features a header card, algorithm selector, a list of participants, and a sticky "Continue" button.
 *
 * @param modifier Modifier for this composable, typically including padding from the Scaffold.
 * @param uiState The current [SplitContract.SplitScreenState] to render the UI from.
 * @param onIntent Lambda to send [SplitContract.SplitIntent] to the ViewModel for processing user actions.
 * @param onAddFriendClick Lambda to trigger the display of the "Add Friend" modal bottom sheet.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SplitScreenContent(
    modifier: Modifier = Modifier,
    uiState: SplitContract.SplitScreenState,
    onIntent: (SplitContract.SplitIntent) -> Unit,
    onAddFriendClick: () -> Unit
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = SplitScreenDimens.ScreenPadding,
                bottom = SplitScreenDimens.StickyButtonHeight + SplitScreenDimens.ScreenPadding
            ),
            verticalArrangement = Arrangement.spacedBy(SplitScreenDimens.ItemSpacing) // Reduced for denser packing of items
        ) {
            // Section 1: New Header Card
            item {
                SplitHeaderCard(
                    merchantName = uiState.merchantInfo?.name
                        ?: stringResource(R.string.split_merchant_name_placeholder),
                    transactionDate = uiState.merchantInfo?.date ?: "",
                    totalAmount = uiState.initialTotalAmount,
                    amountLeftToSplit = uiState.amountLeftToSplit ?: BigDecimal.ZERO,
                    currencyFormatter = currencyFormatter,
                    donationMessage = uiState.merchantInfo?.donationNote,
                    modifier = Modifier.padding(horizontal = SplitScreenDimens.ScreenPadding)
                )
            }

            // Section 2: "Custom split" Header, Algorithm Selector and "Add a friend" Action Button
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = SplitScreenDimens.ScreenPadding)
                        .padding(top = SplitScreenDimens.SectionSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.split_custom_split_header_new_ui),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = onAddFriendClick) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.split_add_friend_action_desc_new_ui),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(SplitScreenDimens.ItemSpacing))
                            Text(
                                text = stringResource(R.string.split_add_friend_action_new_ui),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(SplitScreenDimens.ItemSpacing)) // Space before dropdown
                    AlgorithmSelectorDropDown(
                        selectedAlgorithm = uiState.selectedAlgorithm,
                        onAlgorithmSelected = { algorithm ->
                            onIntent(SplitContract.SplitIntent.SelectAlgorithm(algorithm))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Spacer item for visual separation before participant list
            item {
                Spacer(modifier = Modifier.height(SplitScreenDimens.ItemSpacing))
            }

            // Section 3: Participants List using the new ParticipantCard
            itemsIndexed(
                items = uiState.participants,
                key = { _, participant -> participant.id }
            ) { _, participant ->
                val formattedShare = uiState.splitResult?.get(participant.id)?.let {
                    val formatted = currencyFormatter.format(it)
                    // Assuming USD for now, should be dynamic based on locale/currency settings
                    if (!formatted.contains("USD")) "$formatted USD" else formatted
                } ?: stringResource(R.string.split_zero_amount_usd)

                val percentageShareText = uiState.participantPercentages[participant.id] ?: "0%"
                val cardColor = uiState.participantCardColors[participant.id]
                    ?: MaterialTheme.colorScheme.surfaceVariant

                ParticipantCard(
                    participant = participant,
                    formattedShare = formattedShare,
                    percentageShareText = percentageShareText,
                    cardColor = cardColor,
                    onIntent = onIntent,
                    modifier = Modifier
                        .padding(horizontal = SplitScreenDimens.ScreenPadding)
                        .padding(vertical = SplitScreenDimens.ParticipantCardVerticalSpacing / 2)
                )
            }
        }

        // Sticky "Continue" Button at the bottom of the screen
        Button(
            onClick = { onIntent(SplitContract.SplitIntent.CalculateSplit) },
            enabled = !uiState.isLoading && uiState.participants.isNotEmpty() && uiState.initialTotalAmount > BigDecimal.ZERO,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(SplitScreenDimens.ScreenPadding)
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.split_continue_button_new_ui),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * A composable that provides a dropdown menu for selecting a [SplitAlgorithm].
 * It displays the currently selected algorithm and allows the user to choose a different one from the list.
 *
 * @param selectedAlgorithm The currently selected [SplitAlgorithm].
 * @param onAlgorithmSelected Lambda function invoked when a new algorithm is selected by the user.
 * @param modifier Modifier for this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlgorithmSelectorDropDown(
    selectedAlgorithm: SplitAlgorithm,
    onAlgorithmSelected: (SplitAlgorithm) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val algorithms = remember { SplitAlgorithm.entries.toList() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = stringResource(id = selectedAlgorithm.displayNameResId),
            onValueChange = {}, // Read-only
            readOnly = true,
            label = { Text(stringResource(R.string.split_screen_algorithm_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor() // Important for proper positioning of the dropdown
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            algorithms.forEach { algorithm ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = algorithm.displayNameResId)) },
                    onClick = {
                        onAlgorithmSelected(algorithm)
                        expanded = false
                    }
                )
            }
        }
    }
}


// --- Previews for the new SplitScreenContent ---
@Preview(showBackground = true, name = "Split Screen Content - New UI Light")
@Composable
fun SplitScreenContentNewUILight() {
    CalcTheme(darkTheme = false) {
        val sampleParticipants = listOf(
            Participant(
                id = "0",
                name = "You",
                isYou = true,
                isPaid = true,
                share = BigDecimal("8.00")
            ),
            Participant(id = "1", name = "Samantha W.", isPaid = true, share = BigDecimal("20.00")),
            Participant(id = "2", name = "Jonathan D.", isPaid = true, share = BigDecimal("12.80")),
            Participant(id = "3", name = "Pandi Gembel", isPaid = false, share = BigDecimal("0.00"))
        )
        val sampleSplitResult =
            sampleParticipants.associate { it.id to (it.share ?: BigDecimal.ZERO) }
        val samplePercentages = mapOf("0" to "20%", "1" to "50%", "2" to "30%", "3" to "0%")
        val sampleCardColors = mapOf(
            "0" to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            "1" to Color(0xFFFDE7E9),
            "2" to MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            "3" to Color(0xFFE7F0FD)
        )

        val sampleUiState = SplitContract.SplitScreenState(
            initialTotalAmount = BigDecimal("40.80"),
            totalAmountInput = "40.80",
            participants = sampleParticipants,
            selectedAlgorithm = SplitAlgorithm.EQUAL,
            splitResult = sampleSplitResult,
            participantPercentages = samplePercentages,
            participantCardColors = sampleCardColors,
            merchantInfo = SplitContract.MerchantInfo(
                name = "Burger Gembel",
                date = "22 Jun 2023",
                donationNote = "Burger Gembel sends 2.99 USD for nature conservation"
            ),
            amountLeftToSplit = BigDecimal.ZERO,
            isLoading = false,
            errorMessages = emptyMap()
        )
        SplitScreenContent(
            uiState = sampleUiState,
            onIntent = {},
            onAddFriendClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Split Screen Content - New UI Dark")
@Composable
fun SplitScreenContentNewUIDark() {
    CalcTheme(darkTheme = true) {
        val sampleParticipants = listOf(
            Participant(
                id = "0",
                name = "You",
                isYou = true,
                isPaid = true,
                share = BigDecimal("8.00")
            ),
            Participant(id = "1", name = "Samantha W.", isPaid = true, share = BigDecimal("20.00")),
            Participant(id = "2", name = "Jonathan D.", isPaid = true, share = BigDecimal("12.80")),
            Participant(id = "3", name = "Pandi Gembel", isPaid = false, share = BigDecimal("0.00"))
        )
        val sampleSplitResult =
            sampleParticipants.associate { it.id to (it.share ?: BigDecimal.ZERO) }
        val samplePercentages = mapOf("0" to "20%", "1" to "50%", "2" to "30%", "3" to "0%")
        val sampleCardColors = mapOf(
            "0" to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            "1" to Color(0xFF6F2020),
            "2" to MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            "3" to Color(0xFF20306F)
        )
        val sampleUiState = SplitContract.SplitScreenState(
            initialTotalAmount = BigDecimal("40.80"),
            totalAmountInput = "40.80",
            participants = sampleParticipants,
            selectedAlgorithm = SplitAlgorithm.PERCENTAGE,
            splitResult = sampleSplitResult,
            participantPercentages = samplePercentages,
            participantCardColors = sampleCardColors,
            merchantInfo = SplitContract.MerchantInfo(
                name = "Burger Gembel",
                date = "22 Jun 2023",
                donationNote = "Burger Gembel sends 2.99 USD for nature conservation"
            ),
            amountLeftToSplit = BigDecimal("10.00"),
            isLoading = false,
            errorMessages = emptyMap()
        )
        SplitScreenContent(
            uiState = sampleUiState,
            onIntent = {},
            onAddFriendClick = {}
        )
    }
}
