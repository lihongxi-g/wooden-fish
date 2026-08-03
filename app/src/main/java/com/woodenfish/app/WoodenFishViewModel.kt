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
    val positionIndex: Int, // 0=左上, 1=正上, 2=右上
)

data class WoodenFishState(
    val todayCount: Int = 0,
    val totalCount: Long = 0,
    val particles: List<PlusOneParticle> = emptyList(),
    val showCelebration: Boolean = false,
    val notifyEnabled: Boolean = false,
    val notifyIntervalValue: Int = 1,
    val notifyIntervalUnit: String = "小时", // 小时, 分钟, 天
    val randomTime: Boolean = true,
    val notifyStart: Int = 8,
    val notifyEnd: Int = 21,
    val showSettings: Boolean = false,
    val showAgreement: Boolean = false,
    val showLegalPage: String? = null,
    val showMenu: Boolean = false,
    val themeMode: com.woodenfish.app.ui.theme.ThemeMode = com.woodenfish.app.ui.theme.ThemeMode.SYSTEM,
    val language: String = "zh-CN", // zh-CN, zh-TW, en
    val hammerOffset: Float = 0f, // animation for hammer
    val aboutClickCount: Int = 0,
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
            application.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }

        val todayCount = prefs.getCount()
        val totalCount = prefs.getTotalCount()
        _state.value = _state.value.copy(
            todayCount = todayCount,
            totalCount = totalCount,
            notifyEnabled = prefs.isNotificationEnabled(),
            notifyIntervalValue = prefs.getNotifyIntervalValue(),
            notifyIntervalUnit = prefs.getNotifyIntervalUnit(),
            randomTime = prefs.isRandomTime(),
            notifyStart = prefs.getNotificationStartHour(),
            notifyEnd = prefs.getNotificationEndHour(),
            showAgreement = !prefs.hasAgreedTerms(),
            themeMode = prefs.getThemeMode(),
            language = prefs.getLanguage(),
        )
    }

    fun onFishTap() {
        val newCount = prefs.incrementCount()
        prefs.incrementTotal()
        val totalCount = prefs.getTotalCount()

        // Vibrate + hammer animation
        vibrate(25)
        _state.value = _state.value.copy(hammerOffset = 1f)
        viewModelScope.launch {
            delay(150)
            _state.value = _state.value.copy(hammerOffset = 0f)
        }

        // Random +1 particle
        val colorIndex = Random.nextInt(com.woodenfish.app.ui.theme.PlusOneColors.size)
        val posIndex = Random.nextInt(3)
        val particle = PlusOneParticle(id = particleCounter++, colorIndex = colorIndex, positionIndex = posIndex)

        // Update count state
        _state.value = _state.value.copy(
            todayCount = newCount,
            totalCount = totalCount,
            particles = _state.value.particles + particle,
        )

        viewModelScope.launch {
            delay(1200)
            _state.value = _state.value.copy(particles = _state.value.particles.filter { it.id != particle.id })
        }

        // Check 1000 celebration
        if (newCount == 1000 && !prefs.hasCelebratedToday()) {
            prefs.markCelebrated()
            triggerCelebration()
        }
    }

    private fun triggerCelebration() {
        _state.value = _state.value.copy(showCelebration = true)
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 100, 80, 100, 80, 200, 100, 300),
                intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255), -1
            ))
        }
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
                val tone = ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80)
                val notes = intArrayOf(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE,
                    ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE,
                    ToneGenerator.TONE_CDMA_ONE_MIN_BEEP,
                    ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE,
                )
                for ((_, note) in notes.withIndex()) {
                    tone.startTone(note, 180)
                    delay(120)
                }
                tone.release()
            } catch (_: Exception) {}
        }
    }

    private fun vibrate(ms: Long) {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // --- Menu ---
    fun toggleMenu() { _state.value = _state.value.copy(showMenu = !_state.value.showMenu) }
    fun closeMenu() { _state.value = _state.value.copy(showMenu = false) }

    // --- Settings ---
    fun toggleSettings() { _state.value = _state.value.copy(showSettings = !_state.value.showSettings) }

    // --- Notification ---
    fun updateNotificationEnabled(enabled: Boolean) {
        prefs.setNotificationEnabled(enabled)
        _state.value = _state.value.copy(notifyEnabled = enabled)
        notificationHelper.scheduleNotifications(prefs)
    }

    fun updateInterval(value: Int, unit: String) {
        prefs.setNotifyIntervalValue(value)
        prefs.setNotifyIntervalUnit(unit)
        _state.value = _state.value.copy(notifyIntervalValue = value, notifyIntervalUnit = unit, randomTime = false)
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

    // --- Theme ---
    fun setThemeMode(mode: com.woodenfish.app.ui.theme.ThemeMode) {
        prefs.setThemeMode(mode)
        _state.value = _state.value.copy(themeMode = mode)
    }

    // --- Language ---
    fun setLanguage(lang: String) {
        prefs.setLanguage(lang)
        _state.value = _state.value.copy(language = lang)
    }

    // --- Agreement ---
    fun agreeToTerms() { prefs.setAgreedTerms(); _state.value = _state.value.copy(showAgreement = false) }
    fun showLegalPage(page: String) { _state.value = _state.value.copy(showLegalPage = page) }
    fun dismissLegalPage() { _state.value = _state.value.copy(showLegalPage = null) }

    // --- About ---
    fun onVersionClick() {
        val newCount = _state.value.aboutClickCount + 1
        _state.value = _state.value.copy(aboutClickCount = newCount)
    }
    fun resetAboutClicks() { _state.value = _state.value.copy(aboutClickCount = 0) }

    override fun onCleared() {
        super.onCleared()
        celebrationJob?.cancel()
    }
}
