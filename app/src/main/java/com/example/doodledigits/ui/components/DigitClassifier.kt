package com.example.doodledigits.ml

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.atan2

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

    fun classifyDigit(bitmap: Bitmap, imagePath: String): Pair<Int, Bitmap> {
        // 1. Виправлення орієнтації
        val orientedBitmap = fixImageOrientation(bitmap, imagePath)

        // 2. Перетворення в градації сірого
        val grayBitmap = toGrayscale(orientedBitmap)

        // 3. Otsu Threshold (цифра → біла, фон → чорний)
        val binarizedBitmap = otsuThreshold(grayBitmap)

        // 4. Видаляємо дрібні компоненти (залишаємо найбільшу білу зону)
        val largestCompBitmap = removeSmallComponents(binarizedBitmap)

        // 5. Обрізаємо по цифрі (щоб позбутися зайвих полів)
        val croppedBitmap = cropToDigit(largestCompBitmap)

        // 6. Легка морфологія (closing 2×2) — “зашиває” розриви, не руйнує форму
        val closedBitmap = morphologicalClose2x2(croppedBitmap)

        // 7. Deskew (але тільки якщо кут не надто великий)
        val deskewedBitmap = deskewIfReasonable(closedBitmap, maxAngleDegrees = 30f)

        // 8. Масштабуємо і центруємо в 28×28
        val finalBitmap = resizeAndCenter(deskewedBitmap, 28, 20)

        // 9. Перетворюємо у ByteBuffer
        val byteBuffer = convertBitmapToByteBuffer(finalBitmap)

        // 10. Проганяємо через модель
        val output = Array(1) { FloatArray(10) }
        interpreter?.run(byteBuffer, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        Log.d("DigitClassifier", "📊 Model output: ${output[0].joinToString()}")
        Log.d("DigitClassifier", "🎯 Recognized digit: $maxIndex")

        return Pair(maxIndex, finalBitmap)
    }

    // --------------------------------------------------------------------------------------------
    // 1. Виправлення орієнтації
    private fun fixImageOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (!matrix.isIdentity) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else bitmap
        } catch (e: Exception) {
            Log.e("DigitClassifier", "❌ Error fixing image orientation", e)
            bitmap
        }
    }

    // 2. Градації сірого
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayBitmap
    }

    // 3. Otsu Threshold: білий → цифра, чорний → фон
    private fun otsuThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val histogram = IntArray(256)
        for (p in pixels) {
            val gray = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
            histogram[gray]++
        }

        val total = width * height
        var sum = 0
        for (i in 0..255) sum += i * histogram[i]

        var sumB = 0
        var wB = 0
        var maxVar = 0.0
        var threshold = 0

        for (t in 0..255) {
            wB += histogram[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t * histogram[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val varBetween = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (varBetween > maxVar) {
                maxVar = varBetween
                threshold = t
            }
        }

        val bin = IntArray(width * height)
        for (i in pixels.indices) {
            val gray = (Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i])) / 3
            bin[i] = if (gray <= threshold) Color.WHITE else Color.BLACK
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(bin, 0, width, 0, 0, width, height)
        return out
    }

    // 4. Залишаємо лише найбільшу білу компоненту (видаляємо шум)
    private fun removeSmallComponents(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bin = IntArray(width * height)
        for (i in pixels.indices) {
            bin[i] = if (pixels[i] == Color.WHITE) 1 else 0
        }

        val visited = BooleanArray(width * height)
        val labels = IntArray(width * height)
        var largestCompSize = 0
        var largestLabel = 0
        var currentLabel = 2

        fun idx(x: Int, y: Int) = y * width + x
        val directions = arrayOf(Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1))

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = idx(x, y)
                if (bin[i] == 1 && !visited[i]) {
                    currentLabel++
                    var compSize = 0
                    val stack = ArrayDeque<Pair<Int,Int>>()
                    stack.add(Pair(x, y))
                    visited[i] = true
                    labels[i] = currentLabel

                    while (stack.isNotEmpty()) {
                        val (cx, cy) = stack.removeLast()
                        compSize++
                        for ((dx, dy) in directions) {
                            val nx = cx + dx
                            val ny = cy + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                val nIdx = idx(nx, ny)
                                if (bin[nIdx] == 1 && !visited[nIdx]) {
                                    visited[nIdx] = true
                                    labels[nIdx] = currentLabel
                                    stack.add(Pair(nx, ny))
                                }
                            }
                        }
                    }

                    if (compSize > largestCompSize) {
                        largestCompSize = compSize
                        largestLabel = currentLabel
                    }
                }
            }
        }

        val outPixels = IntArray(width * height)
        for (i in outPixels.indices) {
            outPixels[i] = if (labels[i] == largestLabel) Color.WHITE else Color.BLACK
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    // 5. Обрізаємо “по цифрі” (bounding box)
    private fun cropToDigit(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.WHITE) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            // Не знайшли білих пікселів
            return bitmap
        }
        return Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    // 6. Легка морфологія (closing) з ядром 2×2
    private fun morphologicalClose2x2(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Перетворимо в 0/1
        val bin = IntArray(width * height)
        for (i in pixels.indices) {
            bin[i] = if (pixels[i] == Color.WHITE) 1 else 0
        }

        // dilation 2×2
        val dilated = IntArray(width * height)
        for (y in 0 until height - 1) {
            for (x in 0 until width - 1) {
                // Якщо хоча б один піксель в блоках 2×2 = 1, то результат 1
                var blockWhite = false
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < width && ny < height) {
                            if (bin[ny * width + nx] == 1) {
                                blockWhite = true
                                break
                            }
                        }
                    }
                    if (blockWhite) break
                }
                if (blockWhite) {
                    // Заповнюємо весь блок 2×2 = 1
                    for (dy in 0..1) {
                        for (dx in 0..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx < width && ny < height) {
                                dilated[ny * width + nx] = 1
                            }
                        }
                    }
                }
            }
        }

        // erosion 2×2
        val closed = IntArray(width * height)
        for (y in 0 until height - 1) {
            for (x in 0 until width - 1) {
                // Якщо весь блок 2×2 = 1, то результат 1
                var allWhite = true
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < width && ny < height) {
                            if (dilated[ny * width + nx] == 0) {
                                allWhite = false
                                break
                            }
                        }
                    }
                    if (!allWhite) break
                }
                if (allWhite) {
                    // Заповнюємо весь блок 2×2 = 1
                    for (dy in 0..1) {
                        for (dx in 0..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx < width && ny < height) {
                                closed[ny * width + nx] = 1
                            }
                        }
                    }
                }
            }
        }

        // Формуємо Bitmap
        val outPixels = IntArray(width * height)
        for (i in outPixels.indices) {
            outPixels[i] = if (closed[i] == 1) Color.WHITE else Color.BLACK
        }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    // 7. Deskew (але пропускаємо, якщо кут > maxAngleDegrees)
    private fun deskewIfReasonable(bitmap: Bitmap, maxAngleDegrees: Float): Bitmap {
        val angle = computeSkewAngle(bitmap)
        val absAngle = kotlin.math.abs(angle)
        Log.d("DigitClassifier", "Deskew angle = $angle deg")

        // Якщо кут надто великий — краще не чіпати
        if (absAngle > maxAngleDegrees) {
            Log.d("DigitClassifier", "Angle too large ($angle), skipping deskew")
            return bitmap
        }

        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())

        val rotated = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rotated)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bitmap, matrix, null)
        return rotated
    }

    /**
     * Обчислення кута нахилу (в градусах) через моменти білих пікселів.
     * Повертає “негативний” чи “позитивний” кут, який треба додати (postRotate).
     */
    private fun computeSkewAngle(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sumX = 0.0
        var sumY = 0.0
        var count = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.WHITE) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }
        if (count == 0) return 0.0

        val centerX = sumX / count
        val centerY = sumY / count

        var numerator = 0.0
        var denominator = 0.0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] == Color.WHITE) {
                    numerator += (x - centerX) * (y - centerY)
                    denominator += (x - centerX) * (x - centerX)
                }
            }
        }

        if (denominator == 0.0) return 0.0
        val angleRad = atan2(numerator, denominator)
        // Перетворюємо в градуси (від’ємні / додатні)
        return (-angleRad * 180.0 / Math.PI)
    }

    // 8. Масштабування і центрування у 28×28
    private fun resizeAndCenter(bitmap: Bitmap, outSize: Int, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Масштабуємо так, щоб довша сторона стала targetSize
        val scale = if (width > height) {
            targetSize.toFloat() / width
        } else {
            targetSize.toFloat() / height
        }

        val newW = (width * scale).toInt()
        val newH = (height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        val outBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawColor(Color.BLACK)
        val offsetX = (outSize - newW) / 2f
        val offsetY = (outSize - newH) / 2f
        canvas.drawBitmap(scaled, offsetX, offsetY, null)

        return outBitmap
    }

    // 9. Перетворення у ByteBuffer
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 28 * 28)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val normalized = gray / 255.0f
                byteBuffer.putFloat(normalized)
            }
        }
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
