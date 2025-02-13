package com.example.doodledigits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.doodledigits.ui.screens.WelcomeScreen
import com.example.doodledigits.ui.screens.LoginScreen
import com.example.doodledigits.ui.screens.ChildHomeScreen
import com.example.doodledigits.ui.screens.CameraScreen
import com.example.doodledigits.ui.theme.DoodleDigitsTheme
import androidx.compose.runtime.Composable


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
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController,
        startDestination = "welcome",
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("child_home") { ChildHomeScreen(navController) }
        composable("camera") { CameraScreen(navController) }
    }
}
