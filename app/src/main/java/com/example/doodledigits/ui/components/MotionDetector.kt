package com.example.doodledigits.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*

class MotionDetector(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var movementDetected by mutableStateOf(false)
        private set

    private val movementThreshold = 1.2f // Чим вище значення, тим більше допускається руху
    private var lastAcceleration = 0f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Обчислення сили руху телефону
                val acceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                // Перевірка чи було різке прискорення (порівняння з попереднім значенням)
                if (kotlin.math.abs(acceleration - lastAcceleration) > movementThreshold) {
                    movementDetected = true
                } else {
                    movementDetected = false
                }

                lastAcceleration = acceleration
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(sensorListener)
    }
}
