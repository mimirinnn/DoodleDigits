package com.example.doodledigits.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
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
import com.example.doodledigits.ml.DigitClassifier
import com.example.doodledigits.ui.components.CameraCapture
import com.example.doodledigits.ui.components.CustomButton
import com.example.doodledigits.ui.components.RecognizeNumber
import com.example.doodledigits.utils.RequestCameraPermission
import kotlinx.coroutines.launch
import java.io.File

import com.example.doodledigits.ui.components.TiltDetector
import com.example.doodledigits.ui.components.MotionDetector
import kotlinx.coroutines.delay


@Composable
fun CameraScreen(navController: NavHostController) {
    RequestCameraPermission()

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedFilePath by remember { mutableStateOf<String?>(null) }
    var processedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("Waiting for image...") }
    var captureRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    //
    val tiltDetector = remember { TiltDetector(context) }
    val motionDetector = remember { MotionDetector(context) }
    var showMotionWarning by remember { mutableStateOf(false) }

    var isButtonEnabled by remember { mutableStateOf(true) }
    var lastMovementTime by remember { mutableStateOf(0L) }
    val delayTime = 500L

    LaunchedEffect(Unit) {
        tiltDetector.register()
        motionDetector.register()
    }

    LaunchedEffect(motionDetector.movementDetected) {
        if (motionDetector.movementDetected) {
            isButtonEnabled = false // Блокуємо кнопку при русі
            lastMovementTime = System.currentTimeMillis() // Запам'ятовуємо час руху
        } else {
            coroutineScope.launch {
                delay(delayTime) // Чекаємо 2 секунди
                if (System.currentTimeMillis() - lastMovementTime >= delayTime) {
                    isButtonEnabled = true // Розблоковуємо кнопку після затримки
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tiltDetector.unregister()
            motionDetector.unregister()
        }
    }
    //

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
            //
            Text(
                text = if (tiltDetector.isStable) "✅ Phone is level"
                else "⚠️ Tilt detected!",
                style = MaterialTheme.typography.bodyLarge,
                color = if (tiltDetector.isStable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            if (!tiltDetector.isStable) {
                Text(
                    text = "Place the paper on a table and hold the phone directly above it.",
                    style = MaterialTheme.typography.bodySmall, // Менший розмір тексту
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // Сірий колір
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    captureRequested = true
                    showMotionWarning = motionDetector.movementDetected
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isButtonEnabled // Кнопка доступна тільки після затримки
            ) {
                Text(text = if (isButtonEnabled) "Capture Image" else "Hold still...")
            }

            // Попередження, якщо телефон рухався під час фото
            if (showMotionWarning) {
                Text(
                    text = "⚠️ Phone moved too much! Hold still before taking a photo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
            //
            if (captureRequested) {
                CameraCapture(
                    onImageCaptured = { uri, filePath ->
                        Log.d("CameraScreen", "Image captured: $uri")
                        captureRequested = false
                        capturedUri = uri
                        capturedFilePath = filePath

                        val file = File(filePath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                coroutineScope.launch {
                                    // Створюємо новий екземпляр DigitClassifier
                                    val digitClassifier = DigitClassifier(context)
                                    // Функція classifyDigit повертає пару (розпізнаний індекс, оброблене зображення)
                                    val (recognizedDigit, procBitmap) = digitClassifier.classifyDigit(bitmap, filePath)
                                    recognizedText = recognizedDigit.toString()
                                    processedBitmap = procBitmap
                                    Log.d("CameraScreen", "Recognition result: $recognizedText")
                                    digitClassifier.close()
                                }
                            } else {
                                Log.e("CameraScreen", "Bitmap is null, image might be corrupted.")
                                recognizedText = "Error: Image corrupted"
                            }
                        } else {
                            Log.e("CameraScreen", "ERROR: Captured file does not exist! Full path: ${file.absolutePath}")
                            recognizedText = "Error: File not found"
                        }
                    },
                    captureRequested = captureRequested,
                    onCaptureCompleted = { captureRequested = false }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Відображення обробленого зображення для налагодження
            processedBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Processed Image",
                    modifier = Modifier.size(200.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recognized: $recognizedText",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("child_home") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Back")
            }
        }
    }
}
