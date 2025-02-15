package com.example.doodledigits.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.doodledigits.googlesigninclient.GoogleSingInClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.doodledigits.ui.theme.PastelPurple

@Composable
fun LoginScreen(navController: NavHostController) {
    // Отримання контексту та створення клієнта для Google Sign In
    val context = LocalContext.current
    val googleSignInClient = remember { GoogleSingInClient(context) }

    var isSignedIn by rememberSaveable { mutableStateOf(googleSignInClient.isSingedIn()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isSignedIn) {
                Text(
                    text = "Are you ready to doodle?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PastelPurple
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You are already in the account",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Кнопка продовження
                Button(
                    onClick = { navController.navigate("child_home") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Continue")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Кнопка виходу з аккаунту з використанням іншого стилю
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            googleSignInClient.signOut()
                            isSignedIn = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = "Sign Out")
                }
            } else {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PastelPurple
                )
                Text(
                    text = "Please sign in to the account",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Кнопка для входу через Google
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSignedIn = googleSignInClient.signIn()
                            if (isSignedIn) {
                                navController.navigate("child_home")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign In With Google")
                }
            }
        }
    }
}
