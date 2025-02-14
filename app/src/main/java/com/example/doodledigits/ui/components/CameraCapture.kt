package com.example.doodledigits.ui.components

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun CameraCapture(onImageCaptured: (Uri, String) -> Unit, captureRequested: Boolean, onCaptureCompleted: () -> Unit) {
    val context = LocalContext.current

    val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "DoodleDigits")
    if (!imagesDir.exists()) imagesDir.mkdirs()

    val file = File(imagesDir, "captured_photo.jpg")
    val uri = FileProvider.getUriForFile(context, "com.example.doodledigits.provider", file)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (file.exists()) {
                Log.d("CameraCapture", "Image successfully saved: ${file.absolutePath}")
                onImageCaptured(uri, file.absolutePath)
            } else {
                Log.e("CameraCapture", "Captured file does not exist! Path: ${file.absolutePath}")
            }
        } else {
            Log.e("CameraCapture", "Failed to capture image")
        }
        onCaptureCompleted()
    }

    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            cameraLauncher.launch(uri)
        }
    }
}

