/**
 * This file defines the SIPScreen composable, the main UI for the SIP Calculator feature.
 * It allows users to input various SIP parameters, select calculation types, and view results,
 * featuring a playful, modern, and financial-savvy UI with Material 3 components and expressive animations.
 * It follows MVI principles, observing state from [SIPViewModel] and delegating user actions via [SIPUiIntent].
 *
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.sip.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check // For Switch thumb
import androidx.compose.material.icons.filled.Close // For Switch thumb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droid.droidcalc.R // Assuming R class will be generated for string resources
import com.droid.droidcalc.domain.model.SIPResult
import com.droid.droidcalc.ui.sip.SIPContract
import com.droid.droidcalc.ui.sip.SIPContract.SIPUiIntent
import com.droid.droidcalc.ui.sip.SIPContract.SIPUiState
import com.droid.droidcalc.ui.sip.viewmodel.SIPViewModel
import com.droid.droidcalc.ui.theme.CalcTheme
import java.math.BigDecimal

/**
 * Centralized dimension values for SIPScreen.
 * For full guideline adherence, these should be `dimensionResource(R.dimen.xxx)`.
 * @author DroidSwap
 */
private object SIPScreenDimens {
    val ScreenPadding: Dp = 16.dp
    val CardPadding: Dp = 16.dp
    val FormElementSpacing: Dp = 12.dp
    val SectionSpacingMedium: Dp = 16.dp
    val SectionSpacingLarge: Dp = 24.dp
    val ButtonHeight: Dp = 48.dp
    val ProgressIndicatorSize: Dp = 24.dp
    val RowItemSpacing: Dp = 8.dp
    val TopBarIconSize: Dp = 24.dp // Example if needed
    val ResultRowVerticalPadding: Dp = 4.dp
    val ResultSectionTopPadding: Dp = 16.dp
    val ResultTitleBottomPadding: Dp = 8.dp
    val ErrorTextTopPadding: Dp = 8.dp
    val ErrorTextInternalPadding: Dp = 1.dp
    val ErrorTextHorizontalPadding: Dp = 16.dp 
}

/**
 * The main composable for the SIP (Systematic Investment Plan) Calculator Screen.
 * It observes [SIPUiState] from the [SIPViewModel], dispatches [SIPUiIntent] for user actions,
 * and handles [SIPContract.UiEffect] for showing Snackbars.
 * It renders a modern and interactive UI for SIP calculations.
 *
 * @param viewModel The [SIPViewModel] instance for this screen, defaults to Hilt ViewModel.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SIPScreen(
    viewModel: SIPViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = viewModel) { 
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SIPContract.UiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message, 
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        SIPScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onIntent = {
                viewModel.processIntent(it)
                if (it is SIPUiIntent.CalculateSIP) keyboardController?.hide()
            }
        )
    }
}

/**
 * Content composable for the SIPScreen.
 * Displays input fields for SIP parameters, calculation type selection, and results.
 * Note: Uses centralized Dp values from `SIPScreenDimens` which should ideally be `dimensionResource`.
 *
 * @param modifier Modifier for this composable.
 * @param uiState The current [SIPUiState] to render.
 * @param onIntent Lambda to send [SIPUiIntent] to the ViewModel.
 * @author DroidSwap
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SIPScreenContent(
    modifier: Modifier = Modifier,
    uiState: SIPUiState,
    onIntent: (SIPUiIntent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SIPScreenDimens.ScreenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = SIPScreenDimens.RowItemSpacing), // 8.dp
            shape = MaterialTheme.shapes.extraLarge, 
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(SIPScreenDimens.CardPadding), verticalArrangement = Arrangement.spacedBy(SIPScreenDimens.FormElementSpacing)) {
                SIPInputField(
                    value = uiState.initialInvestmentInput,
                    onValueChange = { onIntent(SIPUiIntent.UpdateInitialInvestment(it)) },
                    label = stringResource(R.string.sip_initial_investment_label),
                    isError = "initialInvestment" in uiState.errorMessages,
                    errorMessage = uiState.errorMessages["initialInvestment"],
                    keyboardType = KeyboardType.Number
                )

                SIPInputField(
                    value = uiState.monthlyContributionInput,
                    onValueChange = { onIntent(SIPUiIntent.UpdateMonthlyContribution(it)) },
                    label = stringResource(R.string.sip_monthly_contribution_label),
                    isError = "monthlyContribution" in uiState.errorMessages,
                    errorMessage = uiState.errorMessages["monthlyContribution"],
                    keyboardType = KeyboardType.Number
                )

                SIPInputField(
                    value = uiState.annualRateInput,
                    onValueChange = { onIntent(SIPUiIntent.UpdateAnnualRate(it)) },
                    label = stringResource(R.string.sip_annual_rate_label),
                    isError = "annualRate" in uiState.errorMessages,
                    errorMessage = uiState.errorMessages["annualRate"],
                    keyboardType = KeyboardType.Decimal
                )

                DurationInputSection(uiState, onIntent)

                SIPInputField(
                    value = uiState.targetGoalInput,
                    onValueChange = { onIntent(SIPUiIntent.UpdateTargetGoal(it)) },
                    label = stringResource(R.string.sip_target_value_optional_label),
                    isError = "targetGoal" in uiState.errorMessages,
                    errorMessage = uiState.errorMessages["targetGoal"],
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )

                ContributionTimingToggle(uiState.isAnnuityDue) { isDue ->
                    onIntent(SIPUiIntent.ToggleAnnuityDue(isDue))
                }
            }
        }

        Spacer(modifier = Modifier.height(SIPScreenDimens.SectionSpacingLarge))

        Button(
            onClick = { onIntent(SIPUiIntent.CalculateSIP) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(SIPScreenDimens.ButtonHeight)
        ) {
            AnimatedContent(targetState = uiState.isLoading, label = "CalculateButtonContent") {
                isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(SIPScreenDimens.ProgressIndicatorSize), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.sip_calculate_button).uppercase())
                }
            }
        }

        uiState.errorMessages["calculation"]?.let {
            ErrorText(message = it, modifier = Modifier.padding(top = SIPScreenDimens.ErrorTextTopPadding))
        }

        Spacer(modifier = Modifier.height(SIPScreenDimens.SectionSpacingMedium))
 
        AnimatedVisibility(
            visible = uiState.calculationResult != null && !uiState.isLoading,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            uiState.calculationResult?.let { result ->
                SIPResultDisplay(result = result, isAnnuityDue = uiState.isAnnuityDue)
            }
        }
    }
}

/**
 * Composable for managing Duration input (value and Years/Months toggle).
 * Note: Uses centralized Dp values from `SIPScreenDimens` which should ideally be `dimensionResource`.
 *
 * @param uiState Current UI state containing duration value and mode.
 * @param onIntent Lambda to send intents for duration changes.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationInputSection(uiState: SIPUiState, onIntent: (SIPUiIntent) -> Unit) {
    Column {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { onIntent(SIPUiIntent.ToggleDurationMode(SIPContract.DurationMode.YEARS)) },
                selected = uiState.durationMode == SIPContract.DurationMode.YEARS
            ) {
                Text(text = stringResource(R.string.sip_duration_years))
            }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { onIntent(SIPUiIntent.ToggleDurationMode(SIPContract.DurationMode.MONTHS)) },
                selected = uiState.durationMode == SIPContract.DurationMode.MONTHS
            ) {
                Text(text = stringResource(R.string.sip_duration_months))
            }
        }
        Spacer(Modifier.height(SIPScreenDimens.RowItemSpacing))
        SIPInputField(
            value = uiState.durationInput,
            onValueChange = { onIntent(SIPUiIntent.UpdateDuration(it)) },
            label = stringResource(if (uiState.durationMode == SIPContract.DurationMode.YEARS) R.string.sip_duration_in_years else R.string.sip_duration_in_months),
            isError = "duration" in uiState.errorMessages,
            errorMessage = uiState.errorMessages["duration"],
            keyboardType = KeyboardType.Number
        )
    }
}

/**
 * Composable for the Contribution Timing (Annuity Due vs. Ordinary) switch.
 * Includes a rotation animation for the switch thumb icon.
 * Note: Uses centralized Dp values from `SIPScreenDimens` which should ideally be `dimensionResource`.
 *
 * @param isAnnuityDue Current selection state.
 * @param onToggle Lambda called when the switch state changes.
 * @author DroidSwap
 */
@Composable
fun ContributionTimingToggle(isAnnuityDue: Boolean, onToggle: (Boolean) -> Unit) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isAnnuityDue) 360f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "SwitchThumbRotation"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = SIPScreenDimens.RowItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.sip_contribution_timing_label),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = isAnnuityDue,
            onCheckedChange = onToggle,
            thumbContent = {
                Icon(
                    imageVector = if (isAnnuityDue) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = if (isAnnuityDue) stringResource(R.string.sip_switch_thumb_annuity_due_desc) 
                                         else stringResource(R.string.sip_switch_thumb_ordinary_desc),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        )
    }
    Text(
        text = stringResource(if (isAnnuityDue) R.string.sip_timing_start_period else R.string.sip_timing_end_period),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * A reusable composable for SIP input fields.
 *
 * @param value The current value of the input field.
 * @param onValueChange Lambda called when the value changes.
 * @param label The label for the input field (expected to be from stringResource by caller).
 * @param isError Whether the input field has an error.
 * @param errorMessage The error message to display if any (expected to be from stringResource or validated by ViewModel).
 * @param keyboardType Keyboard type for the input.
 * @param imeAction The IME action for the keyboard.
 * @author DroidSwap
 */
@Composable
fun SIPInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    errorMessage: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        supportingText = { 
            if (isError && errorMessage != null) { 
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

/**
 * Composable to display the results of the SIP calculation.
 * Note: Uses centralized Dp values from `SIPScreenDimens` which should ideally be `dimensionResource`.
 *
 * @param result The [SIPResult] data to display.
 * @param isAnnuityDue Indicates if the primary result to highlight is Annuity Due.
 * @author DroidSwap
 */
@Composable
fun SIPResultDisplay(result: SIPResult, isAnnuityDue: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = SIPScreenDimens.ResultSectionTopPadding), 
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(SIPScreenDimens.CardPadding), verticalArrangement = Arrangement.spacedBy(SIPScreenDimens.RowItemSpacing)) {
            Text(stringResource(R.string.sip_results_header), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = SIPScreenDimens.ResultTitleBottomPadding))
            
            ResultRow(label = stringResource(R.string.sip_result_fv_ordinary), value = result.futureValueOrdinaryAnnuity.toPlainString(), isHighlighted = !isAnnuityDue)
            ResultRow(label = stringResource(R.string.sip_result_fv_due), value = result.futureValueAnnuityDue.toPlainString(), isHighlighted = isAnnuityDue)
            
            result.requiredMonthlyContribution?.let {
                ResultRow(label = stringResource(R.string.sip_result_req_monthly), value = it.toPlainString())
            }
            Divider(modifier = Modifier.padding(vertical = SIPScreenDimens.RowItemSpacing))
            
            Text(stringResource(R.string.sip_wealth_breakdown_header), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = SIPScreenDimens.ResultRowVerticalPadding))
            ResultRow(label = stringResource(R.string.sip_result_total_investment), value = result.totalInvestment.toPlainString())
            ResultRow(label = stringResource(if (isAnnuityDue) R.string.sip_result_total_interest_due else R.string.sip_result_total_interest_ordinary), value = (if (isAnnuityDue) result.totalInterestEarnedDue else result.totalInterestEarnedOrdinary).toPlainString(), isWealthGain = true)
        }
    }
}

/**
 * Helper composable for displaying a single row in the results section.
 * Features an animated content transition for the value text.
 * Note: Uses centralized Dp values from `SIPScreenDimens` which should ideally be `dimensionResource`.
 *
 * @param label The label for the result value.
 * @param value The result value as a string.
 * @param isHighlighted Whether this row should be visually highlighted.
 * @param isWealthGain Whether this row represents wealth gain.
 * @author DroidSwap
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ResultRow(label: String, value: String, isHighlighted: Boolean = false, isWealthGain: Boolean = false) {
    val valuePrefix = if (isWealthGain) "+ " else ""
    val wealthGainColor = MaterialTheme.colorScheme.primary 
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = SIPScreenDimens.ResultRowVerticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = if (isHighlighted) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        AnimatedContent(targetState = value, label = "ResultValueAnimation",
            transitionSpec = {
                // Slide in from bottom, slide out to top for numeric changes
                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
            }
        ) { targetValue -> 
            Text(
                text = "$valuePrefix${stringResource(R.string.currency_symbol)}$targetValue", 
                style = if (isHighlighted) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, 
                color = if(isWealthGain) wealthGainColor else LocalContentColor.current
            )
        }
    }
}

/**
 * Simple composable to display an error message text for inline field errors.
 * Messages are expected to be resolved from string resources by the ViewModel if static.
 * Note: Uses centralized Dp values from `SIPScreenDimens` which should ideally be `dimensionResource`.
 *
 * @param message The error message to display.
 * @param modifier Modifier for this composable.
 * @author DroidSwap
 */
@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(
            start = SIPScreenDimens.ErrorTextHorizontalPadding, 
            top = SIPScreenDimens.ErrorTextInternalPadding, 
            end = SIPScreenDimens.ErrorTextHorizontalPadding
        ).fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

// --- Previews ---
@Preview(showBackground = true, name = "SIPScreen Initial Empty")
@Composable
fun SIPScreenPreview_InitialEmpty() {
    CalcTheme {
        SIPScreenContent(
            uiState = SIPUiState(),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "SIPScreen With Inputs")
@Composable
fun SIPScreenPreview_WithInputs() {
    CalcTheme {
        SIPScreenContent(
            uiState = SIPUiState(
                initialInvestmentInput = "50000",
                monthlyContributionInput = "10000",
                annualRateInput = "12",
                durationInput = "10",
                durationMode = SIPContract.DurationMode.YEARS
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "SIPScreen With Result")
@Composable
fun SIPScreenPreview_WithResult() {
    val sampleResult = SIPResult(
        futureValueOrdinaryAnnuity = BigDecimal("1965458.76"),
        futureValueAnnuityDue = BigDecimal("1985113.35"),
        requiredMonthlyContribution = null, 
        totalInvestment = BigDecimal("1250000.00"),
        totalInterestEarnedOrdinary = BigDecimal("715458.76"),
        totalInterestEarnedDue = BigDecimal("735113.35"),
        monthlyRate = BigDecimal("0.01"),
        numberOfPeriods = 120,
        initialInvestment = BigDecimal("50000"),
        monthlyContribution = BigDecimal("10000")
    )
    CalcTheme {
        SIPScreenContent(
            uiState = SIPUiState(
                initialInvestmentInput = "50000",
                monthlyContributionInput = "10000",
                annualRateInput = "12",
                durationInput = "10",
                durationMode = SIPContract.DurationMode.YEARS,
                calculationResult = sampleResult,
                isChartVisible = true
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "SIPScreen With Error")
@Composable
fun SIPScreenPreview_WithError() {
    CalcTheme {
        SIPScreenContent(
            uiState = SIPUiState(
                initialInvestmentInput = "10000",
                annualRateInput = "-5", 
                errorMessages = mapOf("annualRate" to "Annual rate must be positive and realistic (e.g., <=100%).")
            ),
            onIntent = {}
        )
    }
}
