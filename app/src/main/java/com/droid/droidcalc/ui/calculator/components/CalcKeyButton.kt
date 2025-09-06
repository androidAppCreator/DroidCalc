package com.droid.droidcalc.ui.calculator.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.droidcalc.ui.theme.CalcTheme
import com.droid.droidcalc.ui.theme.MotionTokens

/**
 * Enum representing the type of calculator key, used to determine its visual style and emphasis.
 * - [NUMBER]: For digit keys (0-9) and decimal point.
 * - [OPERATOR]: For arithmetic operation keys (+, -, *, /).
 * - [ACTION_PRIMARY]: For primary action keys like Clear (C/AC).
 * - [ACTION_SECONDARY]: For secondary action keys like parentheses or backspace.
 * - [ACTION_TERTIARY]: For special action keys like Equals (=).
 *
 * @author DroidSwap
 */
enum class KeyType {
    NUMBER,
    OPERATOR,
    ACTION_PRIMARY,    // e.g., C, AC
    ACTION_SECONDARY,  // e.g., ( ), +/-, %
    ACTION_TERTIARY    // e.g., =
}

/**
 * A customizable button composable for the calculator keypad.
 * It features animated press effects (scale, tonal elevation changes) and haptic feedback.
 * The button's appearance and emphasis change based on its [KeyType].
 *
 * @param text The text label to display on the button.
 * @param onClick The lambda function to be invoked when the button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param keyType The [KeyType] of the button, influencing its styling and emphasis.
 * @param enabled Whether the button is enabled and can be interacted with.
 * @author DroidSwap
 */
@Composable
fun CalcKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    keyType: KeyType = KeyType.NUMBER,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = MotionTokens.QuickSpring,
        label = "buttonScaleAnimation"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 2.dp,
        animationSpec = MotionTokens.QuickSpringDp, // Use the Dp-specific spring
        label = "buttonElevationAnimation"
    )

    val buttonColors = when (keyType) {
        KeyType.NUMBER -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        KeyType.OPERATOR -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        KeyType.ACTION_PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        KeyType.ACTION_SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        KeyType.ACTION_TERTIARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    val baseModifier = modifier
        .fillMaxSize() // Fill the grid cell
        .scale(scale)

    // Using different button types for varied emphasis, could also use a single Button type and customize more.
    when (keyType) {
        KeyType.NUMBER, KeyType.OPERATOR, KeyType.ACTION_SECONDARY -> {
            FilledTonalButton(
                onClick = {
                    onClick()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = baseModifier,
                enabled = enabled,
                shape = CircleShape,
                colors = buttonColors,
                contentPadding = PaddingValues(0.dp),
                interactionSource = interactionSource,
                elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = elevation, pressedElevation = elevation + 4.dp)
            ) {
                ButtonText(text, keyType)
            }
        }
        KeyType.ACTION_PRIMARY, KeyType.ACTION_TERTIARY -> {
            Button(
                onClick = {
                    onClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Stronger haptic for actions
                },
                modifier = baseModifier,
                enabled = enabled,
                shape = CircleShape,
                colors = buttonColors,
                contentPadding = PaddingValues(0.dp),
                interactionSource = interactionSource,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation, pressedElevation = elevation + 4.dp)
            ) {
                ButtonText(text, keyType)
            }
        }
    }
}

/**
 * Composable for rendering the text inside a [CalcKeyButton].
 * Adjusts text style based on [KeyType].
 *
 * @param text The string to display.
 * @param keyType The [KeyType] of the button this text is for.
 * @author DroidSwap
 */
@Composable
private fun ButtonText(text: String, keyType: KeyType) {
    val textStyle = when (keyType) {
        KeyType.NUMBER -> MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp)
        KeyType.OPERATOR, KeyType.ACTION_TERTIARY -> MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp)
        KeyType.ACTION_PRIMARY, KeyType.ACTION_SECONDARY -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
    }
    Text(
        text = text,
        style = textStyle,
        textAlign = TextAlign.Center
    )
}

/**
 * Preview composable for a [CalcKeyButton] of type [KeyType.NUMBER].
 *
 * @author DroidSwap
 */
@Preview(showBackground = false, widthDp = 80, heightDp = 80)
@Composable
fun CalcKeyButtonNumberPreview() {
    CalcTheme {
        Box(Modifier.size(80.dp).padding(4.dp)) {
            CalcKeyButton(text = "7", onClick = {}, keyType = KeyType.NUMBER)
        }
    }
}

/**
 * Preview composable for a [CalcKeyButton] of type [KeyType.OPERATOR].
 *
 * @author DroidSwap
 */
@Preview(showBackground = false, widthDp = 80, heightDp = 80)
@Composable
fun CalcKeyButtonOperatorPreview() {
    CalcTheme {
        Box(Modifier.size(80.dp).padding(4.dp)) {
            CalcKeyButton(text = "+", onClick = {}, keyType = KeyType.OPERATOR)
        }
    }
}

/**
 * Preview composable for a [CalcKeyButton] of type [KeyType.ACTION_PRIMARY].
 *
 * @author DroidSwap
 */
@Preview(showBackground = false, widthDp = 80, heightDp = 80)
@Composable
fun CalcKeyButtonActionPrimaryPreview() {
    CalcTheme {
        Box(Modifier.size(80.dp).padding(4.dp)) {
            CalcKeyButton(text = "C", onClick = {}, keyType = KeyType.ACTION_PRIMARY)
        }
    }
}

/**
 * Preview composable for a [CalcKeyButton] of type [KeyType.ACTION_TERTIARY] (Equals).
 *
 * @author DroidSwap
 */
@Preview(showBackground = false, widthDp = 80, heightDp = 80)
@Composable
fun CalcKeyButtonActionEqualsPreview() {
    CalcTheme {
        Box(Modifier.size(80.dp).padding(4.dp)) {
            CalcKeyButton(text = "=", onClick = {}, keyType = KeyType.ACTION_TERTIARY)
        }
    }
}

/**
 * Preview composable for a [CalcKeyButton] of type [KeyType.ACTION_TERTIARY] (Equals).
 *
 * @author DroidSwap
 */
@Preview(showBackground = false, widthDp = 80, heightDp = 80)
@Composable
fun CalcKeyButtonActionParenthesisPreview() {
    CalcTheme {
        Box(Modifier.size(80.dp).padding(4.dp)) {
            CalcKeyButton(text = "(", onClick = {}, keyType = KeyType.ACTION_SECONDARY)
        }
    }
}
