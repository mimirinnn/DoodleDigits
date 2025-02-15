package com.example.doodledigits.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.doodledigits.googlesigninclient.GoogleSingInClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color

import com.example.doodledigits.datastore.UserPreferences




@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val googleSignInClient = remember { GoogleSingInClient(context) }
    val userPreferences = remember { UserPreferences(context.applicationContext) }

    var isSignedIn by rememberSaveable { mutableStateOf(googleSignInClient.isSingedIn()) }
    val coroutineScope = rememberCoroutineScope()

    val user = googleSignInClient.getUser()
    var userName by remember { mutableStateOf(user?.displayName ?: "Unknown User") }

    // Змінні для діалогових вікон
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Таймер перед видаленням акаунту
    var deleteButtonEnabled by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(5) } // Лічильник секунд


    var isDarkTheme by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        userPreferences.themeMode.collect { isDarkTheme = it }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            var newName by remember { mutableStateOf(userName) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Name:", modifier = Modifier.weight(1f))
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier
                        .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Dark Theme", modifier = Modifier.weight(1f))
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = {
                        isDarkTheme = it
                        coroutineScope.launch {
                            userPreferences.setThemeMode(it)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val success = googleSignInClient.updateProfile(newName, user?.photoUrl?.toString())
                        if (success) {
                            userName = newName
                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Save Changes")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isSignedIn) {
                Text(
                    text = "You are signed in",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка виходу з акаунту (відкриває діалог)
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign Out")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        showDeleteDialog = true
                        countdown = 5 // Скидаємо лічильник
                        deleteButtonEnabled = false // Забороняємо натискати
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Delete Account")
                }

                if (showSignOutDialog) {
                    AlertDialog(
                        onDismissRequest = { showSignOutDialog = false },
                        title = { Text("Confirm Sign Out") },
                        text = { Text("Are you sure you want to sign out?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        googleSignInClient.signOut()
                                        isSignedIn = false
                                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                        navController.navigate("login") { popUpTo("welcome") { inclusive = true } }
                                    }
                                    showSignOutDialog = false
                                }
                            ) {
                                Text("Sign Out", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }


                if (showDeleteDialog) {
                    LaunchedEffect(Unit) {
                        for (i in 5 downTo 1) {
                            countdown = i
                            delay(1000L)
                        }
                        deleteButtonEnabled = true
                    }

                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Confirm Deletion") },
                        text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val success = googleSignInClient.deleteAccount()
                                        if (success) {
                                            isSignedIn = false
                                            Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                                            navController.navigate("login") { popUpTo("welcome") { inclusive = true } }
                                        } else {
                                            Toast.makeText(context, "Failed to delete account", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    showDeleteDialog = false
                                },
                                enabled = deleteButtonEnabled // Кнопка активується після таймера
                            ) {
                                if (deleteButtonEnabled) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Delete ($countdown s)", color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

            } else {
                Text(
                    text = "You are not signed in",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign In")
                }
            }
        }
    }
}
