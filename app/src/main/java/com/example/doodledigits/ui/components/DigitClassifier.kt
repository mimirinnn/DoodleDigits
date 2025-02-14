package com.example.doodledigits.ml

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DigitClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile())
            Log.d("DigitClassifier", "✅ TFLite Model Loaded Successfully!")
        } catch (e: Exception) {
            Log.e("DigitClassifier", "❌ Error loading model", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("mnist.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun classifyDigit(bitmap: Bitmap, imagePath: String): Int {
        Log.d("DigitClassifier", "📸 Received image for classification (Size: ${bitmap.width}x${bitmap.height})")

        val correctedBitmap = fixImageOrientation(bitmap, imagePath)
        val processedBitmap = preprocessImage(correctedBitmap)
        val byteBuffer = convertBitmapToByteBuffer(processedBitmap)

        val output = Array(1) { FloatArray(10) }
        interpreter?.run(byteBuffer, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

        Log.d("DigitClassifier", "📊 Model output: ${output[0].joinToString()}")
        Log.d("DigitClassifier", "🎯 Recognized digit: $maxIndex")

        return maxIndex
    }

    fun fixImageOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e("DigitClassifier", "❌ Error fixing image orientation", e)
            bitmap
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)
        val processedBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix(floatArrayOf(
            -1f,  0f,  0f,  0f, 255f,  // Invert Red
            0f, -1f,  0f,  0f, 255f,  // Invert Green
            0f,  0f, -1f,  0f, 255f,  // Invert Blue
            0f,  0f,  0f,  1f,   0f   // Alpha unchanged
        ))
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(resizedBitmap, 0f, 0f, paint)

        return processedBitmap
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 28 * 28)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = bitmap.getPixel(x, y)
                val grayscale = (Color.red(pixel) * 0.3f + Color.green(pixel) * 0.59f + Color.blue(pixel) * 0.11f)
                val normalizedPixel = grayscale / 255.0f
                byteBuffer.putFloat(normalizedPixel)
            }
        }
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
