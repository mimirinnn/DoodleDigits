package com.example.doodledigits.ui.components

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.platform.LocalContext

@Composable
fun CameraCapture(onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current

    // Створюємо файл для збереження фото
    val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "captured_photo.jpg"
    )
    val uri = FileProvider.getUriForFile(context, "com.example.doodledigits.provider", file)

    Log.d("CameraCapture", "Saving image to: ${file.absolutePath}")

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && file.exists()) {
            Log.d("CameraCapture", "Image successfully saved: $uri")
            onImageCaptured(uri)
        } else {
            Log.e("CameraCapture", "Failed to capture image or file not found!")
        }
    }

    LaunchedEffect(Unit) {
        cameraLauncher.launch(uri)
    }
}
