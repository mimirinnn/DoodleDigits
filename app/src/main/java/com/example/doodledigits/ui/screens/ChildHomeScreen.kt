package com.example.doodledigits.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.doodledigits.ui.theme.PastelGreen

@Composable
fun ChildHomeScreen(navController: NavHostController) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hello, young artist!",
                style = MaterialTheme.typography.headlineLarge,
                color = PastelGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("camera") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Draw a number!")
            }
        }
    }
}
