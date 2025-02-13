package com.example.doodledigits.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.doodledigits.ui.components.CameraCapture
import com.example.doodledigits.ui.components.CustomButton
import com.example.doodledigits.utils.RequestCameraPermission

@Composable
fun CameraScreen(navController: NavHostController) {
    // Запит дозволу на камеру
    RequestCameraPermission()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var captureRequested by remember { mutableStateOf(false) }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Take a photo of your number!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomButton(text = "Capture Image") {
                Log.d("CameraScreen", "Capture button clicked")
                captureRequested = true
            }

            if (captureRequested) {
                CameraCapture(
                    onImageCaptured = { bitmap ->
                        capturedBitmap = bitmap
                        captureRequested = false
                        Log.d("CameraScreen", "Image captured successfully")
                    },
                    onClick = { captureRequested = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            capturedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomButton(text = "Back") {
                navController.navigate("child_home")
            }
        }
    }
}
