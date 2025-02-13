package com.example.doodledigits.ui.components

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun RecognizeNumber(bitmap: Bitmap): String {
    return withContext(Dispatchers.IO) { // Виконуємо у фоні
        Log.d("MLKit", "RecognizeNumber() called") // Логування

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        try {
            val result = recognizer.process(image).await() // Виконуємо очікування
            val detectedText = result.text.trim()
            Log.d("MLKit", "Recognized text: $detectedText") // Лог успішного розпізнавання
            if (detectedText.isEmpty()) "No text detected" else detectedText
        } catch (e: Exception) {
            Log.e("MLKit", "Recognition failed", e)
            "Recognition failed"
        }
    }
}
