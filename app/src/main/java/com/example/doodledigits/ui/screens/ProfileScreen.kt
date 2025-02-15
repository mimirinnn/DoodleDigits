package com.example.doodledigits.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.doodledigits.googlesigninclient.GoogleSingInClient
import android.widget.Toast
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.launch
import androidx.compose.foundation.border




@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val googleSignInClient = remember { GoogleSingInClient(context) }
    val coroutineScope = rememberCoroutineScope()

    val user = googleSignInClient.getUser()
    var userName by remember { mutableStateOf(user?.displayName ?: "Unknown User") }
    val userPhotoUrl = user?.photoUrl?.toString()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ) {
                if (userPhotoUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(userPhotoUrl),
                        contentDescription = "User Profile Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("No Profile Photo", modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
