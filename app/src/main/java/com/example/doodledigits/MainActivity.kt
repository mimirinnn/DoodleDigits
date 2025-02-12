package com.example.doodledigits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.doodledigits.ui.theme.DoodleDigitsTheme
import androidx.compose.ui.unit.dp


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
    NavHost(navController, startDestination = "welcome") {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("child_home") { ChildHomeScreen(navController) }
    }
}

@Composable
fun WelcomeScreen(navController: NavHostController) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
        ) {
            Text("Welcome to DoodleDigits!", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = { navController.navigate("login") }) {
                Text("Get Started")
            }
        }
    }
}


@Composable
fun LoginScreen(navController: NavHostController) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
        ) {
            Text("Login or Sign Up", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = { navController.navigate("child_home") }) {
                Text("Continue as Parent/Child")
            }
        }
    }
}

@Composable
fun ChildHomeScreen(navController: NavHostController) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding) // Використовуємо contentPadding
                .padding(16.dp),
        ) {
            Text("Hello, young artist!", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = { /* Тут буде логіка фото */ }) {
                Text("Draw a number!")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    DoodleDigitsTheme {
        WelcomeScreen(rememberNavController())
    }
}
