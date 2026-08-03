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
import com.woodenfish.app.ui.theme.THEME_BROWN
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PlusOneParticle(val id: Int, val colorIndex: Int, val positionIndex: Int)

data class WoodenFishState(
    val todayCount: Int = 0, val totalCount: Long = 0,
    val particles: List<PlusOneParticle> = emptyList(),
    val showCelebration: Boolean = false,
    val notifyEnabled: Boolean = false,
    val notifyIntervalValue: Int = 1, val notifyIntervalUnit: String = "小时",
    val randomTime: Boolean = true, val notifyStart: Int = 8, val notifyEnd: Int = 21,
    val showAgreement: Boolean = false, val showLegalPage: String? = null,
    val showMenu: Boolean = false, val hammerOffset: Float = 0f,
    val aboutClickCount: Int = 0,
    val themeColorIndex: Int = THEME_BROWN,
    val themeMode: com.woodenfish.app.ui.theme.ThemeMode = com.woodenfish.app.ui.theme.ThemeMode.SYSTEM,
    val language: String = "zh-CN",
    val soundVolume: Float = 0.7f, val vibrationIntensity: Float = 0.8f,
    val vibrationSupported: Boolean = true, val soundSupported: Boolean = true,
    val toastMessage: String? = null,
)

class WoodenFishViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val vibrator: Vibrator
    private val audioManager = application.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    private val notificationHelper = NotificationHelper(application)

    private val _state = MutableStateFlow(WoodenFishState())
    val state: StateFlow<WoodenFishState> = _state.asStateFlow()

    private var particleCounter = 0
    private var celebrationJob: Job? = null

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            application.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }

        val todayCount = prefs.getCount()
        val totalCount = prefs.getTotalCount()
        _state.value = _state.value.copy(
            todayCount = todayCount, totalCount = totalCount,
            notifyEnabled = prefs.isNotificationEnabled(),
            notifyIntervalValue = prefs.getNotifyIntervalValue(),
            notifyIntervalUnit = prefs.getNotifyIntervalUnit(),
            randomTime = prefs.isRandomTime(),
            notifyStart = prefs.getNotificationStartHour(),
            notifyEnd = prefs.getNotificationEndHour(),
            showAgreement = !prefs.hasAgreedTerms(),
            themeColorIndex = prefs.getThemeColorIndex(),
            themeMode = prefs.getThemeMode(),
            language = prefs.getLanguage(),
            soundVolume = prefs.getSoundVolume(),
            vibrationIntensity = prefs.getVibrationIntensity(),
            vibrationSupported = vibrator.hasVibrator(),
            soundSupported = true, // ToneGenerator works on most devices
        )
    }

    fun onFishTap() {
        val newCount = prefs.incrementCount(); prefs.incrementTotal()
        val totalCount = prefs.getTotalCount()
        val sv = _state.value.soundVolume; val vi = _state.value.vibrationIntensity

        // Vibrate
        vibrate((25 * vi).toLong().coerceAtLeast(1))

        // Sound
        if (sv > 0f) playTapTone(sv)

        // Hammer animation
        _state.value = _state.value.copy(hammerOffset = 1f)
        viewModelScope.launch { delay(150); _state.value = _state.value.copy(hammerOffset = 0f) }

        // Particle
        val ci = Random.nextInt(com.woodenfish.app.ui.theme.PlusOneColors.size)
        val pi = Random.nextInt(3)
        val p = PlusOneParticle(particleCounter++, ci, pi)
        _state.value = _state.value.copy(todayCount = newCount, totalCount = totalCount, particles = _state.value.particles + p)
        viewModelScope.launch { delay(1200); _state.value = _state.value.copy(particles = _state.value.particles.filter { it.id != p.id }) }

        if (newCount == 1000 && !prefs.hasCelebratedToday()) { prefs.markCelebrated(); triggerCelebration() }
    }

    private fun triggerCelebration() {
        _state.value = _state.value.copy(showCelebration = true)
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100, 80, 100, 80, 200, 100, 300), intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255), -1))
        playTapTone(0.8f)
        celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch { delay(4000); _state.value = _state.value.copy(showCelebration = false) }
    }

    private fun playTapTone(vol: Float) {
        viewModelScope.launch {
            try {
                val t = ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, (vol * 100).toInt().coerceIn(1, 100))
                t.startTone(ToneGenerator.TONE_PROP_ACK, 40)
                kotlinx.coroutines.delay(60)
                t.release()
            } catch (_: Exception) {}
        }
    }

    private fun vibrate(ms: Long) {
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(ms, (255 * _state.value.vibrationIntensity).toInt().coerceIn(1, 255)))
    }

    // ── Toast ──
    fun toast(msg: String) { _state.value = _state.value.copy(toastMessage = msg) }
    fun clearToast() { _state.value = _state.value.copy(toastMessage = null) }

    // ── Menu ──
    fun toggleMenu() { _state.value = _state.value.copy(showMenu = !_state.value.showMenu) }
    fun closeMenu() { _state.value = _state.value.copy(showMenu = false) }

    // ── Notification ──
    fun updateNotificationEnabled(enabled: Boolean) {
        prefs.setNotificationEnabled(enabled); _state.value = _state.value.copy(notifyEnabled = enabled)
        notificationHelper.scheduleNotifications(prefs)
    }
    fun updateInterval(v: Int, u: String) { prefs.setNotifyIntervalValue(v); prefs.setNotifyIntervalUnit(u); _state.value = _state.value.copy(notifyIntervalValue = v, notifyIntervalUnit = u, randomTime = false); notificationHelper.scheduleNotifications(prefs) }
    fun updateRandomTime(r: Boolean) { prefs.setRandomTime(r); _state.value = _state.value.copy(randomTime = r); notificationHelper.scheduleNotifications(prefs) }
    fun updateTimeRange(s: Int, e: Int) { prefs.setNotificationStartHour(s); prefs.setNotificationEndHour(e); _state.value = _state.value.copy(notifyStart = s, notifyEnd = e); notificationHelper.scheduleNotifications(prefs) }

    // ── Theme ──
    fun setThemeColor(idx: Int) { prefs.setThemeColorIndex(idx); _state.value = _state.value.copy(themeColorIndex = idx) }
    fun setThemeMode(m: com.woodenfish.app.ui.theme.ThemeMode) { prefs.setThemeMode(m); _state.value = _state.value.copy(themeMode = m) }

    // ── Language ──
    fun setLanguage(l: String) { prefs.setLanguage(l); _state.value = _state.value.copy(language = l) }

    // ── Sound & Vibration ──
    fun setSoundVolume(v: Float) { prefs.setSoundVolume(v); _state.value = _state.value.copy(soundVolume = v) }
    fun setVibrationIntensity(v: Float) { prefs.setVibrationIntensity(v); _state.value = _state.value.copy(vibrationIntensity = v) }

    // ── Agreement ──
    fun agreeToTerms() { prefs.setAgreedTerms(); _state.value = _state.value.copy(showAgreement = false) }
    fun showLegalPage(p: String) { _state.value = _state.value.copy(showLegalPage = p) }
    fun dismissLegalPage() { _state.value = _state.value.copy(showLegalPage = null) }

    // ── About ──
    fun onVersionClick() { _state.value = _state.value.copy(aboutClickCount = _state.value.aboutClickCount + 1) }
    fun resetAboutClicks() { _state.value = _state.value.copy(aboutClickCount = 0) }

    override fun onCleared() { super.onCleared(); celebrationJob?.cancel() }
}
