package com.droid.droidcalc.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.droidcalc.ui.theme.CalcTheme
import com.droid.droidcalc.ui.theme.MotionTokens
import kotlinx.coroutines.delay

/**
 * Animation constants used within AnimatedDisplay.
 * @author DroidSwap
 */
private object AnimationConstants {
    const val POP_ANIMATION_EXTRA_DELAY_MS = 50L
    const val SCALE_POP_TARGET = 1.1f
    const val SCALE_NORMAL_TARGET = 1.0f
}

/**
 * Dimension constants used within AnimatedDisplay.
 * @author DroidSwap
 */
private object DisplayDimens {
    val ScreenPaddingHorizontal = 16.dp
    val ScreenPaddingVertical = 24.dp
    val ExpressionTextSize = 20.sp
    val ResultTextLargeSize = 56.sp
    val ResultTextSmallSize = 40.sp
    const val RESULT_TEXT_LENGTH_THRESHOLD = 10
    val ExpressionResultSpacing = 4.dp
    val PreviewContainerPadding = 16.dp
}

/**
 * A composable function that displays the calculator's expression, live evaluation, and result with animations.
 * The top area shows the raw input expression when the user is typing.
 * The main display area (using [AnimatedContent]) shows either a live evaluation of the current expression
 * or the final result (or error) after calculation or if live evaluation is not available.
 *
 * Features include slide & fade transitions and a scale pop animation when a new final result is shown.
 * The result display can be made clickable to trigger further actions.
 *
 * @param expression The current mathematical expression string (raw user input).
 * @param liveEvaluation The live calculated result of the `expression` string, shown during input.
 * @param result The main display value to show in `AnimatedContent` (current input segment, final result, or error).
 *               This is typically `uiState.displayValue` from the ViewModel.
 * @param isError Indicates if the current `result` string represents an error from a final calculation.
 * @param isResultDisplayed Indicates if `result` is a final submitted result (e.g., after '=' is pressed).
 * @param onResultClick Lambda to be invoked when the result display area is clicked and is actionable.
 * @param modifier The modifier to be applied to this component.
 * @author DroidSwap
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedDisplay(
    expression: String,
    liveEvaluation: String,
    result: String, // This is the uiState.displayValue (current input, final result, or error)
    isError: Boolean,
    isResultDisplayed: Boolean,
    modifier: Modifier = Modifier,
    onResultClick: () -> Unit = {} // Default no-op
) {
    var previousResultForPopAnimation by remember { mutableStateOf<String?>(null) }
    var showScalePop by remember { mutableStateOf(false) }
    var isResultPopAnimationTrigger by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Trigger scale pop animation for final results
    LaunchedEffect(result, isResultDisplayed) {
        if (isResultDisplayed && result != previousResultForPopAnimation) {
            showScalePop = true
            delay(MotionTokens.LongDuration + AnimationConstants.POP_ANIMATION_EXTRA_DELAY_MS)
            showScalePop = false
            previousResultForPopAnimation = result
            isResultPopAnimationTrigger = true // Used for specific transition in AnimatedContent
            delay(100) // Duration for the specific transition
            isResultPopAnimationTrigger = false
        } else if (!isResultDisplayed) {
            previousResultForPopAnimation = null // Reset if we are back to input mode
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (showScalePop && isResultDisplayed && !isError) AnimationConstants.SCALE_POP_TARGET else AnimationConstants.SCALE_NORMAL_TARGET,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "resultScalePop"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(
                horizontal = DisplayDimens.ScreenPaddingHorizontal,
                vertical = DisplayDimens.ScreenPaddingVertical
            ),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display the raw input expression at the top only when user is actively typing (not after result is shown)
        // and there is an expression to show.
        if (expression.isNotEmpty() && !isResultDisplayed) {
            Text(
                text = expression,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = DisplayDimens.ExpressionTextSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Visible // Allow expression to overflow temporarily if needed during typing
            )
            Spacer(modifier = Modifier.height(DisplayDimens.ExpressionResultSpacing))
        }

        // Determine what to show in the main animated display area
        val mainDisplayTarget = when {
            !isResultDisplayed && liveEvaluation.isNotBlank() -> liveEvaluation // Show live evaluation if available during input
            else -> result // Otherwise, show the primary result (current input, final result, or error)
        }

        AnimatedContent(
            targetState = mainDisplayTarget,
            transitionSpec = {
                // Use pop animation for final results, otherwise standard slide
                if (isResultPopAnimationTrigger || (targetState.length > initialState.length && !initialState.endsWith("...")) || (targetState != "0" && initialState == "0")) {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> height } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "animatedDisplayContent"
        ) { targetDisplayString ->
            val isClickableResult = isResultDisplayed && !isError && result == targetDisplayString // Clickable only if it's the final, non-error result
            val rippleEffect = remember { ripple(bounded = false) } // Ripple for clickable results

            Box(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    enabled = isClickableResult,
                    indication = if (isClickableResult) rippleEffect else null,
                    onClick = { if (isClickableResult) onResultClick() },
                    role = if (isClickableResult) Role.Button else null
                )
            ) {
                // Optional: Background highlight for final, non-error results (e.g., shimmer)
                // if (isResultDisplayed && !isError && result == targetDisplayString) {
                //     Box(modifier = Modifier.matchParentSize().background(ShimmerBrush()))
                // }

                Text(
                    text = targetDisplayString,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (targetDisplayString.length > DisplayDimens.RESULT_TEXT_LENGTH_THRESHOLD) DisplayDimens.ResultTextSmallSize else DisplayDimens.ResultTextLargeSize
                    ),
                    // Color depends on whether it's a final error result or standard display
                    color = if (isError && isResultDisplayed && result == targetDisplayString) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer() {
                            // Apply scale pop animation only to final, non-error results
                            if (isResultDisplayed && !isError && result == targetDisplayString) {
                                scaleX = scale
                                scaleY = scale
                            }
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Previews need to be updated to reflect new parameter `liveEvaluation`
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Final Result Display")
@Composable
fun AnimatedDisplayPreviewFinalResult() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "123+456",
                liveEvaluation = "", // No live eval when final result is shown
                result = "579",
                isError = false,
                isResultDisplayed = true,
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Error Result Display")
@Composable
fun AnimatedDisplayErrorPreview() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "1/0",
                liveEvaluation = "", // No live eval when final error is shown
                result = "Error",
                isError = true,
                isResultDisplayed = true,
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Ongoing Input with Live Eval")
@Composable
fun AnimatedDisplayInputWithLiveEvalPreview() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "123+45*2",       // Raw input
                liveEvaluation = "213",      // Live result of 123 + (45*2) -> 123 + 90 = 213
                result = "123+45*2",       // Current input segment shown if no live eval
                isError = false,
                isResultDisplayed = false,     // User is typing
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Ongoing Input no Live Eval (incomplete)")
@Composable
fun AnimatedDisplayInputNoLiveEvalPreview() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "123+45*",       // Raw input, ends with operator
                liveEvaluation = "",          // No live eval because it's incomplete
                result = "123+45*",       // Current input segment shown
                isError = false,
                isResultDisplayed = false,     // User is typing
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Initial State")
@Composable
fun AnimatedDisplayInitialStatePreview() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "",
                liveEvaluation = "",
                result = "0", // Default display
                isError = false,
                isResultDisplayed = false,
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}
