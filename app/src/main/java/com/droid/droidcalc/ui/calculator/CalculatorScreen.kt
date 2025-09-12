/**
 * This file defines the [CalculatorScreen] and its content, which constitutes the main UI
 * for the calculator feature. It interacts with [CalculatorViewModel]
 * to manage state and handle user intents, including interactions with the calculated result
 * to navigate directly to the Split screen.
 * It adheres to MVI principles, observing [CalculatorContract.State] and sending [CalculatorContract.Intent].
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.droid.droidcalc.R
import com.droid.droidcalc.navigation.NavigationEvent
import com.droid.droidcalc.ui.calculator.components.AnimatedDisplay
import com.droid.droidcalc.ui.calculator.components.CalcKeyButton
import com.droid.droidcalc.ui.calculator.components.KeyType
import com.droid.droidcalc.ui.calculator.viewmodel.CalculatorViewModel
import com.droid.droidcalc.ui.common.UiEffect
import com.droid.droidcalc.ui.theme.CalcTheme

/**
 * Internal constants for dimensions and layout parameters specific to the CalculatorScreen.
 * @author DroidSwap
 */
private object CalculatorScreenDimens {
    val DisplayBottomPadding = 16.dp
    const val KeypadColumns = 4
    val KeypadContentPadding = 4.dp
    val KeypadArrangementSpacing = 8.dp
    // BottomSheetBottomPadding is no longer needed as bottom sheet is removed
    // val BottomSheetBottomPadding = 32.dp
    const val KeypadNormalButtonRatio = 1f
}

/**
 * Represents a key on the calculator keypad. Links UI element to a specific Intent.
 *
 * @property label The text displayed on the key (e.g., "7", "+", "C").
 * @property intent The [CalculatorContract.Intent] to dispatch when this key is pressed.
 * @property keyType The [KeyType] influencing the button's visual style (e.g., number, operator, action).
 * @property span The number of columns this key should span in the grid (default is 1).
 * @author DroidSwap
 */
data class KeypadButton(
    val label: String,
    val intent: CalculatorContract.Intent,
    val keyType: KeyType,
    val span: Int = 1
)

/**
 * Main composable for the Calculator screen.
 * This screen displays the calculator interface, including the expression/result display
 * and the keypad. It observes state from [CalculatorViewModel], dispatches user intents,
 * and handles navigation (direct to Split screen on result click) and UI effects triggered by the ViewModel.
 *
 * @param navController The [NavHostController] for navigating to other screens.
 * @param viewModel The [CalculatorViewModel] instance for this screen, injected by Hilt.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class) // Retained for Scaffold, SnackbarHost etc.
@Composable
fun CalculatorScreen(
    navController: NavHostController,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // sheetState is no longer needed as ModalBottomSheet is removed
    // val sheetState = rememberModalBottomSheetState(
    //     skipPartiallyExpanded = true
    // )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToRoute -> {
                    navController.navigate(event.route) {
                        event.popUpToRoute?.let { popUpTo(it) { inclusive = event.inclusive } }
                        launchSingleTop = event.isLaunchSingleTop
                    }
                    // It's crucial to inform ViewModel that navigation has been consumed
                    // to prevent re-navigation on recomposition or config change.
                    viewModel.processIntent(CalculatorContract.Intent.NavigationEffectConsumed)
                }

                is NavigationEvent.NavigateBack -> navController.popBackStack()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is UiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel,
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues -> // Changed from _ to paddingValues to follow convention
        CalculatorScreenContent(
            uiState = uiState,
            onIntent = viewModel::processIntent
        )

        // ModalBottomSheet and its content are removed as per requirements.
        // if (uiState.showResultActionBottomSheet) { ... }
    }
}

/**
 * The stateless content composable for the Calculator screen UI elements.
 * This function defines the UI structure based on the provided [CalculatorContract.State]
 * and dispatches [CalculatorContract.Intent]s for user actions.
 *
 * @param modifier Modifier for this composable.
 * @param uiState The current [CalculatorContract.State] to render.
 * @param onIntent Callback to dispatch a [CalculatorContract.Intent] to the ViewModel.
 * @author DroidSwap
 */
@Composable
private fun CalculatorScreenContent(
    modifier: Modifier = Modifier,
    uiState: CalculatorContract.State,
    onIntent: (CalculatorContract.Intent) -> Unit
) {
    val keypadButtons = rememberKeypadButtons() // Remember keypad buttons to avoid re-creation

    Column(
        // Apply the modifier passed from the Scaffold, then fillMaxSize and specific padding.
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp) // Padding for keypad from bottom of screen
            .padding(horizontal = 8.dp) // Horizontal padding for the content column
    ) {
        AnimatedDisplay(
            expression = uiState.expression,
            liveEvaluation = uiState.liveEvaluation,
            result = uiState.displayValue,
            isError = uiState.error != null,
            isResultDisplayed = uiState.isResultDisplayed,
            onResultClick = { onIntent(CalculatorContract.Intent.ResultDisplayClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Ensures display takes available vertical space
                .padding(bottom = CalculatorScreenDimens.DisplayBottomPadding)
        )

        KeypadView(
            buttons = keypadButtons,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth() // Keypad takes full width
        )
    }
}

// ResultActionBottomSheetContent is removed as the bottom sheet is no longer used.

/**
 * Composable function that defines and remembers the list of [KeypadButton]s for the calculator.
 * It uses the consolidated [CalculatorContract.Intent] and stringResources for all labels.
 * @return A list of [KeypadButton] objects.
 * @author DroidSwap
 */
@Composable
private fun rememberKeypadButtons(): List<KeypadButton> {
    // It's important that stringResource calls are direct calls within the Composable scope
    // or within another @Composable function. 'remember' should store the resolved strings.
    val clearLabel = stringResource(id = R.string.keypad_clear)
    val parenthesesLabel = stringResource(R.string.keypad_parentheses)
    val percentageLabel = stringResource(R.string.keypad_percentage)
    val divideLabel = stringResource(R.string.keypad_divide)
    val sevenLabel = stringResource(R.string.keypad_7)
    val eightLabel = stringResource(R.string.keypad_8)
    val nineLabel = stringResource(R.string.keypad_9)
    val multiplyLabel = stringResource(R.string.keypad_multiply)
    val fourLabel = stringResource(R.string.keypad_4)
    val fiveLabel = stringResource(R.string.keypad_5)
    val sixLabel = stringResource(R.string.keypad_6)
    val subtractLabel = stringResource(R.string.keypad_subtract)
    val oneLabel = stringResource(R.string.keypad_1)
    val twoLabel = stringResource(R.string.keypad_2)
    val threeLabel = stringResource(R.string.keypad_3)
    val addLabel = stringResource(R.string.keypad_add)
    val zeroLabel = stringResource(R.string.keypad_0)
    val decimalLabel = stringResource(R.string.keypad_decimal)
    val deleteLabel = stringResource(R.string.keypad_delete)
    val equalsLabel = stringResource(R.string.keypad_equals)

    return remember(clearLabel, parenthesesLabel, percentageLabel, divideLabel, sevenLabel, eightLabel, nineLabel, multiplyLabel, fourLabel, fiveLabel, sixLabel, subtractLabel, oneLabel, twoLabel, threeLabel, addLabel, zeroLabel, decimalLabel, deleteLabel, equalsLabel) {
        listOf(
            KeypadButton(clearLabel, CalculatorContract.Intent.Clear, KeyType.ACTION_PRIMARY),
            KeypadButton(parenthesesLabel, CalculatorContract.Intent.ParenthesesInput, KeyType.ACTION_SECONDARY),
            KeypadButton(percentageLabel, CalculatorContract.Intent.PercentageInput, KeyType.ACTION_SECONDARY),
            KeypadButton(divideLabel, CalculatorContract.Intent.OperatorInput('/'), KeyType.OPERATOR),
            KeypadButton(sevenLabel, CalculatorContract.Intent.NumberInput('7'), KeyType.NUMBER),
            KeypadButton(eightLabel, CalculatorContract.Intent.NumberInput('8'), KeyType.NUMBER),
            KeypadButton(nineLabel, CalculatorContract.Intent.NumberInput('9'), KeyType.NUMBER),
            KeypadButton(multiplyLabel, CalculatorContract.Intent.OperatorInput('*'), KeyType.OPERATOR),
            KeypadButton(fourLabel, CalculatorContract.Intent.NumberInput('4'), KeyType.NUMBER),
            KeypadButton(fiveLabel, CalculatorContract.Intent.NumberInput('5'), KeyType.NUMBER),
            KeypadButton(sixLabel, CalculatorContract.Intent.NumberInput('6'), KeyType.NUMBER),
            KeypadButton(subtractLabel, CalculatorContract.Intent.OperatorInput('-'), KeyType.OPERATOR),
            KeypadButton(oneLabel, CalculatorContract.Intent.NumberInput('1'), KeyType.NUMBER),
            KeypadButton(twoLabel, CalculatorContract.Intent.NumberInput('2'), KeyType.NUMBER),
            KeypadButton(threeLabel, CalculatorContract.Intent.NumberInput('3'), KeyType.NUMBER),
            KeypadButton(addLabel, CalculatorContract.Intent.OperatorInput('+'), KeyType.OPERATOR),
            KeypadButton(zeroLabel, CalculatorContract.Intent.NumberInput('0'), KeyType.NUMBER),
            KeypadButton(decimalLabel, CalculatorContract.Intent.DecimalInput, KeyType.NUMBER),
            KeypadButton(deleteLabel, CalculatorContract.Intent.Delete, KeyType.ACTION_SECONDARY),
            KeypadButton(equalsLabel, CalculatorContract.Intent.Calculate, KeyType.ACTION_TERTIARY)
        )
    }
}

/**
 * Composable function for rendering the calculator keypad.
 * Uses [LazyVerticalGrid] for efficient display of [KeypadButton]s.
 *
 * @param buttons The list of [KeypadButton]s to display.
 * @param onIntent Callback to dispatch a [CalculatorContract.Intent] to the ViewModel when a key is pressed.
 * @param modifier The modifier to be applied to this layout.
 * @author DroidSwap
 */
@Composable
private fun KeypadView(
    buttons: List<KeypadButton>,
    onIntent: (CalculatorContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(CalculatorScreenDimens.KeypadColumns),
        modifier = modifier,
        contentPadding = PaddingValues(CalculatorScreenDimens.KeypadContentPadding),
        verticalArrangement = Arrangement.spacedBy(CalculatorScreenDimens.KeypadArrangementSpacing),
        horizontalArrangement = Arrangement.spacedBy(CalculatorScreenDimens.KeypadArrangementSpacing)
    ) {
        items(buttons, key = { it.label }) { button -> // Use button.label as a unique key for items
            CalcKeyButton(
                text = button.label,
                onClick = { onIntent(button.intent) },
                keyType = button.keyType,
                modifier = Modifier
                    .aspectRatio(CalculatorScreenDimens.KeypadNormalButtonRatio) // Maintain aspect ratio
                    .fillMaxWidth() // Fill width within the grid cell
            )
        }
    }
}

@Preview(showBackground = true, name = "Phone Preview - CalculatorScreenContent")
@Composable
fun CalculatorScreenContentPreview() {
    CalcTheme {
        CalculatorScreenContent(
            uiState = CalculatorContract.State(
                expression = "123+456",
                liveEvaluation = "579",
                displayValue = "579",
                isResultDisplayed = true
            ),
            onIntent = {}
        )
    }
}

// Preview for ResultActionBottomSheetContent is removed as the composable is removed.
