package com.example.doodledigits.ml

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.atan2
import kotlin.math.roundToInt

class DigitClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile(), Interpreter.Options().apply {
                setNumThreads(4)
            })
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

    /**
     * Основна функція класифікації.
     * Повертає пару: розпізнаний індекс та оброблене зображення.
     */
    fun classifyDigit(bitmap: Bitmap, imagePath: String): Pair<Int, Bitmap> {
        Log.d("DigitClassifier", "📸 Received image for classification (Size: ${bitmap.width}x${bitmap.height})")

        // 1. Виправлення орієнтації
        val correctedBitmap = fixImageOrientation(bitmap, imagePath)

        // 2. Дескейвінг (вирівнювання) зображення
        val deskewedBitmap = deskewImage(correctedBitmap)

        // 3. Передобробка: бінаризація, обрізання білого простору, додавання паддінгу
        val processedBitmap = preprocessImage(deskewedBitmap)

        // 4. Перетворення у ByteBuffer для моделі
        val byteBuffer = convertBitmapToByteBuffer(processedBitmap)

        val output = Array(1) { FloatArray(10) }
        interpreter?.run(byteBuffer, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

        Log.d("DigitClassifier", "📊 Model output: ${output[0].joinToString()}")
        Log.d("DigitClassifier", "🎯 Recognized digit: $maxIndex")

        return Pair(maxIndex, processedBitmap)
    }

    /**
     * Виправлення орієнтації зображення за EXIF-даними.
     */
    fun fixImageOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    Log.d("DigitClassifier", "🔄 Rotating image by 90°")
                    matrix.postRotate(90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    Log.d("DigitClassifier", "🔄 Rotating image by 180°")
                    matrix.postRotate(180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    Log.d("DigitClassifier", "🔄 Rotating image by 270°")
                    matrix.postRotate(270f)
                }
                else -> {
                    Log.d("DigitClassifier", "✅ No rotation needed")
                    return bitmap
                }
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e("DigitClassifier", "❌ Error fixing image orientation", e)
            bitmap
        }
    }

    /**
     * Дескейвінг зображення: обчислення кута нахилу та поворот зображення для вирівнювання.
     */
    private fun deskewImage(bitmap: Bitmap): Bitmap {
        // Обчислюємо моменти зображення
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Обчислюємо центр мас та нахил (це спрощена версія, яка може потребувати додаткової оптимізації)
        var sumX = 0.0
        var sumY = 0.0
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.BLACK) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }
        if (count == 0) return bitmap

        val centerX = sumX / count
        val centerY = sumY / count

        // Обчислення ковариації для нахилу (це спрощено)
        var numerator = 0.0
        var denominator = 0.0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.BLACK) {
                    numerator += (x - centerX) * (y - centerY)
                    denominator += (x - centerX) * (x - centerX)
                }
            }
        }
        val angle = if (denominator != 0.0) atan2(numerator, denominator) else 0.0
        Log.d("DigitClassifier", "🧭 Deskew angle (radians): $angle")

        val matrix = Matrix()
        matrix.postRotate((-angle * 180 / Math.PI).toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * Передобробка зображення: бінаризація через Otsu Thresholding та центрування цифри.
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)
        val binarizedBitmap = otsuThreshold(resizedBitmap)
        val centeredBitmap = centerDigit(binarizedBitmap)
        Log.d("DigitClassifier", "✅ Image preprocessing completed!")
        return centeredBitmap
    }

    /**
     * Otsu Thresholding для бінаризації.
     */
    private fun otsuThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val thresholdedBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val histogram = IntArray(256)
        for (pixel in pixels) {
            val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            histogram[gray]++
        }

        var sum = 0
        for (t in 0..255) sum += t * histogram[t]

        var sumB = 0
        var wB = 0
        var wF: Int
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
     * Центрування цифри в зображенні 28x28.
     */
    private fun centerDigit(bitmap: Bitmap): Bitmap {
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

        val digitWidth = maxX - minX + 1
        val digitHeight = maxY - minY + 1

        val cropped = if (digitWidth > 0 && digitHeight > 0) {
            Bitmap.createBitmap(bitmap, minX, minY, digitWidth, digitHeight)
        } else {
            bitmap
        }

        val centeredBitmap = Bitmap.createBitmap(28, 28, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(centeredBitmap)
        canvas.drawColor(Color.WHITE)
        val offsetX = (28 - cropped.width) / 2f
        val offsetY = (28 - cropped.height) / 2f
        canvas.drawBitmap(cropped, offsetX, offsetY, null)
        return centeredBitmap
    }

    /**
     * Перетворення обробленого зображення у ByteBuffer.
     * Нормалізація: чорний піксель (цифра) → 1.0f, білий (фон) → 0.0f.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 28 * 28)
        byteBuffer.order(ByteOrder.nativeOrder())

        Log.d("DigitClassifier", "🎨 Converting image to ByteBuffer...")
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = bitmap.getPixel(x, y)
                // Обчислюємо середнє значення (грейскейл)
                val gray = (Color.red(pixel) * 0.3f + Color.green(pixel) * 0.59f + Color.blue(pixel) * 0.11f)
                // Нормалізація: фон (білий) → 0, цифра (чорний) → 1
                val normalizedPixel = 1.0f - (gray / 255.0f)
                byteBuffer.putFloat(normalizedPixel)
            }
        }
        Log.d("DigitClassifier", "✅ Image successfully converted to ByteBuffer")
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
