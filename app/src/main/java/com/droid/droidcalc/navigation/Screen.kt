package com.droid.droidcalc.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.droid.droidcalc.R // Assuming R class is in this package or imported correctly

/**
 * Sealed class representing the different screens in the DroidCalc application.
 * Each screen object holds its route string, a resource ID for its title, and an icon.
 * This approach ensures type-safe navigation and centralizes navigation-related constants.
 *
 * @property route The unique string identifier for the navigation route.
 * @property titleResId The string resource ID for the screen's title (e.g., for display in a TopAppBar or NavigationBar).
 * @property icon The [ImageVector] representing the icon for this screen (e.g., for display in a NavigationBar).
 * @author DroidSwap
 */
sealed class Screen(
    val route: String,
    val titleResId: Int, 
    val icon: Int
) {
    /**
     * Represents the Calculator screen.
     * This is the main screen for performing calculations.
     */
    data object Calculator : Screen(
        route = "calculator", 
        titleResId = R.string.screen_title_calculator,
        icon = R.drawable.ic_calculator
    )

    /**
     * Represents the History screen.
     * This screen displays past calculations.
     */
    data object History : Screen(
        route = "history", 
        titleResId = R.string.screen_title_history,
        icon = R.drawable.ic_history
    )

    /**
     * Represents the Split Calculator screen.
     * This screen allows users to split amounts among participants.
     */
    data object SplitCalculator : Screen(
        route = "split_calculator_landing", // Base route for the tab
        titleResId = R.string.screen_title_split_calculator, // Needs to be defined in strings.xml
        icon = R.drawable.ic_split
    )

    /**
     * Represents the SIP (Systematic Investment Plan) Calculator screen.
     * This screen allows users to perform SIP-related calculations.
     */
    data object SIPCalculator : Screen(
        route = "sip_calculator_landing", // Base route for the tab
        titleResId = R.string.screen_title_sip_calculator, // Needs to be defined in strings.xml
        icon = R.drawable.ic_sip
    )

    // Parameterized routes like "split/{total}" and "sip/{initialAmount}" 
    // will be defined directly in AppNavGraph.kt arguments for more flexibility.
}

/**
 * List of top-level screens to be displayed in the NavigationBar.
 * The order in this list dictates the order in the bottom navigation.
 * @author DroidSwap
 */
val bottomNavScreens = listOf(
    Screen.Calculator,
    Screen.SplitCalculator,
    Screen.SIPCalculator
)

/**
 * List of all defined screens in the application.
 * This list is used to determine screen titles and other properties dynamically.
 * @author DroidSwap
 */
val allScreens = listOf(
    Screen.Calculator,
    Screen.History,
    Screen.SplitCalculator,
    Screen.SIPCalculator
)
