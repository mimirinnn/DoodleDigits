package com.example.doodledigits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.doodledigits.ui.screens.*

import com.example.doodledigits.ui.theme.DoodleDigitsTheme
import androidx.compose.runtime.Composable
import com.example.doodledigits.ui.components.BottomNavigationBar

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoodleDigitsTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun currentRoute(navController: NavHostController): String? {
    return navController.currentBackStackEntryAsState().value?.destination?.route
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            if (currentRoute(navController) in listOf("child_home", "profile", "settings")) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController,
            startDestination = "welcome",
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            modifier = Modifier.padding(paddingValues) // Щоб нижнє меню не перекривало контент
        ) {
            composable("welcome") { WelcomeScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("child_home") { ChildHomeScreen(navController) }
            composable("camera") { CameraScreen(navController) }

            composable("profile") { ProfileScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}




