package com.example.stereovision

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

/**
 * MotionTracker handles accelerometer and gyroscope data to provide filtered 
 * linear acceleration, translation, and rotation (relative pose R, t).
 * It uses a low-pass filter to remove gravity and performs double integration.
 */
class MotionTracker : SensorEventListener {

    private val alpha = 0.9f
    private val gravity = FloatArray(3)
    private val linearAcceleration = AtomicReference(FloatArray(3) { 0f })

    private var lastTimestampAcc: Long = 0
    private val velocity = FloatArray(3)
    private val displacement = FloatArray(3)
    
    private var lastTimestampGyro: Long = 0
    private val rotationEuler = FloatArray(3) // [pitch, yaw, roll]
    
    private val zuptThreshold = 0.15f 
    private val stateLock = Any()

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val timestamp = event.timestamp
        if (lastTimestampAcc == 0L) {
            lastTimestampAcc = timestamp
            return
        }

        val dt = (timestamp - lastTimestampAcc) / 1_000_000_000.0f
        lastTimestampAcc = timestamp

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        val ax = event.values[0] - gravity[0]
        val ay = event.values[1] - gravity[1]
        val az = event.values[2] - gravity[2]

        synchronized(stateLock) {
            val accelMagnitude = sqrt(ax * ax + ay * ay + az * az)
            if (accelMagnitude < zuptThreshold) {
                velocity[0] = 0f; velocity[1] = 0f; velocity[2] = 0f
            } else {
                velocity[0] += ax * dt
                velocity[1] += ay * dt
                velocity[2] += az * dt
            }

            displacement[0] += velocity[0] * dt
            displacement[1] += velocity[1] * dt
            displacement[2] += velocity[2] * dt
            
            linearAcceleration.set(floatArrayOf(ax, ay, az))
        }
    }

    private fun handleGyroscope(event: SensorEvent) {
        val timestamp = event.timestamp
        if (lastTimestampGyro == 0L) {
            lastTimestampGyro = timestamp
            return
        }
        val dt = (timestamp - lastTimestampGyro) / 1_000_000_000.0f
        lastTimestampGyro = timestamp
        synchronized(stateLock) {
            rotationEuler[0] += event.values[0] * dt
            rotationEuler[1] += event.values[1] * dt
            rotationEuler[2] += event.values[2] * dt
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun start(sensorManager: SensorManager) {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop(sensorManager: SensorManager) {
        sensorManager.unregisterListener(this)
    }

    fun getRelativePose(): FloatArray {
        synchronized(stateLock) {
            val pose = floatArrayOf(
                displacement[0], displacement[1], displacement[2],
                rotationEuler[0], rotationEuler[1], rotationEuler[2]
            )
            displacement[0] = 0f; displacement[1] = 0f; displacement[2] = 0f
            rotationEuler[0] = 0f; rotationEuler[1] = 0f; rotationEuler[2] = 0f
            return pose
        }
    }
    
    fun reset() {
        synchronized(stateLock) {
            for (i in 0..2) {
                gravity[i] = 0f; velocity[i] = 0f; displacement[i] = 0f; rotationEuler[i] = 0f
            }
            lastTimestampAcc = 0L; lastTimestampGyro = 0L
            linearAcceleration.set(FloatArray(3) { 0f })
        }
    }
}
