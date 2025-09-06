/**
 * This file defines the main navigation graph for the DroidCalc application using Jetpack Navigation Compose.
 * It sets up all navigable screens and handles passing arguments between them, including the Calculator,
 * History, Split Calculator, and SIP Calculator screens.
 * @author DroidSwap
 */
package com.droid.droidcalc.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.droid.droidcalc.ui.calculator.CalculatorScreen
import com.droid.droidcalc.ui.history.HistoryScreen
import com.droid.droidcalc.ui.sip.screen.SIPScreen
import com.droid.droidcalc.ui.split.screen.SplitScreen

/**
 * Defines the navigation graph for the DroidCalc application.
 * This composable sets up all the navigation routes and their corresponding screen contents.
 * It supports navigation between Calculator, History, Split Calculator, and SIP Calculator screens,
 * including parameterized routes for passing data to Split and SIP calculators.
 *
 * The screen content parameters default to calling the actual screen composables, passing the necessary
 * [NavHostController] and arguments.
 *
 * @param navController The [NavHostController] managing navigation within this graph.
 * @param windowSizeClass The [WindowSizeClass] of the current window, potentially for adaptive layouts within screens.
 * @param modifier The modifier to be applied to the NavHost.
 * @param snackbarHostState The [SnackbarHostState] for managing snackbars, passed down to relevant screens.
 * @param calculatorScreenContent Composable lambda for the Calculator screen content.
 * @param historyScreenContent Composable lambda for the History screen content.
 * @param splitCalculatorScreenContent Composable lambda for the Split Calculator screen content.
 *                                     It receives an optional `total` string argument.
 * @param sipCalculatorScreenContent Composable lambda for the SIP Calculator screen content.
 *                                   It receives an optional `initialAmount` string argument.
 * @author DroidSwap
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass, 
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState, 
    calculatorScreenContent: @Composable () -> Unit = { CalculatorScreen(navController = navController) },
    historyScreenContent: @Composable () -> Unit = { HistoryScreen(snackbarHostState = snackbarHostState) },
    splitCalculatorScreenContent: @Composable (total: String?) -> Unit = { total ->
        SplitScreen()
    },
    sipCalculatorScreenContent: @Composable (initialAmount: String?) -> Unit = { initialAmount ->
        SIPScreen()
    }
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calculator.route,
        modifier = modifier
    ) {
        composable(Screen.Calculator.route) {
            calculatorScreenContent()
        }
        composable(Screen.History.route) {
            historyScreenContent()
        }

        composable(Screen.SplitCalculator.route) { 
            splitCalculatorScreenContent(null) 
        }

        composable(
            route = "split/{total}",
            arguments = listOf(navArgument("total") { 
                type = NavType.StringType
                nullable = true 
            })
        ) { backStackEntry ->
            val total = backStackEntry.arguments?.getString("total")
            splitCalculatorScreenContent(total)
        }

        composable(Screen.SIPCalculator.route) { 
            sipCalculatorScreenContent(null)
        }

        composable(
            route = "sip/{initialAmount}",
            arguments = listOf(navArgument("initialAmount") { 
                type = NavType.StringType
                nullable = true 
            })
        ) { backStackEntry ->
            val initialAmount = backStackEntry.arguments?.getString("initialAmount")
            sipCalculatorScreenContent(initialAmount)
        }
    }
}
