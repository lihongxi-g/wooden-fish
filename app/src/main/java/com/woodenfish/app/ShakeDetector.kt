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
 * 采用"连续摇动"判定——在时间窗口内检测到多次摇晃（默认 1.5s 内 3 次）才触发，
 * 避免单次晃动/走路误触。触发后有较长冷却。
 * 注意：onShake 回调在传感器线程，不要在回调里做 UI 操作。
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** 单次摇晃判定阈值（m/s²）：用力晃一下约 12-20，走路/拿手机约 2-5 */
    private val threshold = 12f
    /** 连续摇动窗口（ms）：窗口内的摇晃次数达到要求才触发 */
    private val windowMs = 1500L
    /** 窗口内需要的摇晃次数 */
    private val requiredShakes = 3
    /** 触发后的冷却时间（ms）：防止一次连续摇动多次触发 */
    private val triggerCooldown = 3500L

    private val shakeTimes = ArrayDeque<Long>()
    private var lastTriggerTime = 0L

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
        if (delta <= threshold) return
        val now = SystemClock.elapsedRealtime()
        // 记录本次摇晃，清理窗口外的旧记录
        shakeTimes.addLast(now)
        while (shakeTimes.isNotEmpty() && now - shakeTimes.first() > windowMs) {
            shakeTimes.removeFirst()
        }
        // 窗口内摇晃次数达标 + 冷却期已过 → 触发
        if (shakeTimes.size >= requiredShakes && now - lastTriggerTime > triggerCooldown) {
            shakeTimes.clear()
            lastTriggerTime = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
