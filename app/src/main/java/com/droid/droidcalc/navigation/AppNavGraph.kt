/**
 * This file defines the main navigation graph for the DroidCalc application using Jetpack Navigation Compose.
 * It sets up all navigable screens. Data between Calculator and Split screen is now passed via a SharedViewModel.
 * @author DroidSwap
 */
package com.droid.droidcalc.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
 * Data passing for Split Calculator now primarily relies on a [SharedCalcSplitViewModel].
 *
 * @param navController The [NavHostController] managing navigation within this graph.
 * @param windowSizeClass The [WindowSizeClass] of the current window, potentially for adaptive layouts within screens.
 * @param modifier The modifier to be applied to the NavHost.
 * @param snackbarHostState The [SnackbarHostState] for managing snackbars, passed down to relevant screens.
 * @param calculatorScreenContent Composable lambda for the Calculator screen content.
 * @param historyScreenContent Composable lambda for the History screen content.
 * @param splitCalculatorScreenContent Composable lambda for the Split Calculator screen content.
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
    splitCalculatorScreenContent: @Composable (onNavigateBack: () -> Unit) -> Unit = { onNavigateBack ->
        SplitScreen(onNavigateBack = onNavigateBack, onShareClick = { /* TODO: Implement Share from SplitScreen */})
    },
    sipCalculatorScreenContent: @Composable (initialAmount: String?) -> Unit = { initialAmount ->
        // Assuming SIPScreen also uses a Hilt ViewModel that might inject SharedCalcSplitViewModel if needed
        // or receives initialAmount differently.
        SIPScreen() // If SIPScreen needs initialAmount, its ViewModel should handle it via SavedStateHandle or a shared VM.
                    // For now, matching the existing signature but not explicitly passing initialAmount to SIPScreen directly.
                    // If initialAmount for SIP is still from route, that needs specific handling.
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

        // Updated route for SplitCalculator: No longer takes route argument for total amount.
        // Data is passed via SharedCalcSplitViewModel.
        composable(Screen.SplitCalculator.route) {
            splitCalculatorScreenContent{
                navController.popBackStack()
            }
        }

        // SIP Calculator route remains if it uses route arguments.
        // If SIP also needs to use a shared ViewModel, its route and ViewModel would be updated similarly.
        composable(
            route = "sip/{initialAmount}", // Assuming this is Screen.SIPCalculator.route + "/{initialAmount}"
            arguments = listOf(navArgument("initialAmount") {
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val initialAmount = backStackEntry.arguments?.getString("initialAmount")
            // Ensure SIPScreen or its ViewModel can handle this initialAmount argument, likely via SavedStateHandle.
            // The lambda signature for sipCalculatorScreenContent has (String?) -> Unit.
            // If SIPScreen() doesn't directly take initialAmount, its ViewModel should.
            sipCalculatorScreenContent(initialAmount)
        }

        // Example if SIPCalculator screen was also to be simplified (assuming it has a route in Screen sealed class):
        // composable(Screen.SIPCalculator.route) {
        //     val sharedViewModel: SharedCalcGeneralViewModel = hiltViewModel(remember { navController.getBackStackEntry(ROUTE_OF_PARENT_GRAPH_FOR_SIP) })
        //     SIPScreen(sharedViewModel) // Or SIPViewModel gets it by injection
        // }
    }
}
