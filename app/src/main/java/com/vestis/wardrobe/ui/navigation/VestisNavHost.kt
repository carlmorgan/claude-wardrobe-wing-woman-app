package com.vestis.wardrobe.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.vestis.wardrobe.ui.AppViewModel
import com.vestis.wardrobe.ui.screens.backup.BackupScreen
import com.vestis.wardrobe.ui.screens.history.HistoryScreen
import com.vestis.wardrobe.ui.screens.library.LibraryScreen
import com.vestis.wardrobe.ui.screens.outfit.OutfitScreen
import com.vestis.wardrobe.ui.screens.reports.ReportsScreen

@Composable
fun VestisNavHost(appVm: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val hemisphere by appVm.hemisphere.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentDest = navBackStack?.destination
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Library.route)  { LibraryScreen() }
            composable(Screen.Outfit.route)   { OutfitScreen(hemisphere = hemisphere) }
            composable(Screen.History.route)  { HistoryScreen(hemisphere = hemisphere) }
            composable(Screen.Reports.route)  { ReportsScreen() }
            composable(Screen.Backup.route)   { BackupScreen(hemisphere = hemisphere) }
        }
    }
}
