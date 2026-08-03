package com.woodenfish.app

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.PI

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
    val customMode: Boolean = false, val customMediaPath: String? = null, val customAudioPath: String? = null,
)

class WoodenFishViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val vibrator: Vibrator
    private val notificationHelper = NotificationHelper(application)
    private val _state = MutableStateFlow(WoodenFishState())
    val state: StateFlow<WoodenFishState> = _state.asStateFlow()
    private var particleCounter = 0
    private var celebrationJob: Job? = null
    private var customPlayer: android.media.MediaPlayer? = null

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            application.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator

        val todayCount = prefs.getCount(); val totalCount = prefs.getTotalCount()
        _state.value = _state.value.copy(todayCount = todayCount, totalCount = totalCount,
            notifyEnabled = prefs.isNotificationEnabled(), notifyIntervalValue = prefs.getNotifyIntervalValue(),
            notifyIntervalUnit = prefs.getNotifyIntervalUnit(), randomTime = prefs.isRandomTime(),
            notifyStart = prefs.getNotificationStartHour(), notifyEnd = prefs.getNotificationEndHour(),
            showAgreement = !prefs.hasAgreedTerms(), themeColorIndex = prefs.getThemeColorIndex(),
            themeMode = prefs.getThemeMode(), language = prefs.getLanguage(),
            soundVolume = prefs.getSoundVolume(), vibrationIntensity = prefs.getVibrationIntensity(),
            vibrationSupported = vibrator.hasVibrator(), soundSupported = true,
            customMode = prefs.isCustomMode(), customMediaPath = prefs.getCustomMediaPath(),
            customAudioPath = prefs.getCustomAudioPath())
    }

    fun onFishTap() {
        val newCount = prefs.incrementCount(); prefs.incrementTotal(); val totalCount = prefs.getTotalCount()
        val sv = _state.value.soundVolume; val vi = _state.value.vibrationIntensity

        vibrate((25 * vi).toLong().coerceAtLeast(1))

        // Play sound: custom audio > wood synth
        val audioPath = prefs.getCustomAudioPath()
        if (prefs.isCustomMode() && audioPath != null) playCustomSound(audioPath, sv)
        else playWoodSound(sv)

        _state.value = _state.value.copy(hammerOffset = 1f)
        viewModelScope.launch { delay(150); _state.value = _state.value.copy(hammerOffset = 0f) }

        val ci = Random.nextInt(com.woodenfish.app.ui.theme.PlusOneColors.size); val pi = Random.nextInt(3)
        val p = PlusOneParticle(particleCounter++, ci, pi)
        _state.value = _state.value.copy(todayCount = newCount, totalCount = totalCount, particles = _state.value.particles + p)
        viewModelScope.launch { delay(1200); _state.value = _state.value.copy(particles = _state.value.particles.filter { it.id != p.id }) }
        if (newCount == 1000 && !prefs.hasCelebratedToday()) { prefs.markCelebrated(); triggerCelebration() }
    }

    private fun playCustomSound(path: String, vol: Float) {
        try {
            customPlayer?.release()
            customPlayer = android.media.MediaPlayer().apply {
                setDataSource(path); setVolume(vol, vol); prepare(); start()
                setOnCompletionListener { it.release() }
            }
        } catch (_: Exception) { playWoodSound(vol) }
    }

    private fun playWoodSound(vol: Float) {
        try {
            val rate = 44100; val dur = 0.04; val f0 = 280.0
            val n = (rate * dur).toInt(); val buf = ShortArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / rate
                val env = exp(-t * 80.0)
                var s = 0.0
                for (h in 1..4) s += sin(2 * PI * f0 * h * t) * (1.0 / h) * env
                buf[i] = (s * vol * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            val at = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(n * 2).build()
            at.write(buf, 0, n); at.play(); at.release()
        } catch (_: Exception) {
            try { val t = ToneGenerator(AudioManager.STREAM_NOTIFICATION, (vol * 100).toInt()); t.startTone(ToneGenerator.TONE_PROP_NACK, 40); Thread.sleep(50); t.release() } catch (_: Exception) {}
        }
    }

    private fun triggerCelebration() {
        _state.value = _state.value.copy(showCelebration = true)
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100, 80, 100, 80, 200, 100, 300), intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255), -1))
        playWoodSound(0.9f); celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch { delay(4000); _state.value = _state.value.copy(showCelebration = false) }
    }

    private fun vibrate(ms: Long) { if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(ms, (255 * _state.value.vibrationIntensity).toInt().coerceIn(1, 255))) }

    fun toast(msg: String) { _state.value = _state.value.copy(toastMessage = msg) }
    fun clearToast() { _state.value = _state.value.copy(toastMessage = null) }
    fun toggleMenu() { _state.value = _state.value.copy(showMenu = !_state.value.showMenu) }
    fun closeMenu() { _state.value = _state.value.copy(showMenu = false) }
    fun updateNotificationEnabled(e: Boolean) { prefs.setNotificationEnabled(e); _state.value = _state.value.copy(notifyEnabled = e); notificationHelper.scheduleNotifications(prefs) }
    fun updateInterval(v: Int, u: String) { prefs.setNotifyIntervalValue(v); prefs.setNotifyIntervalUnit(u); _state.value = _state.value.copy(notifyIntervalValue = v, notifyIntervalUnit = u, randomTime = false); notificationHelper.scheduleNotifications(prefs) }
    fun updateRandomTime(r: Boolean) { prefs.setRandomTime(r); _state.value = _state.value.copy(randomTime = r); notificationHelper.scheduleNotifications(prefs) }
    fun updateTimeRange(s: Int, e: Int) { prefs.setNotificationStartHour(s); prefs.setNotificationEndHour(e); _state.value = _state.value.copy(notifyStart = s, notifyEnd = e); notificationHelper.scheduleNotifications(prefs) }
    fun setThemeColor(i: Int) { prefs.setThemeColorIndex(i); _state.value = _state.value.copy(themeColorIndex = i) }
    fun setThemeMode(m: com.woodenfish.app.ui.theme.ThemeMode) { prefs.setThemeMode(m); _state.value = _state.value.copy(themeMode = m) }
    fun setLanguage(l: String) { prefs.setLanguage(l); _state.value = _state.value.copy(language = l) }
    fun setSoundVolume(v: Float) { prefs.setSoundVolume(v); _state.value = _state.value.copy(soundVolume = v) }
    fun setVibrationIntensity(v: Float) { prefs.setVibrationIntensity(v); _state.value = _state.value.copy(vibrationIntensity = v) }
    fun setCustomMedia(p: String?) { prefs.setCustomMediaPath(p); _state.value = _state.value.copy(customMediaPath = p) }
    fun setCustomAudio(p: String?) { prefs.setCustomAudioPath(p); _state.value = _state.value.copy(customAudioPath = p) }
    fun setCustomMode(v: Boolean) { prefs.setCustomMode(v); _state.value = _state.value.copy(customMode = v) }
    fun testCustomAudio(vol: Float) { val p = prefs.getCustomAudioPath(); if (p != null) playCustomSound(p, vol) }
    fun agreeToTerms() { prefs.setAgreedTerms(); _state.value = _state.value.copy(showAgreement = false) }
    fun showLegalPage(p: String) { _state.value = _state.value.copy(showLegalPage = p) }
    fun dismissLegalPage() { _state.value = _state.value.copy(showLegalPage = null) }
    fun onVersionClick() { _state.value = _state.value.copy(aboutClickCount = _state.value.aboutClickCount + 1) }
    fun resetAboutClicks() { _state.value = _state.value.copy(aboutClickCount = 0) }
    override fun onCleared() { super.onCleared(); celebrationJob?.cancel(); customPlayer?.release() }
}
