package com.example.doodledigits.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.doodledigits.ui.components.CameraCapture
import com.example.doodledigits.ui.components.CustomButton
import com.example.doodledigits.ui.components.RecognizeNumber
import com.example.doodledigits.utils.RequestCameraPermission
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(navController: NavHostController) {
    RequestCameraPermission()

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("Waiting for image...") }
    var captureRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                    onImageCaptured = { uri ->
                        Log.d("CameraScreen", "Image captured: $uri")
                        captureRequested = false
                        capturedUri = uri

                        val file = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "captured_photo.jpg"
                        )

                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            coroutineScope.launch {
                                Log.d("CameraScreen", "Starting ML Kit recognition...")
                                recognizedText = RecognizeNumber(bitmap)
                                Log.d("CameraScreen", "Recognition result: $recognizedText")
                            }
                        } else {
                            Log.e("CameraScreen", "Captured file does not exist!")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            capturedUri?.let { uri ->
                val file = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "captured_photo.jpg"
                )

                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Log.e("CameraScreen", "Image file not found at ${file.absolutePath}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recognized: $recognizedText",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomButton(text = "Back") {
                navController.navigate("child_home")
            }
        }
    }
}
