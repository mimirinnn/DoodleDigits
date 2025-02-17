package com.example.doodledigits.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
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
import com.example.doodledigits.utils.RequestCameraPermission
import kotlinx.coroutines.launch
import java.io.File
import com.example.doodledigits.ui.components.TiltDetector
import com.example.doodledigits.ui.components.MotionDetector
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

@Composable
fun CameraScreen(navController: NavHostController) {
    RequestCameraPermission()

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var capturedFilePath by remember { mutableStateOf<String?>(null) }
    var processedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("Waiting for image...") }
    var captureRequested by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Додавання системи завдань
    val targetDigits = (0..9).toList()
    var isRandomMode by remember { mutableStateOf(false) }
    var currentDigitIndex by remember { mutableStateOf(0) }
    var shuffledDigits by remember { mutableStateOf(targetDigits.shuffled()) }
    val currentTargetDigit = if (isRandomMode) shuffledDigits[currentDigitIndex] else targetDigits[currentDigitIndex]

    var isButtonEnabled by remember { mutableStateOf(true) }
    var lastMovementTime by remember { mutableStateOf(0L) }
    val delayTime = 500L

    val tiltDetector = remember { TiltDetector(context) }
    val motionDetector = remember { MotionDetector(context) }
    var showMotionWarning by remember { mutableStateOf(false) }

    fun saveProgressToFirestore(digit: Int, recognized: Boolean, skipped: Boolean) {
        val userId = auth.currentUser?.uid ?: return // Отримуємо ідентифікатор користувача
        val progressData = hashMapOf(
            "digit" to digit,
            "timestamp" to Date().time, // Поточний час у мілісекундах
            "recognized" to recognized,
            "skipped" to skipped
        )

        db.collection("progress_tracking")
            .document(userId)
            .collection("digits")
            .add(progressData)
            .addOnSuccessListener { Log.d("Firestore", "✅ Progress saved!") }
            .addOnFailureListener { e -> Log.e("Firestore", "❌ Error saving progress", e) }
    }


    LaunchedEffect(Unit) {
        tiltDetector.register()
        motionDetector.register()
    }

    LaunchedEffect(motionDetector.movementDetected) {
        if (motionDetector.movementDetected) {
            isButtonEnabled = false
            lastMovementTime = System.currentTimeMillis()
        } else {
            coroutineScope.launch {
                delay(delayTime)
                if (System.currentTimeMillis() - lastMovementTime >= delayTime) {
                    isButtonEnabled = true
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

    fun goToNextDigit() {
        if (currentDigitIndex < targetDigits.size - 1) {
            currentDigitIndex++
        } else {
            currentDigitIndex = 0
            if (isRandomMode) shuffledDigits = targetDigits.shuffled()
        }
    }

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
                text = "Write the number: $currentTargetDigit",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Random Mode", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isRandomMode,
                    onCheckedChange = {
                        isRandomMode = it
                        currentDigitIndex = 0
                        shuffledDigits = targetDigits.shuffled()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                enabled = isButtonEnabled
            ) {
                Text(text = if (isButtonEnabled) "Capture Image" else "Hold still...")
            }

            if (showMotionWarning) {
                Text(
                    text = "⚠️ Phone moved too much! Hold still before taking a photo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

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
                                    val digitClassifier = DigitClassifier(context)
                                    val (recognizedDigit, procBitmap) = digitClassifier.classifyDigit(bitmap, filePath)

                                    processedBitmap = procBitmap

                                    if (recognizedDigit == currentTargetDigit) {
                                        recognizedText = "✅ Correct! $recognizedDigit"
                                        saveProgressToFirestore(recognizedDigit, recognized = true, skipped = false)
                                        goToNextDigit()
                                    } else {
                                        recognizedText = "❌ Incorrect. Got: $recognizedDigit"
                                        saveProgressToFirestore(currentTargetDigit, recognized = false, skipped = false)
                                    }

                                    digitClassifier.close()
                                }
                            } else {
                                Log.e("CameraScreen", "Bitmap is null, image might be corrupted.")
                                recognizedText = "Error: Image corrupted"
                            }
                        } else {
                            Log.e("CameraScreen", "ERROR: Captured file does not exist!")
                            recognizedText = "Error: File not found"
                        }
                    },
                    captureRequested = captureRequested,
                    onCaptureCompleted = { captureRequested = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            Row {
                CustomButton(text = "Skip") {
                    saveProgressToFirestore(currentTargetDigit, recognized = false, skipped = true)
                    goToNextDigit()
                    recognizedText = "Skipped to ${if (isRandomMode) shuffledDigits[currentDigitIndex] else targetDigits[currentDigitIndex]}"
                }

                Spacer(modifier = Modifier.width(16.dp))

                CustomButton(text = "Back") {
                    navController.navigate("child_home")
                }
            }
        }
    }
}
