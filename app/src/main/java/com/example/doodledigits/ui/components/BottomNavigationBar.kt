package com.example.doodledigits.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier


sealed class BottomNavScreen(val route: String, val title: String, val icon: Int) {
    object Home : BottomNavScreen("child_home", "Home", android.R.drawable.ic_menu_view)
    object Profile : BottomNavScreen("profile", "Profile", android.R.drawable.ic_menu_myplaces)
    object Settings : BottomNavScreen("settings", "Settings", android.R.drawable.ic_menu_preferences)
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(BottomNavScreen.Home, BottomNavScreen.Profile, BottomNavScreen.Settings)
    NavigationBar(
        tonalElevation = 8.dp
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(painterResource(screen.icon), contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}