package com.woodenfish.app

import android.app.Application
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PlusOneParticle(
    val id: Int,
    val colorIndex: Int,
    val offsetX: Float, // random horizontal offset
)

data class WoodenFishState(
    val todayCount: Int = 0,
    val totalCount: Long = 0,
    val particles: List<PlusOneParticle> = emptyList(),
    val showCelebration: Boolean = false,
    val notifyEnabled: Boolean = false,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val notifyCount: Int = 3,
    val randomTime: Boolean = true,
    val notifyStart: Int = 8,
    val notifyEnd: Int = 21,
    val showSettings: Boolean = false,
)

class WoodenFishViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val vibrator: Vibrator
    private val notificationHelper = NotificationHelper(application)

    private val _state = MutableStateFlow(WoodenFishState())
    val state: StateFlow<WoodenFishState> = _state.asStateFlow()

    private var particleCounter = 0
    private var celebrationJob: Job? = null

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = application.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Load initial state
        val todayCount = prefs.getCount()
        val totalCount = prefs.getTotalCount()
        _state.value = _state.value.copy(
            todayCount = todayCount,
            totalCount = totalCount,
            notifyEnabled = prefs.isNotificationEnabled(),
            notifyHour = prefs.getNotificationHour(),
            notifyMinute = prefs.getNotificationMinute(),
            notifyCount = prefs.getNotificationCount(),
            randomTime = prefs.isRandomTime(),
            notifyStart = prefs.getNotificationStartHour(),
            notifyEnd = prefs.getNotificationEndHour(),
        )
    }

    fun onFishTap() {
        val newCount = prefs.incrementCount()
        prefs.incrementTotalCount()
        val totalCount = prefs.getTotalCount()

        // Vibrate
        vibrate(30)

        // Add a particle with random color and offset
        val colorIndex = Random.nextInt(com.woodenfish.app.ui.theme.PlusOneColors.size)
        val offsetX = Random.nextFloat() * 160f - 80f // -80 to +80
        val particle = PlusOneParticle(
            id = particleCounter++,
            colorIndex = colorIndex,
            offsetX = offsetX,
        )

        _state.value = _state.value.copy(
            todayCount = newCount,
            totalCount = totalCount,
            particles = _state.value.particles + particle,
        )

        // Remove particle after animation
        viewModelScope.launch {
            delay(1200)
            _state.value = _state.value.copy(
                particles = _state.value.particles.filter { it.id != particle.id }
            )
        }

        // Check 1000 celebration
        if (newCount == 1000 && !prefs.hasCelebratedToday()) {
            prefs.markCelebratedToday()
            triggerCelebration()
        }
    }

    private fun triggerCelebration() {
        _state.value = _state.value.copy(showCelebration = true)

        // Long vibration pattern
        if (vibrator.hasVibrator()) {
            val timings = longArrayOf(0, 100, 80, 100, 80, 100, 80, 200, 100, 300)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        }

        // Play celebration sound via ToneGenerator
        playCelebrationTone()

        celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch {
            delay(4000)
            _state.value = _state.value.copy(showCelebration = false)
        }
    }

    private fun playCelebrationTone() {
        viewModelScope.launch {
            try {
                val toneGen = ToneGenerator(
                    android.media.AudioManager.STREAM_NOTIFICATION, 80
                )
                // Rising arpeggio — like a simple fanfare
                val notes = intArrayOf(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE,
                    ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE,
                    ToneGenerator.TONE_CDMA_ONE_MIN_BEEP,
                    ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE,
                )
                for ((i, note) in notes.withIndex()) {
                    toneGen.startTone(note, 180)
                    delay(120)
                }
                toneGen.release()
            } catch (_: Exception) {
                // Some devices don't support ToneGenerator well
            }
        }
    }

    private fun vibrate(millis: Long) {
        if (vibrator.hasVibrator()) {
            val effect = VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }
    }

    fun toggleSettings() {
        _state.value = _state.value.copy(showSettings = !_state.value.showSettings)
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        prefs.setNotificationEnabled(enabled)
        _state.value = _state.value.copy(notifyEnabled = enabled)
        notificationHelper.scheduleNotifications(prefs)
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        prefs.setNotificationHour(hour)
        prefs.setNotificationMinute(minute)
        _state.value = _state.value.copy(notifyHour = hour, notifyMinute = minute)
        notificationHelper.scheduleNotifications(prefs)
    }

    fun updateNotificationCount(count: Int) {
        prefs.setNotificationCount(count)
        _state.value = _state.value.copy(notifyCount = count)
        notificationHelper.scheduleNotifications(prefs)
    }

    fun updateRandomTime(random: Boolean) {
        prefs.setRandomTime(random)
        _state.value = _state.value.copy(randomTime = random)
        notificationHelper.scheduleNotifications(prefs)
    }

    fun updateTimeRange(start: Int, end: Int) {
        prefs.setNotificationStartHour(start)
        prefs.setNotificationEndHour(end)
        _state.value = _state.value.copy(notifyStart = start, notifyEnd = end)
        notificationHelper.scheduleNotifications(prefs)
    }

    override fun onCleared() {
        super.onCleared()
        celebrationJob?.cancel()
    }
}
