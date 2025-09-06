/**
 * @author DroidSwap
 * This file defines standard motion tokens for animations within the DroidCalc application,
 * including durations, easing curves, and spring configurations to ensure consistent
 * and expressive motion design throughout the app.
 */
package com.droid.droidcalc.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

/**
 * Defines motion tokens for animations within the DroidCalc application.
 * This includes standard durations, easing curves, and spring configurations
 * to ensure consistent and expressive motion design throughout the app.
 *
 * @author DroidSwap
 */
object MotionTokens {

    /** Standard duration for very quick transitions, like icon state changes (100ms). */
    const val ShortDuration: Int = 100
    /** Standard duration for common transitions, like short fades or slides (200ms). */
    const val MediumDuration: Int = 200
    /** Standard duration for more significant transitions, like screen fades (300ms). */
    const val LongDuration: Int = 300
    /** Standard duration for slower, more deliberate animations (500ms). */
    const val ExtraLongDuration: Int = 500

    /** Material Design standard easing curve: Decelerate. Objects decelerate quickly and finish slowly. */
    val FastOutSlowInEasing: Easing = androidx.compose.animation.core.FastOutSlowInEasing
    /** Material Design standard easing curve: Accelerate. Objects start slowly and accelerate quickly. */
    val LinearOutSlowInEasing: Easing = androidx.compose.animation.core.LinearOutSlowInEasing
    /** Material Design standard easing curve: Sharp. Objects accelerate and decelerate quickly. */
    val FastOutLinearInEasing: Easing = androidx.compose.animation.core.FastOutLinearInEasing

    /**
     * A very bouncy spring configuration.
     * Suitable for attention-grabbing effects or playful interactions.
     * Uses [Spring.DampingRatioMediumBouncy] and [Spring.StiffnessLow].
     * The type parameter T is explicitly set to Float, a common use case for spring animations.
     */
    val BouncySpring: SpringSpec<Float> = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * A less bouncy, smooth spring configuration.
     * Suitable for natural movements like list item reordering or gentle UI adjustments.
     * Uses [Spring.DampingRatioNoBouncy] and [Spring.StiffnessMedium].
     * The type parameter T is explicitly set to Float, a common use case for spring animations.
     */
    val SmoothSpring: SpringSpec<Float> = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * A stiff spring configuration for quick, responsive animations.
     * Minimal oscillation is desired with this spring.
     * Uses [Spring.DampingRatioNoBouncy] and [Spring.StiffnessHigh].
     * The type parameter T is explicitly set to Float, a common use case for spring animations.
     */
    val QuickSpring: SpringSpec<Float> = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )


    // New SpringSpec for Dp values
    val QuickSpringDp: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
        // You might want to adjust dampingRatio and stiffness for Dp animations
        // as they might feel different than Float animations.
    )

    /**
     * A low stiffness spring for gentle, subtle animations.
     * Can exhibit a bit of bounce due to [Spring.DampingRatioLowBouncy].
     * Uses [Spring.DampingRatioLowBouncy] and [Spring.StiffnessVeryLow].
     * The type parameter T is explicitly set to Float, a common use case for spring animations.
     */
    val GentleSpring: SpringSpec<Float> = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
}
