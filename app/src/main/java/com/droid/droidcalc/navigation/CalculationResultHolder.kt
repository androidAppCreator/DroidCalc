package com.droid.droidcalc.navigation

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import javax.inject.Inject

/**
 * A Hilt-managed holder for sharing the calculator result between different ViewModels.
 * This instance is scoped to the lifecycle of an Activity, ensuring data persistence
 * across configuration changes and shared access among ViewModels within that activity.
 *
 * @author DroidSwap
 */
@ActivityRetainedScoped // Ensures the same instance is used throughout the activity's lifecycle
class CalculationResultHolder @Inject constructor() {

    private val _calculatorResult = MutableStateFlow<BigDecimal?>(null)
    val calculatorResult: StateFlow<BigDecimal?> = _calculatorResult.asStateFlow()

    private val _resultConsumed = MutableStateFlow(true) // Start as true, becomes false when new result is set
    val resultConsumed: StateFlow<Boolean> = _resultConsumed.asStateFlow()

    /**
     * Sets the calculator result to be shared.
     * Marks the result as unconsumed.
     *
     * @param result The BigDecimal result from the calculator.
     */
    fun setCalculatorResult(result: BigDecimal) {
        _calculatorResult.value = result
        _resultConsumed.value = false // Mark that a new result is available and unconsumed
    }

    /**
     * Marks the calculator result as consumed.
     * This should be called by the consuming ViewModel after it has processed the result
     * to prevent re-processing on configuration changes or recompositions.
     */
    fun consumeCalculatorResult() {
        _resultConsumed.value = true
        // Optionally clear the result after consumption if it's a strict one-time event.
        // _calculatorResult.value = null
    }

    /**
     * Clears the calculator result and marks it as consumed.
     */
    fun clearResult() {
        _calculatorResult.value = null
        _resultConsumed.value = true
    }
}
