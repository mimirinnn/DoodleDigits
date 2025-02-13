package com.example.doodledigits.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.doodledigits.ui.components.CustomButton
import com.example.doodledigits.ui.theme.PastelPurple

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
