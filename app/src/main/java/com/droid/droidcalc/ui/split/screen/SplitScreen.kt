/**
 * This file defines the SplitScreen composable, which is the main UI for the bill splitting feature.
 * It allows users to input bill details, manage participants, select a splitting algorithm,
 * and view the calculated shares. It follows MVI principles, observing state from [SplitViewModel]
 * and delegating user actions to it via [SplitContract.SplitIntent].
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.split.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droid.droidcalc.R // Assuming R class will be generated for string resources
import com.droid.droidcalc.domain.model.Participant
import com.droid.droidcalc.domain.usecase.SplitAlgorithm
import com.droid.droidcalc.ui.split.SplitContract
import com.droid.droidcalc.ui.split.viewmodel.SplitViewModel
import com.droid.droidcalc.ui.theme.CalcTheme
import java.math.BigDecimal
import java.text.NumberFormat

/**
 * The main composable for the Split Screen.
 * It observes [SplitContract.SplitScreenState] and dispatches [SplitContract.SplitIntent] to [SplitViewModel].
 * It also handles [SplitContract.UiEffect] for showing Snackbars.
 *
 * @param viewModel The [SplitViewModel] instance for this screen.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    viewModel: SplitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UiEffects from the ViewModel
    LaunchedEffect(key1 = viewModel) { // Or key1 = Unit if viewModel instance doesn't change
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SplitContract.UiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message, // ViewModel should resolve this from string resources ideally
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // MAX_PARTICIPANTS is 10, defined in ViewModel. Consider moving this logic to ViewModel state.
            if (uiState.participants.size < 10) { 
                FloatingActionButton(onClick = { viewModel.processIntent(SplitContract.SplitIntent.AddParticipant) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.split_add_participant_desc)) // Assuming R.string.split_add_participant_desc exists
                }
            }
        }
    ) { paddingValues ->
        SplitScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onIntent = viewModel::processIntent
        )
    }
}

/**
 * Content composable for the SplitScreen.
 * Displays input fields, participant list, algorithm selection, and results.
 * All user interactions are sent as [SplitContract.SplitIntent] via the [onIntent] lambda.
 * Note: Uses hardcoded dp values; recommend using dimensionResource for full guideline adherence.
 *
 * @param modifier Modifier for this composable.
 * @param uiState The current [SplitContract.SplitScreenState] to render.
 * @param onIntent Lambda to send [SplitContract.SplitIntent] to the ViewModel.
 * @author DroidSwap
 */
@Composable
fun SplitScreenContent(
    modifier: Modifier = Modifier,
    uiState: SplitContract.SplitScreenState,
    onIntent: (SplitContract.SplitIntent) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Example of hardcoded dp, ideally dimensionResource
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                // Assuming R.string.split_initial_total_label is like "Initial Total: %1$s"
                text = stringResource(R.string.split_initial_total_label, uiState.initialTotalAmount.toPlainString()), 
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.tipAmountInput,
                    onValueChange = { onIntent(SplitContract.SplitIntent.UpdateTipAmount(it)) },
                    label = { Text(stringResource(R.string.split_tip_label)) }, // Assuming R.string.split_tip_label exists
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                    isError = uiState.errorMessages.containsKey("tip")
                )
                OutlinedTextField(
                    value = uiState.taxAmountInput,
                    onValueChange = { onIntent(SplitContract.SplitIntent.UpdateTaxAmount(it)) },
                    label = { Text(stringResource(R.string.split_tax_label)) }, // Assuming R.string.split_tax_label exists
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                    isError = uiState.errorMessages.containsKey("tax")
                )
            }
            uiState.errorMessages["tip"]?.let { ErrorText(it) } // Inline error messages
            uiState.errorMessages["tax"]?.let { ErrorText(it) } // Inline error messages
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            AlgorithmSelector(uiState.selectedAlgorithm) { algorithm ->
                onIntent(SplitContract.SplitIntent.SelectAlgorithm(algorithm))
            }
            uiState.errorMessages["percentageSum"]?.let { ErrorText(it) }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = stringResource(R.string.split_participants_header), // Assuming R.string.split_participants_header exists
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        itemsIndexed(uiState.participants, key = { _, p -> "p-${p.id}" }) { index, participant ->
            // Removed non-functional AnimatedVisibility(visible = true)
            ParticipantInputCard(
                participant = participant,
                algorithm = uiState.selectedAlgorithm,
                onIntent = onIntent,
                // Min participants is 2, from ViewModel. Consider moving logic to ViewModel state.
                canRemove = uiState.participants.size > 2, 
                errorMessages = uiState.errorMessages.filterKeys { it.startsWith("p${index}") }
            )
            Spacer(Modifier.height(8.dp))
        }
        
        item {
            // This error key "participants" was previously used for Snackbar, now potentially for inline if needed
            // uiState.errorMessages["participants"]?.let { ErrorText(it) }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = { onIntent(SplitContract.SplitIntent.CalculateSplit) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.split_calculate_button)) // Assuming R.string.split_calculate_button exists
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.splitResult != null && !uiState.isLoading) {
            item {
                Text(
                    text = stringResource(R.string.split_results_header), // Assuming R.string.split_results_header exists
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                uiState.finalTotalToSplit?.let {
                     Text(
                        // Assuming R.string.split_total_to_split_label is like "Total to Split: %1$s"
                        text = stringResource(R.string.split_total_to_split_label, formatCurrency(it)), 
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
            itemsIndexed(uiState.splitResult.toList(), key = {_, item -> "r-${item.first}" }) { _, (participantId, amount) ->
                val participantName = uiState.participants.find { it.id == participantId }?.name ?: stringResource(R.string.split_unknown_participant) // Assuming R.string.split_unknown_participant exists
                SplitResultItem(participantName = participantName, share = amount)
                Divider()
            }
            item {
                IconButton(onClick = { onIntent(SplitContract.SplitIntent.ToggleRoundingExplanation(true)) }) {
                    Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.split_rounding_info_desc)) // Assuming R.string.split_rounding_info_desc exists
                }
            }
        }
        
        if (uiState.showRoundingExplanation) {
            item {
                AlertDialog(
                    onDismissRequest = { onIntent(SplitContract.SplitIntent.ToggleRoundingExplanation(false)) },
                    title = { Text(stringResource(R.string.split_rounding_title)) }, // Assuming R.string.split_rounding_title exists
                    text = { Text(stringResource(R.string.split_rounding_details)) }, // Assuming R.string.split_rounding_details exists
                    confirmButton = {
                        TextButton(onClick = { onIntent(SplitContract.SplitIntent.ToggleRoundingExplanation(false)) }) {
                            Text(stringResource(R.string.split_ok_button)) // Assuming R.string.split_ok_button exists
                        }
                    }
                )
            }
        }
         item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
    }
}

/**
 * Composable for selecting the split algorithm.
 * Display names for algorithms are generated from enum names. For i18n, map enums to string resources.
 *
 * @param selectedAlgorithm The currently selected algorithm.
 * @param onAlgorithmSelected Lambda called when an algorithm is selected.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmSelector(
    selectedAlgorithm: SplitAlgorithm,
    onAlgorithmSelected: (SplitAlgorithm) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val algorithms = SplitAlgorithm.values()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedAlgorithm.name.replace("_", " ").lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, // This formatting is for display
            onValueChange = {}, 
            readOnly = true,
            label = { Text(stringResource(R.string.split_algorithm_label)) }, // Assuming R.string.split_algorithm_label exists
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            algorithms.forEach { algorithm ->
                DropdownMenuItem(
                    text = { Text(algorithm.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                    onClick = {
                        onAlgorithmSelected(algorithm)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Composable card for a single participant's input fields.
 *
 * @param participant The participant data.
 * @param algorithm The currently selected split algorithm, to show relevant fields.
 * @param onIntent Lambda to send [SplitContract.SplitIntent] to the ViewModel.
 * @param canRemove Whether the remove button should be enabled.
 * @param errorMessages Map of error messages relevant to this participant (for inline display).
 * @author DroidSwap
 */
@Composable
fun ParticipantInputCard(
    participant: Participant,
    algorithm: SplitAlgorithm,
    onIntent: (SplitContract.SplitIntent) -> Unit,
    canRemove: Boolean,
    errorMessages: Map<String, String>
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = participant.name,
                    onValueChange = { newName -> onIntent(SplitContract.SplitIntent.UpdateParticipantField(participant.id) { it.copy(name = newName) }) },
                    label = { Text(stringResource(R.string.split_participant_name_label)) }, // Assuming R.string.split_participant_name_label exists
                    modifier = Modifier.weight(1f),
                    isError = errorMessages.containsKey("name")
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onIntent(SplitContract.SplitIntent.RemoveParticipant(participant.id)) }, enabled = canRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.split_remove_participant_desc)) // Assuming R.string.split_remove_participant_desc exists
                }
            }
            errorMessages["name"]?.let { ErrorText(it) }

            val onFieldUpdate = { updatedParticipant: (Participant) -> Participant ->
                onIntent(SplitContract.SplitIntent.UpdateParticipantField(participant.id, updatedParticipant))
            }

            when (algorithm) {
                SplitAlgorithm.FIXED_THEN_EQUAL -> {
                    OutlinedTextField(
                        value = participant.fixedAmountInput,
                        onValueChange = { newVal -> onFieldUpdate { it.copy(fixedAmountInput = newVal) } },
                        label = { Text(stringResource(R.string.split_fixed_amount_label)) }, // Assuming R.string.split_fixed_amount_label exists
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessages.containsKey("fixed")
                    )
                    errorMessages["fixed"]?.let { ErrorText(it) }
                }
                SplitAlgorithm.PERCENTAGE -> {
                    OutlinedTextField(
                        value = participant.percentageInput,
                        onValueChange = { newVal -> onFieldUpdate { it.copy(percentageInput = newVal) } },
                        label = { Text(stringResource(R.string.split_percentage_label)) }, // Assuming R.string.split_percentage_label exists
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessages.containsKey("percentage")
                    )
                    errorMessages["percentage"]?.let { ErrorText(it) }
                }
                SplitAlgorithm.WEIGHTED -> {
                    OutlinedTextField(
                        value = participant.weightInput,
                        onValueChange = { newVal -> onFieldUpdate { it.copy(weightInput = newVal) } },
                        label = { Text(stringResource(R.string.split_weight_label)) }, // Assuming R.string.split_weight_label exists
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessages.containsKey("weight")
                    )
                     errorMessages["weight"]?.let { ErrorText(it) }
                }
                SplitAlgorithm.RATIO -> {
                     OutlinedTextField(
                        value = participant.ratioInput,
                        onValueChange = { newVal -> onFieldUpdate { it.copy(ratioInput = newVal) } },
                        label = { Text(stringResource(R.string.split_ratio_label)) }, // Assuming R.string.split_ratio_label exists
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessages.containsKey("ratio")
                    )
                    errorMessages["ratio"]?.let { ErrorText(it) }
                }
                SplitAlgorithm.EQUAL -> { /* No specific field needed */ }
            }
        }
    }
}

/**
 * Composable to display a single participant's share in the results.
 *
 * @param participantName Name of the participant.
 * @param share Calculated share for the participant.
 * @author DroidSwap
 */
@Composable
fun SplitResultItem(participantName: String, share: BigDecimal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(participantName, style = MaterialTheme.typography.bodyLarge)
        Text(formatCurrency(share), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Simple composable to display an error message text for inline field errors.
 * These messages originate from UiState.errorMessages and should be user-friendly.
 * @param message The error message to display.
 * @author DroidSwap
 */
@Composable
fun ErrorText(message: String) {
    Text(
        text = message, // ViewModel should ideally resolve this from string resources
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp).fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

/**
 * Formats a BigDecimal value as a currency string using the default locale.
 * @param amount The BigDecimal amount to format.
 * @return A string representing the formatted currency.
 * @author DroidSwap
 */
@Composable
private fun formatCurrency(amount: BigDecimal): String {
    // Using LocalContext.current might be an issue in @Preview if not handled.
    // Consider providing NumberFormat instance or Locale via parameters for better testability/previewability.
    val formatter = NumberFormat.getCurrencyInstance() // Uses default locale from LocalContext indirectly
    return formatter.format(amount)
}

// --- Previews ---
@Preview(showBackground = true, name = "SplitScreen Initial State")
@Composable
fun SplitScreenPreviewInitial() {
    CalcTheme {
        SplitScreenContent(
            uiState = SplitContract.SplitScreenState(
                initialTotalAmount = BigDecimal("123.45"),
                participants = listOf(Participant(id="1", name = "Alice"), Participant(id="2", name = "Bob"))
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "SplitScreen With Result")
@Composable
fun SplitScreenPreviewWithResult() {
    val participant1 = Participant(id="1", name = "Alice")
    val participant2 = Participant(id="2", name = "Bob")
    CalcTheme {
        SplitScreenContent(
            uiState = SplitContract.SplitScreenState(
                initialTotalAmount = BigDecimal("100.00"),
                tipAmountInput = "15",
                taxAmountInput = "7.50",
                participants = listOf(participant1, participant2),
                selectedAlgorithm = SplitAlgorithm.EQUAL,
                splitResult = mapOf(participant1.id to BigDecimal("61.25"), participant2.id to BigDecimal("61.25")),
                finalTotalToSplit = BigDecimal("122.50")
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "SplitScreen With Error Messages")
@Composable
fun SplitScreenPreviewWithErrorMessages() {
    CalcTheme {
        SplitScreenContent(
            uiState = SplitContract.SplitScreenState(
                initialTotalAmount = BigDecimal("100.00"),
                participants = listOf(Participant(id="1", name = "Alice"), Participant(id="2", name = "Bob")),
                errorMessages = mapOf("tip" to "Tip cannot be negative", "percentageSum" to "Percentages must sum to 100%")
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "Participant Card - Percentage")
@Composable
fun ParticipantInputCardPreview_Percentage() {
    CalcTheme {
        ParticipantInputCard(
            participant = Participant(id = "1", name = "Charlie", percentageInput = "50"),
            algorithm = SplitAlgorithm.PERCENTAGE,
            onIntent = {},
            canRemove = true,
            errorMessages = emptyMap()
        )
    }
}

@Preview(showBackground = true, name = "Participant Card - Fixed Amount Error")
@Composable
fun ParticipantInputCardPreview_FixedError() {
    CalcTheme {
        ParticipantInputCard(
            participant = Participant(id = "1", name = "Dave", fixedAmountInput = "-5"),
            algorithm = SplitAlgorithm.FIXED_THEN_EQUAL,
            onIntent = {},
            canRemove = true,
            errorMessages = mapOf("p0_fixed" to "Fixed amount cannot be negative.") // Example of field-specific error key
        )
    }
}
