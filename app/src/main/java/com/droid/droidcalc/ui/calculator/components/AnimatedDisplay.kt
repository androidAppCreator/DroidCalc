package com.droid.droidcalc.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
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
    const val SHIMMER_ALPHA_MEDIUM = 0.5f
    const val SHIMMER_ALPHA_LOW = 0.2f
    const val SHIMMER_TRANSLATE_ANIM_TARGET = 1000f
    const val SHIMMER_DURATION_MS = 1500
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
 * A composable function that displays the calculator's expression and result with animations.
 * It uses [AnimatedContent] for smooth transitions when the displayed value changes.
 * Features include slide & fade transitions and a scale pop animation when a new result is shown.
 * A subtle gradient shimmer background is applied when a final, non-error result is displayed.
 * The result display can be made clickable to trigger further actions.
 *
 * @param expression The current mathematical expression string.
 * @param result The main display value (current input, live evaluation, final result, or error).
 * @param isError Indicates if the current `result` string represents an error from a final calculation.
 * @param isResultDisplayed Indicates if `result` is a final submitted result.
 * @param onResultClick Lambda to be invoked when the result display area is clicked and is actionable.
 * @param modifier The modifier to be applied to this component.
 * @author DroidSwap
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedDisplay(
    expression: String,
    result: String,
    isError: Boolean,
    isResultDisplayed: Boolean,
    modifier: Modifier = Modifier,
    onResultClick: () -> Unit = {} // Default no-op
) {
    var previousResult by remember { mutableStateOf<String?>(null) }
    var showScalePop by remember { mutableStateOf(false) }
    var isResultAnimation by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(result, isResultDisplayed) {
        if (isResultDisplayed && result != previousResult) {
            showScalePop = true
            delay(MotionTokens.LongDuration + AnimationConstants.POP_ANIMATION_EXTRA_DELAY_MS)
            showScalePop = false
            previousResult = result
            isResultAnimation = true
            delay(100)
            isResultAnimation = false
        } else if (!isResultDisplayed) {
            previousResult = null
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
        if (expression.isNotEmpty() && !isResultDisplayed && !isResultAnimation) {
            Text(
                text = expression,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = DisplayDimens.ExpressionTextSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
            Spacer(modifier = Modifier.height(DisplayDimens.ExpressionResultSpacing))
        }

        AnimatedContent(
            targetState = result,
            transitionSpec = {
                if (isResultAnimation || targetState.length > initialState.length || (targetState != "0" && initialState == "0")) {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> height } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "animatedDisplayContent"
        ) { targetResult ->
            val isClickableResult = isResultDisplayed && !isError
            // Use the Material 3 ripple factory function directly
            val ripple = ripple(bounded = false)
            Box(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    enabled = isClickableResult,
                    indication = if (isClickableResult) ripple else null,
                    onClick = { if (isClickableResult) onResultClick() },
                    role = if (isClickableResult) Role.Button else null
                )
            ) {
                if (isResultDisplayed && !isError) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                    )
                }
                Text(
                    text = targetResult,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (targetResult.length > DisplayDimens.RESULT_TEXT_LENGTH_THRESHOLD) DisplayDimens.ResultTextSmallSize else DisplayDimens.ResultTextLargeSize
                    ),
                    color = if (isError && isResultDisplayed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer() {
                            if (isResultDisplayed && !isError) {
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Final Result Display")
@Composable
fun AnimatedDisplayPreviewFinalResult() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "123+456",
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
                result = "Error",
                isError = true,
                isResultDisplayed = true,
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Ongoing Input Display")
@Composable
fun AnimatedDisplayInputPreview() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "123+45*",
                result = "123+45*",
                isError = false,
                isResultDisplayed = false,
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Live Evaluation Display")
@Composable
fun AnimatedDisplayLiveEvaluationPreview() {
    CalcTheme {
        Box(Modifier.padding(DisplayDimens.PreviewContainerPadding)) {
            AnimatedDisplay(
                expression = "24+2",
                result = "26",
                isError = false,
                isResultDisplayed = false,
                modifier = Modifier.fillMaxWidth(),
                onResultClick = {}
            )
        }
    }
}
