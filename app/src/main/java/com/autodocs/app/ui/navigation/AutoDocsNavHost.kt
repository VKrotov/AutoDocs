package com.autodocs.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autodocs.app.ui.screens.home.HomeScreen
import com.autodocs.app.ui.screens.journal.JournalScreen
import com.autodocs.app.ui.screens.plan.PlanScreen
import com.autodocs.app.ui.screens.settings.SettingsScreen

@Composable
fun AutoDocsNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.HOME.route
    val currentDestination = Destination.entries.firstOrNull { it.route == currentRoute } ?: Destination.HOME

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomMenu(
                current = currentDestination,
                onSelect = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddClick = {
                    // Створення нового запису журналу — етап 3.
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Destination.HOME.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Destination.HOME.route) {
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
                composable(Destination.JOURNAL.route) {
                    JournalScreen(modifier = Modifier.padding(innerPadding))
                }
                composable(Destination.PLAN.route) {
                    PlanScreen(modifier = Modifier.padding(innerPadding))
                }
                composable(Destination.SETTINGS.route) {
                    SettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
