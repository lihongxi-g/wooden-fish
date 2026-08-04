package com.woodenfish.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 摇一摇检测：基于加速度传感器。
 * 加速度矢量模长偏离重力加速度超过阈值即判定一次"摇动"，带冷却防抖。
 * 注意：onShake 回调在传感器线程，不要在回调里做 UI 操作。
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** 触发阈值（m/s²）：用力晃一下约 12-20，走路/拿手机约 2-5 */
    private val threshold = 13f
    private val cooldownMs = 1200L
    private var lastShakeTime = 0L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)
        val now = SystemClock.elapsedRealtime()
        if (delta > threshold && now - lastShakeTime > cooldownMs) {
            lastShakeTime = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
