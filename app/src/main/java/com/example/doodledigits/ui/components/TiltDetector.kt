package com.example.doodledigits.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*

class TiltDetector(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var tiltX by mutableStateOf(0f)
    var tiltY by mutableStateOf(0f)
    var isStable by mutableStateOf(true)

    private val tiltThreshold = 30f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val gravityX = event.values[0]
                val gravityY = event.values[1]
                val gravityZ = event.values[2] // Пряме положення телефону

                // Обчислюємо нахили в градусах
                tiltX = Math.toDegrees(Math.atan2(gravityX.toDouble(), gravityZ.toDouble())).toFloat()
                tiltY = Math.toDegrees(Math.atan2(gravityY.toDouble(), gravityZ.toDouble())).toFloat()

                // Телефон вважається рівним, якщо нахил < 10°
                isStable = (tiltX in -tiltThreshold..tiltThreshold) && (tiltY in -tiltThreshold..tiltThreshold)
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
