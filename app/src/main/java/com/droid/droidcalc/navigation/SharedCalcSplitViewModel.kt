package com.droid.droidcalc.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import javax.inject.Inject

/**
 * A ViewModel shared between the Calculator and Split screens to pass the calculated total.
 * This ViewModel is managed by Hilt and can be injected into other Hilt ViewModels,
 * effectively sharing the same instance if their scopes align (e.g., both part of the same activity).
 *
 * @author DroidSwap
 */
@HiltViewModel
class SharedCalcSplitViewModel @Inject constructor() : ViewModel() {

    private val _calculatorResult = MutableStateFlow<BigDecimal?>(null)
    val calculatorResult: StateFlow<BigDecimal?> = _calculatorResult.asStateFlow()

    private val _resultConsumed = MutableStateFlow(true) // Start as true, becomes false when new result is set
    val resultConsumed: StateFlow<Boolean> = _resultConsumed.asStateFlow()

    /**
     * Sets the calculator result to be passed to the Split screen.
     * The result is considered unconsumed after being set.
     *
     * @param result The BigDecimal result from the calculator.
     */
    fun setCalculatorResult(result: BigDecimal) {
        _calculatorResult.value = result
        _resultConsumed.value = false // Mark that a new result is available and unconsumed
    }

    /**
     * Consumes the calculator result.
     * This should be called by the SplitViewModel after it has processed the result
     * to prevent re-processing on configuration changes or recompositions.
     */
    fun consumeCalculatorResult() {
        _resultConsumed.value = true
        // Optionally clear the result after consumption if it's truly a one-time event
        // and not needed if the user navigates back and forth without a new calculation.
        // _calculatorResult.value = null 
    }

    /**
     * Clears the calculator result and marks it as consumed.
     * Useful if navigating away from the consuming screen or if the data is no longer needed.
     * For instance, if SplitScreen is popped from backstack, this could be called.
     */
    fun clearResult() {
        _calculatorResult.value = null
        _resultConsumed.value = true
    }
}
