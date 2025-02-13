package com.example.doodledigits.ui.components

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

fun RecognizeNumber(bitmap: Bitmap, callback: (String) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val image = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(image)
        .addOnSuccessListener { result ->
            val detectedText = result.text.trim()
            Log.d("MLKit", "Recognized text: $detectedText")
            callback(detectedText)
        }
        .addOnFailureListener { e ->
            Log.e("MLKit", "Recognition failed", e)
            callback("Recognition failed")
        }
}
