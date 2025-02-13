package com.example.doodledigits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.doodledigits.ui.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color


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
        enterTransition = { fadeIn() }, // Додаємо плавну появу екрану
        exitTransition = { fadeOut() }  // Додаємо плавне зникнення екрану
    ) {
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
            Text(
                text = "Welcome to DoodleDigits!",
                style = MaterialTheme.typography.headlineLarge,
                color = PastelPurple
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomButton(text = "Get Started") {
                navController.navigate("login")
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
            Text(
                text = "Login or Sign Up",
                style = MaterialTheme.typography.headlineLarge,
                color = PastelPink
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomButton(text = "Continue as Parent/Child") {
                navController.navigate("child_home")
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
                .padding(contentPadding)
                .padding(16.dp),
        ) {
            Text(
                text = "Hello, young artist!",
                style = MaterialTheme.typography.headlineLarge,
                color = PastelGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            CustomButton(text = "Draw a number!") {
                // Тут буде логіка відкриття камери
            }
        }
    }
}

/** Кастомна кнопка для єдиного стилю у всьому додатку */
@Composable
fun CustomButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PastelBlue),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, color = Color.Black)
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    DoodleDigitsTheme {
        WelcomeScreen(rememberNavController())
    }
}
