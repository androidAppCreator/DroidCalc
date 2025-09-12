package com.droid.droidcalc.ui.split.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droid.droidcalc.R // Ensure R class is correctly imported
import com.droid.droidcalc.ui.split.model.DefaultShareMethod
import com.droid.droidcalc.ui.split.model.NewParticipantData
import com.droid.droidcalc.ui.theme.CalcTheme

// Constants for AddFriendSheet styling and dimensions
private object AddFriendSheetDimens {
    val SheetPadding = 16.dp
    val VerticalSpacing = 16.dp
    val ButtonSpacing = 8.dp
}

/**
 * A modal bottom sheet composable for adding a new participant to the split.
 * It allows users to input the participant's name and other relevant details (currently only name).
 * The sheet provides options to add the participant and either close the sheet or add another one.
 *
 * This composable is stateful concerning its own input fields but relies on callbacks
 * to communicate actions (add participant, dismiss) to the parent composable (SplitScreen).
 *
 * @param onDismissRequest Lambda invoked when the sheet should be dismissed (e.g., user clicks outside, or presses back).
 * @param onAddParticipant Lambda invoked when the user confirms adding a participant. It provides the
 *                         [NewParticipantData] and a boolean indicating if the user wishes to add another participant immediately.
 * @param sheetState The [SheetState] for controlling the modal bottom sheet, typically remembered by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class) // Required for ModalBottomSheet, rememberModalBottomSheetState
@Composable
fun AddFriendSheet(
    onDismissRequest: () -> Unit,
    onAddParticipant: (participantData: NewParticipantData, addAnother: Boolean) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) // Default sheet state
) {
    var nameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val addFriendError = stringResource(R.string.split_add_friend_error_name_required)

    // Request focus on the name field when the sheet becomes visible.
    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Handle cases where focus request might fail (e.g., during tests or specific lifecycle states)
                println("AddFriendSheet: Focus request failed: ${e.message}")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // TODO: Consider windowInsets if keyboard handling becomes an issue with bottom sheets
        // windowInsets = WindowInsets.ime.add(WindowInsets(bottom = SheetPadding)), 
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AddFriendSheetDimens.SheetPadding)
                .padding(bottom = AddFriendSheetDimens.SheetPadding), // Extra padding at bottom if keyboard is up
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.split_add_friend_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = AddFriendSheetDimens.VerticalSpacing)
            )

            // Name Input Field
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    if (nameError != null && it.isNotBlank()) { // Clear error once user starts typing valid input
                        nameError = null
                    }
                },
                label = { Text(stringResource(R.string.split_add_friend_name_label)) },
                singleLine = true,
                isError = nameError != null,
                supportingText = { // Display error message if present
                    nameError?.let { ErrorText(text = it) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        // Attempt to add with current input when action done is pressed on keyboard
                        if (nameInput.isNotBlank()) {
                            handleAddParticipant(nameInput, false, onAddParticipant) { err -> nameError = err }
                            nameInput = "" // Clear input for next potential add
                        } else {
                            nameError = addFriendError
                        }
                        keyboardController?.hide() // Hide keyboard on done
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester) // Apply focus requester
            )

            // TODO: Add Avatar Picker/Selector here in a future iteration
            // AvatarPicker(...) 

            Spacer(modifier = Modifier.height(AddFriendSheetDimens.VerticalSpacing * 2)) // More space before buttons

            // Action Buttons: "Add Another" and "Add & Close"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AddFriendSheetDimens.ButtonSpacing)
            ) {
                OutlinedButton(
                    onClick = {
                        handleAddParticipant(nameInput, true, onAddParticipant) { err -> nameError = err }
                        if (nameInput.isNotBlank() && nameError == null) { // Only clear if successfully added
                           nameInput = "" // Clear input for next participant
                           focusRequester.requestFocus() // Keep focus to allow quick next entry
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.split_add_friend_add_another_button))
                }
                Button(
                    onClick = {
                        handleAddParticipant(nameInput, false, onAddParticipant) { err -> nameError = err }
                        // If successful (no error), the parent (SplitScreen) will dismiss the sheet.
                        // If error, sheet remains open with error displayed.
                        if (nameInput.isNotBlank() && nameError == null) {
                            nameInput = "" // Clear input
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.split_add_friend_button))
                }
            }
            Spacer(modifier = Modifier.height(AddFriendSheetDimens.VerticalSpacing))
        }
    }
}

/**
 * Helper function to encapsulate the logic for adding a participant.
 * It validates the input and calls the [onAddParticipantCallback].
 *
 * @param name The current name input from the TextField.
 * @param addAnother True if the user intends to add another participant immediately after this one.
 * @param onAddParticipantCallback The callback to invoke with participant data and addAnother flag.
 * @param onError The callback to invoke with an error message string if validation fails.
 */
private fun handleAddParticipant(
    name: String,
    addAnother: Boolean,
    onAddParticipantCallback: (NewParticipantData, Boolean) -> Unit,
    onError: (String?) -> Unit
) {
    val trimmedName = name.trim()
    if (trimmedName.isBlank()) {
        onError("Participant name cannot be empty.") // Use a generic string or R.string resource
        return
    }
    onError(null) // Clear any previous error
    onAddParticipantCallback(NewParticipantData(name = trimmedName, defaultShareMethod = DefaultShareMethod.EQUAL_SHARE), addAnother)
}

/**
 * A simple composable for displaying error text, reused from SplitScreen.kt for consistency.
 * Ideally, this would be in a common UI module.
 */
@Composable
private fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}

// --- Previews ---

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddFriendSheet - Default State")
@Composable
fun AddFriendSheetPreview() {
    CalcTheme {
        // For preview, ModalBottomSheet needs to be directly composed or wrapped if visibility is tricky.
        // We'll simulate its content directly as ModalBottomSheet itself is hard to preview without full screen context.
        Column(Modifier.padding(16.dp)) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            AddFriendSheet(
                onDismissRequest = {},
                onAddParticipant = { _, _ -> },
                sheetState = sheetState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "AddFriendSheet - With Error")
@Composable
fun AddFriendSheetWithErrorPreview() {
    CalcTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp)) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            // To show error, we'd need to simulate the state that leads to an error.
            // This is easier if AddFriendSheet takes initial values or an error state directly.
            // For now, this preview is structurally same as default.
            // A more complex preview might involve a wrapper Composable to set initial state.
            AddFriendSheet(
                onDismissRequest = {},
                onAddParticipant = { data, _ -> println("Participant added: ${data.name}") },
                sheetState = sheetState
            ) 
            // How to show error? Could pass an initial error to a modified AddFriendSheet for preview,
            // or use a `remember` block to simulate an error after a delay.
        }
    }
}
