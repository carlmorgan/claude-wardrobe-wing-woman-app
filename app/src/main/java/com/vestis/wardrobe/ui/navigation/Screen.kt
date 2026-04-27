package com.vestis.wardrobe.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Library  : Screen("library",  "Wardrobe", Icons.Default.Checkroom)
    object Outfit   : Screen("outfit",   "Record",   Icons.Default.AddCircleOutline)
    object History  : Screen("history",  "History",  Icons.Default.CalendarMonth)
    object Reports  : Screen("reports",  "Reports",  Icons.Default.BarChart)
    object Backup   : Screen("backup",   "Backup",   Icons.Default.CloudDownload)

    // Detail routes
    object ItemDetail : Screen("item/{itemId}", "Item", Icons.Default.Checkroom) {
        fun route(id: String) = "item/$id"
    }
}

val bottomNavScreens = listOf(
    Screen.Library, Screen.Outfit, Screen.History, Screen.Reports, Screen.Backup
)
