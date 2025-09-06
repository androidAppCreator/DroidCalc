/**
 * This file defines the main entry point of the DroidCalc application, [MainActivity].
 * It sets up the core UI structure using Jetpack Compose and Material 3, including
 * the overall theme, window size class handling, and the primary application scaffold
 * which hosts the navigation graph.
 *
 * @author DroidSwap
 */
package com.droid.droidcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.droid.droidcalc.navigation.AppNavGraph
import com.droid.droidcalc.navigation.Screen
import com.droid.droidcalc.navigation.allScreens
import com.droid.droidcalc.navigation.bottomNavScreens
import com.droid.droidcalc.ui.theme.CalcTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The main activity for the DroidCalc application.
 * This activity serves as the entry point for the UI and sets up the core
 * Jetpack Compose layout, including theme, navigation, and adaptive UI considerations.
 * It is annotated with @AndroidEntryPoint to enable Hilt dependency injection.
 *
 * @author DroidSwap
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is first created. This is where you should do all of your normal static set up:
     * create views, bind data to lists, etc. This method also provides you with a Bundle containing the activity's
     * previously frozen state, if there was one.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then
     * this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     * Note: Otherwise it is null.
     */
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            CalcTheme {
                AppContent(windowSizeClass = windowSizeClass)
            }
        }
    }
}

/**
 * Defines the main application content, including the Scaffold, TopAppBar, NavigationBar, and the main screen area.
 * This composable sets up the primary UI structure for DroidCalc.
 * The TopAppBar displays the current screen's title. It includes a conditional "Up" navigation icon
 * (back arrow) for screens not on the bottom navigation bar, allowing users to return to the previous screen.
 * The TopAppBar also includes a global action to navigate to the History screen, which is hidden if already on the History screen.
 * The NavigationBar dynamically displays items from [bottomNavScreens] (Calculator, Split, SIP).
 *
 * @param windowSizeClass The [WindowSizeClass] of the current window, used for adaptive layouts.
 * @param navController The [NavHostController] to be used for navigation. Defaults to a remembered NavController.
 * @param snackbarHostState The [SnackbarHostState] for managing Snackbars. Defaults to a remembered SnackbarHostState.
 * @param screenContent The composable lambda that defines the main content of the screen (typically the NavHost).
 *                      It receives a [Modifier] that should be applied, which includes necessary padding from the Scaffold.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    windowSizeClass: WindowSizeClass,
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    screenContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
        AppNavGraph(
            navController = navController,
            windowSizeClass = windowSizeClass,
            modifier = modifier,
            snackbarHostState = snackbarHostState
        )
    }
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentScreenForTitle = allScreens.find { navScreen ->
        currentDestination?.hierarchy?.any { it.route == navScreen.route || currentDestination.route?.startsWith(navScreen.route.substringBefore("/")) == true } == true
    } ?: Screen.Calculator // Default title to Calculator if no match is found

    val canNavigateBack = navController.previousBackStackEntry != null
    val isBottomNavScreen = bottomNavScreens.any { it.route == currentDestination?.route }
    val showUpButton = canNavigateBack && !isBottomNavScreen

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = currentScreenForTitle.titleResId)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                navigationIcon = {
                    if (showUpButton) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_up) // Ensure this string exists
                            )
                        }
                    }
                },
                actions = {
                    if (currentDestination?.route != Screen.History.route) {
                        IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                            Icon(
                                imageVector = Screen.History.icon,
                                contentDescription = stringResource(id = Screen.History.titleResId)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(id = screen.titleResId)) },
                        label = { Text(stringResource(id = screen.titleResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        screenContent(Modifier.padding(innerPadding))
    }
}

/**
 * A preview composable for [AppContent] on a standard phone, visualizing the main scaffold structure
 * integrating [AppNavGraph] with placeholder screens for all features.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Phone Scaffold with NavGraph Placeholders", widthDp = 360, heightDp = 720)
@Composable
fun AppContentPreviewPhone() {
    CalcTheme {
        val previewWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 720.dp))
        val navController = rememberNavController() // Preview NavController
        val snackbarHostState = remember { SnackbarHostState() }

        AppContent(
            windowSizeClass = previewWindowSizeClass,
            navController = navController,
            snackbarHostState = snackbarHostState,
            screenContent = { modifier ->
                // Simplified NavGraph for preview, or use a placeholder Box
                AppNavGraph(
                    navController = navController,
                    windowSizeClass = previewWindowSizeClass,
                    modifier = modifier,
                    snackbarHostState = snackbarHostState,
                    calculatorScreenContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Calculator Screen Placeholder (Preview)")
                        }
                    },
                    historyScreenContent = { // Simulate being on history screen for preview
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("History Screen Placeholder (Preview)")
                        }
                    },
                    splitCalculatorScreenContent = { _ ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Split Calculator Placeholder (Preview)")
                        }
                    },
                    sipCalculatorScreenContent = { _ ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("SIP Calculator Placeholder (Preview)")
                        }
                    }
                )
            }
        )
    }
}


/**
 * A preview composable for [AppContent] on a foldable device, with placeholders.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Foldable Scaffold with NavGraph Placeholders", widthDp = 673, heightDp = 841)
@Composable
fun AppContentPreviewFoldable() {
    CalcTheme {
        val previewWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(673.dp, 841.dp))
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        AppContent(
            windowSizeClass = previewWindowSizeClass,
            navController = navController,
            snackbarHostState = snackbarHostState,
            screenContent = { modifier ->
                AppNavGraph(
                    navController = navController,
                    windowSizeClass = previewWindowSizeClass,
                    modifier = modifier,
                    snackbarHostState = snackbarHostState,
                    calculatorScreenContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Calculator Screen Placeholder (Preview)")
                        }
                    },
                    historyScreenContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("History Screen Placeholder (Preview)")
                        }
                    },
                    splitCalculatorScreenContent = { _ ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Split Calculator Placeholder (Preview)")
                        }
                    },
                    sipCalculatorScreenContent = { _ ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("SIP Calculator Placeholder (Preview)")
                        }
                    }
                )
            }
        )
    }
}

/**
 * A preview composable for [AppContent] on a tablet in portrait, with placeholders.
 * @author DroidSwap
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Tablet Portrait Scaffold with NavGraph Placeholders", widthDp = 800, heightDp = 1280)
@Composable
fun AppContentPreviewTabletPortrait() {
    CalcTheme {
        val previewWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 1280.dp))
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        AppContent(
            windowSizeClass = previewWindowSizeClass,
            navController = navController,
            snackbarHostState = snackbarHostState,
            screenContent = { modifier ->
                AppNavGraph(
                    navController = navController,
                    windowSizeClass = previewWindowSizeClass,
                    modifier = modifier,
                    snackbarHostState = snackbarHostState,
                    calculatorScreenContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Calculator Screen Placeholder (Preview)")
                        }
                    },
                    historyScreenContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("History Screen Placeholder (Preview)")
                        }
                    },
                    splitCalculatorScreenContent = { _ ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Split Calculator Placeholder (Preview)")
                        }
                    },
                    sipCalculatorScreenContent = { _ ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SIP Calculator Placeholder (Preview)")
                        }
                    }
                )
            }
        )
    }
}
