/**
 * This file defines the [CalculatorScreen] and its content, which constitutes the main UI
 * for the calculator feature. It interacts with [CalculatorViewModel]
 * to manage state and handle user intents, including interactions with the calculated result
 * to navigate to other features like Split and SIP calculators via a bottom sheet.
 * It adheres to MVI principles, observing [CalculatorContract.State] and sending [CalculatorContract.Intent].
 * @author DroidSwap
 */
package com.droid.droidcalc.ui.calculator

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
    val BottomSheetBottomPadding = 32.dp
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
    val intent: CalculatorContract.Intent, // Updated to use consolidated Intent
    val keyType: KeyType,
    val span: Int = 1
)

/**
 * Main composable for the Calculator screen.
 * This screen displays the calculator interface, including the expression/result display
 * and the keypad. It observes state from [CalculatorViewModel], dispatches user intents,
 * and handles navigation and UI effects triggered by the ViewModel.
 *
 * @param navController The [NavHostController] for navigating to other screens.
 * @param viewModel The [CalculatorViewModel] instance for this screen, injected by Hilt.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    navController: NavHostController,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Ensures sheet is either fully open or hidden
    )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToRoute -> {
                    navController.navigate(event.route) {
                        event.popUpToRoute?.let { popUpTo(it) { inclusive = event.inclusive } }
                        launchSingleTop = event.isLaunchSingleTop
                    }
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
    ) { _ ->
        CalculatorScreenContent(
            uiState = uiState,
            onIntent = viewModel::processIntent
        )

        if (uiState.showResultActionBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.processIntent(CalculatorContract.Intent.DismissResultActions) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                ResultActionBottomSheetContent(
                    uiState = uiState,
                    onIntent = viewModel::processIntent
                )
            }
        }
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
    val keypadButtons = rememberKeypadButtons()

    Column(
        modifier = modifier.fillMaxSize().padding(bottom = 16.dp).padding(horizontal = 8.dp)
    ) {
        AnimatedDisplay(
            expression = uiState.expression,
            result = uiState.displayValue,
            isError = uiState.error != null,
            isResultDisplayed = uiState.isResultDisplayed,
            onResultClick = { onIntent(CalculatorContract.Intent.ResultDisplayClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = CalculatorScreenDimens.DisplayBottomPadding)
        )

        KeypadView(
            buttons = keypadButtons,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Composable content for the result action bottom sheet.
 * Displays options to use the calculator result in other features like Split or SIP.
 *
 * @param uiState The current [CalculatorContract.State], used to access `actionableNumericResult`.
 * @param onIntent Callback to dispatch [CalculatorContract.Intent]s for actions.
 * @author DroidSwap
 */
@Composable
private fun ResultActionBottomSheetContent(
    uiState: CalculatorContract.State,
    onIntent: (CalculatorContract.Intent) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = CalculatorScreenDimens.BottomSheetBottomPadding)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.action_use_result_for_split)) },
            leadingContent = {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = stringResource(R.string.action_use_result_for_split)
                )
            },
            modifier = Modifier.clickable { onIntent(CalculatorContract.Intent.ActionNavigateToSplit) }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.action_use_result_for_sip)) },
            leadingContent = {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = stringResource(R.string.action_use_result_for_sip)
                )
            },
            modifier = Modifier.clickable { onIntent(CalculatorContract.Intent.ActionNavigateToSIP) }
        )
        if (uiState.actionableNumericResult == null && uiState.showResultActionBottomSheet) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_no_valid_result_for_action)) },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) }
            )
        }
    }
}


/**
 * Composable function that defines and remembers the list of [KeypadButton]s for the calculator.
 * It now uses the consolidated [CalculatorContract.Intent] and stringResources for all labels.
 * @return A list of [KeypadButton] objects.
 * @author DroidSwap
 */
@Composable
private fun rememberKeypadButtons(): List<KeypadButton> {
    // Assuming R.string.keypad_0, R.string.keypad_1 ... R.string.keypad_9 are defined
    // For example: <string name="keypad_7">7</string>
    return listOf(
        KeypadButton(
            stringResource(R.string.keypad_clear),
            CalculatorContract.Intent.Clear,
            KeyType.ACTION_PRIMARY
        ),
        KeypadButton(
            stringResource(R.string.keypad_parentheses),
            CalculatorContract.Intent.ParenthesesInput,
            KeyType.ACTION_SECONDARY
        ),
        KeypadButton(
            stringResource(R.string.keypad_percentage),
            CalculatorContract.Intent.PercentageInput,
            KeyType.ACTION_SECONDARY
        ),
        KeypadButton(
            stringResource(R.string.keypad_divide),
            CalculatorContract.Intent.OperatorInput('/'),
            KeyType.OPERATOR
        ),

        KeypadButton(
            stringResource(R.string.keypad_7),
            CalculatorContract.Intent.NumberInput('7'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_7 = "7"
        KeypadButton(
            stringResource(R.string.keypad_8),
            CalculatorContract.Intent.NumberInput('8'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_8 = "8"
        KeypadButton(
            stringResource(R.string.keypad_9),
            CalculatorContract.Intent.NumberInput('9'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_9 = "9"
        KeypadButton(
            stringResource(R.string.keypad_multiply),
            CalculatorContract.Intent.OperatorInput('*'),
            KeyType.OPERATOR
        ),

        KeypadButton(
            stringResource(R.string.keypad_4),
            CalculatorContract.Intent.NumberInput('4'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_4 = "4"
        KeypadButton(
            stringResource(R.string.keypad_5),
            CalculatorContract.Intent.NumberInput('5'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_5 = "5"
        KeypadButton(
            stringResource(R.string.keypad_6),
            CalculatorContract.Intent.NumberInput('6'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_6 = "6"
        KeypadButton(
            stringResource(R.string.keypad_subtract),
            CalculatorContract.Intent.OperatorInput('-'),
            KeyType.OPERATOR
        ),

        KeypadButton(
            stringResource(R.string.keypad_1),
            CalculatorContract.Intent.NumberInput('1'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_1 = "1"
        KeypadButton(
            stringResource(R.string.keypad_2),
            CalculatorContract.Intent.NumberInput('2'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_2 = "2"
        KeypadButton(
            stringResource(R.string.keypad_3),
            CalculatorContract.Intent.NumberInput('3'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_3 = "3"
        KeypadButton(
            stringResource(R.string.keypad_add),
            CalculatorContract.Intent.OperatorInput('+'),
            KeyType.OPERATOR
        ),

        KeypadButton(
            stringResource(R.string.keypad_0),
            CalculatorContract.Intent.NumberInput('0'),
            KeyType.NUMBER
        ), // Assuming R.string.keypad_0 = "0"
        KeypadButton(
            stringResource(R.string.keypad_decimal),
            CalculatorContract.Intent.DecimalInput,
            KeyType.NUMBER
        ),
        KeypadButton(
            stringResource(R.string.keypad_delete),
            CalculatorContract.Intent.Delete,
            KeyType.ACTION_SECONDARY
        ),
        KeypadButton(
            stringResource(R.string.keypad_equals),
            CalculatorContract.Intent.Calculate,
            KeyType.ACTION_TERTIARY
        )
    )
}

/**
 * Composable function for rendering the calculator keypad.
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
        items(buttons, key = { it.label }) { button ->
            CalcKeyButton(
                text = button.label,
                onClick = { onIntent(button.intent) },
                keyType = button.keyType,
                modifier = Modifier
                    .aspectRatio(CalculatorScreenDimens.KeypadNormalButtonRatio)
                    .fillMaxWidth()
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
                displayValue = "579",
                isResultDisplayed = true
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "Bottom Sheet Preview - Result Actions")
@Composable
fun ResultActionBottomSheetPreview() {
    CalcTheme {
        ResultActionBottomSheetContent(
            uiState = CalculatorContract.State(
                actionableNumericResult = "579",
                showResultActionBottomSheet = true
            ),
            onIntent = {}
        )
    }
}
