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

    fun classifyDigit(bitmap: Bitmap, imagePath: String): Pair<Int, Bitmap> {
        Log.d("DigitClassifier", "📸 Received image for classification (Size: ${bitmap.width}x${bitmap.height})")

        // 🔄 Виправлення орієнтації
        val correctedBitmap = fixImageOrientation(bitmap, imagePath)

        // 🔧 Передобробка (отримуємо також оброблене зображення)
        val processedBitmap = preprocessImage(correctedBitmap)

        // 🎯 Перетворення в ByteBuffer
        val byteBuffer = convertBitmapToByteBuffer(processedBitmap)

        val output = Array(1) { FloatArray(10) }
        interpreter?.run(byteBuffer, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

        Log.d("DigitClassifier", "📊 Model output: ${output[0].joinToString()}")
        Log.d("DigitClassifier", "🎯 Recognized digit: $maxIndex")

        // 🔹 Повертаємо результат і оброблене зображення для відображення
        return Pair(maxIndex, processedBitmap)
    }

    /**
     * 🔄 **Виправлення орієнтації**
     */
    fun fixImageOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e("DigitClassifier", "❌ Error fixing image orientation", e)
            bitmap
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)

        // 1️⃣ **Бінаризація (Otsu Thresholding)**
        val binarizedBitmap = otsuThreshold(resizedBitmap)

        // 2️⃣ **Обрізання зайвого білого простору**
        val croppedBitmap = cropDigit(binarizedBitmap)

        // 3️⃣ **Додавання відступів та масштабування**
        val finalBitmap = addPadding(croppedBitmap)

        Log.d("DigitClassifier", "✅ Image preprocessing completed!")
        return finalBitmap
    }

    /**
     * 📌 **Otsu Thresholding для кращої бінаризації**
     */
    private fun otsuThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val thresholdedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Otsu Threshold
        val histogram = IntArray(256)
        for (pixel in pixels) {
            val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            histogram[gray]++
        }

        var sum = 0
        for (t in 0..255) sum += t * histogram[t]

        var sumB = 0
        var wB = 0
        var wF = 0
        var maxVariance = 0.0
        var threshold = 0

        for (t in 0..255) {
            wB += histogram[t]
            if (wB == 0) continue

            wF = width * height - wB
            if (wF == 0) break

            sumB += t * histogram[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val variance = wB * wF * (mB - mF) * (mB - mF)

            if (variance > maxVariance) {
                maxVariance = variance
                threshold = t
            }
        }

        for (i in pixels.indices) {
            val gray = (Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i])) / 3
            pixels[i] = if (gray > threshold) Color.WHITE else Color.BLACK
        }

        thresholdedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return thresholdedBitmap
    }

    /**
     * 📌 **Обрізання зайвого білого простору**
     */
    private fun cropDigit(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.BLACK) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (minX < maxX && minY < maxY) {
            Bitmap.createBitmap(bitmap, minX, minY, maxX - minX, maxY - minY)
        } else {
            bitmap
        }
    }

    /**
     * 📌 **Додавання відступів і масштабування**
     */
    private fun addPadding(bitmap: Bitmap): Bitmap {
        val paddedSize = 32
        val result = Bitmap.createBitmap(paddedSize, paddedSize, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        val left = (paddedSize - bitmap.width) / 2
        val top = (paddedSize - bitmap.height) / 2
        canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), null)

        return Bitmap.createScaledBitmap(result, 28, 28, true)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 28 * 28)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = bitmap.getPixel(x, y)
                val grayscale = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val normalizedPixel = 1 - (grayscale / 255.0f)
                byteBuffer.putFloat(normalizedPixel.toFloat())
            }
        }
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}