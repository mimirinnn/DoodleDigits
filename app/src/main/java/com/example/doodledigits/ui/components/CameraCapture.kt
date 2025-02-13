package com.example.doodledigits.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

@Composable
fun CameraCapture(onImageCaptured: (Bitmap) -> Unit, onClick: () -> Unit) {
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            onImageCaptured(it)
        }
    }

    LaunchedEffect(Unit) {
        onClick() // Запускаємо камеру після кліку
        cameraLauncher.launch(null)
    }
}
