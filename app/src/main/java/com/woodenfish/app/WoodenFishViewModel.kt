package com.woodenfish.app

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
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
import kotlin.math.cos

data class PlusOneParticle(val id: Int, val colorIndex: Int, val dx: Float, val dy: Float)

data class WoodenFishState(
    val todayCount: Int = 0, val totalCount: Long = 0,
    val particles: List<PlusOneParticle> = emptyList(),
    val showCelebration: Boolean = false,
    val notifyEnabled: Boolean = false,
    val notifyIntervalValue: Int = 1, val notifyIntervalUnit: String = "小时",
    val randomTime: Boolean = true, val notifyStartMin: Int = 480, val notifyEndMin: Int = 1260,
    val fixedTimeEnabled: Boolean = false, val fixedTimeMin: Int = 540,
    val showAgreement: Boolean = false, val showLegalPage: String? = null,
    val showMenu: Boolean = false, val tapTick: Int = 0,
    val aboutClickCount: Int = 0,
    val themeColorIndex: Int = THEME_BROWN,
    val themeMode: com.woodenfish.app.ui.theme.ThemeMode = com.woodenfish.app.ui.theme.ThemeMode.SYSTEM,
    val language: String = "zh-CN",
    val soundVolume: Float = 0.7f, val vibrationIntensity: Float = 0.8f, val tapSpeed: Float = 1.0f,
    val vibrationSupported: Boolean = true, val soundSupported: Boolean = true,
    val toastMessage: String? = null,
    val selectedCalendarName: String? = null,
)

class WoodenFishViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val vibrator: Vibrator
    private val notificationHelper = NotificationHelper(application)
    private val _state = MutableStateFlow(WoodenFishState())
    val state: StateFlow<WoodenFishState> = _state.asStateFlow()
    private var particleCounter = 0
    private var celebrationJob: Job? = null
    private var lastVibrateTime = 0L

    // 常驻 SoundPool：音效预加载，敲击即时播放（低延迟、快速连敲可叠加）
    private var soundPool: SoundPool? = null
    private var woodSoundId = 0
    @Volatile private var soundLoaded = false

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            application.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator

        val todayCount = prefs.getCount(); val totalCount = prefs.getTotalCount()
        _state.value = _state.value.copy(todayCount = todayCount, totalCount = totalCount,
            notifyEnabled = prefs.isNotificationEnabled(), notifyIntervalValue = prefs.getNotifyIntervalValue(),
            notifyIntervalUnit = prefs.getNotifyIntervalUnit(), randomTime = prefs.isRandomTime(),
            notifyStartMin = prefs.getNotificationStartMin(), notifyEndMin = prefs.getNotificationEndMin(),
            fixedTimeEnabled = prefs.isFixedTimeEnabled(), fixedTimeMin = prefs.getFixedTimeMin(),
            showAgreement = !prefs.hasAgreedTerms(), themeColorIndex = prefs.getThemeColorIndex(),
            themeMode = prefs.getThemeMode(), language = prefs.getLanguage(),
            soundVolume = prefs.getSoundVolume(), vibrationIntensity = prefs.getVibrationIntensity(), tapSpeed = prefs.getTapSpeed(),
            vibrationSupported = vibrator.hasVibrator(), soundSupported = true,
            selectedCalendarName = prefs.getSelectedCalendarName())
        setupSoundPool(application)
    }

    private fun setupSoundPool(context: Application) {
        try {
            val sp = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            soundPool = sp
            val file = java.io.File(context.cacheDir, "wood_sound.wav")
            if (!file.exists() || file.length() == 0L) {
                writeWav(file, synthesizeWoodPcm(1.0f))
            }
            woodSoundId = sp.load(file.absolutePath, 1)
            sp.setOnLoadCompleteListener { _, _, status -> if (status == 0) soundLoaded = true }
        } catch (_: Exception) {
            try { soundPool?.release() } catch (_: Exception) {}
            soundPool = null
        }
    }

    private fun synthesizeWoodPcm(vol: Float): ShortArray {
        val rate = 44100; val dur = 0.04; val f0 = 280.0
        val n = (rate * dur).toInt(); val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val env = exp(-t * 80.0)
            var s = 0.0
            for (h in 1..4) s += sin(2 * PI * f0 * h * t) * (1.0 / h) * env
            buf[i] = (s * vol * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun writeWav(file: java.io.File, samples: ShortArray) {
        val rate = 44100
        val dataSize = samples.size * 2
        java.io.FileOutputStream(file).use { fos ->
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII)); header.putInt(36 + dataSize); header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII)); header.putInt(16)
            header.putShort(1); header.putShort(1)
            header.putInt(rate); header.putInt(rate * 2)
            header.putShort(2); header.putShort(16)
            header.put("data".toByteArray(Charsets.US_ASCII)); header.putInt(dataSize)
            fos.write(header.array())
            val body = java.nio.ByteBuffer.allocate(dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            samples.forEach { body.putShort(it) }
            fos.write(body.array())
        }
    }

    fun onFishTap() {
        val newCount = prefs.incrementCountAndTotal(); val totalCount = prefs.getTotalCount()
        val sv = _state.value.soundVolume; val vi = _state.value.vibrationIntensity

        vibrate((25 * vi).toLong().coerceAtLeast(1))
        playWoodSound(sv)

        val spd = _state.value.tapSpeed
        val ci = Random.nextInt(com.woodenfish.app.ui.theme.PlusOneColors.size)
        val angle = Random.nextDouble(0.0, 2 * PI)
        val radius = Random.nextDouble(105.0, 118.0)
        val p = PlusOneParticle(particleCounter++, ci, (cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
        // 单次状态更新；粒子数上限 8 个，快速连敲时丢弃最旧的
        val newParticles = (_state.value.particles + p).takeLast(8)
        _state.value = _state.value.copy(todayCount = newCount, totalCount = totalCount, particles = newParticles, tapTick = _state.value.tapTick + 1)
        viewModelScope.launch { delay((1200 / spd).toLong()); _state.value = _state.value.copy(particles = _state.value.particles.filter { it.id != p.id }) }
        if (newCount == 1000 && !prefs.hasCelebratedToday()) { prefs.markCelebrated(); triggerCelebration() }
    }

    private fun playWoodSound(vol: Float) {
        val sp = soundPool
        if (sp != null && soundLoaded && woodSoundId != 0) {
            sp.play(woodSoundId, vol, vol, 1, 0, 1f)
        } else {
            // 兜底：SoundPool 未就绪时用一次性 AudioTrack
            try {
                val buf = synthesizeWoodPcm(vol)
                val at = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(buf.size * 2).build()
                at.write(buf, 0, buf.size); at.play(); at.release()
            } catch (_: Exception) {
                try { val t = ToneGenerator(AudioManager.STREAM_NOTIFICATION, (vol * 100).toInt()); t.startTone(ToneGenerator.TONE_PROP_NACK, 40); Thread.sleep(50); t.release() } catch (_: Exception) {}
            }
        }
    }

    private fun triggerCelebration() {
        _state.value = _state.value.copy(showCelebration = true)
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100, 80, 100, 80, 200, 100, 300), intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255), -1))
        playWoodSound(0.9f); celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch { delay(4000); _state.value = _state.value.copy(showCelebration = false) }
    }

    private fun vibrate(ms: Long) {
        // 60ms 节流：快速连敲时跳过部分振动调用（人感知不出差别，减少系统调用开销）
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastVibrateTime < 60) return
        lastVibrateTime = now
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(ms, (255 * _state.value.vibrationIntensity).toInt().coerceIn(1, 255)))
    }

    fun toast(msg: String) { _state.value = _state.value.copy(toastMessage = msg) }
    fun clearToast() { _state.value = _state.value.copy(toastMessage = null) }
    fun toggleMenu() { _state.value = _state.value.copy(showMenu = !_state.value.showMenu) }
    fun closeMenu() { _state.value = _state.value.copy(showMenu = false) }
    fun updateNotificationEnabled(e: Boolean) {
        prefs.setNotificationEnabled(e)
        _state.value = _state.value.copy(notifyEnabled = e)
        if (!e) {
            CalendarSync.deleteEvent(getApplication(), prefs.getCalendarEventId().takeIf { it > 0 })
            prefs.setCalendarEventId(-1)
            notificationHelper.cancelAll()
        } else if (prefs.isFixedTimeEnabled()) {
            // 固定时间模式：日历事件已存在，无需额外调度
        } else {
            notificationHelper.scheduleNotifications(prefs)
        }
    }
    fun updateInterval(v: Int, u: String) { prefs.setNotifyIntervalValue(v); prefs.setNotifyIntervalUnit(u); _state.value = _state.value.copy(notifyIntervalValue = v, notifyIntervalUnit = u, randomTime = false); notificationHelper.scheduleNotifications(prefs) }
    fun selectRandomTime() {
        val wasFixed = _state.value.fixedTimeEnabled
        prefs.setRandomTime(true); prefs.setFixedTimeEnabled(false)
        _state.value = _state.value.copy(randomTime = true, fixedTimeEnabled = false)
        if (wasFixed) { CalendarSync.deleteEvent(getApplication(), prefs.getCalendarEventId().takeIf { it > 0 }); prefs.setCalendarEventId(-1) }
        notificationHelper.scheduleNotifications(prefs)
    }
    fun selectInterval() {
        val wasFixed = _state.value.fixedTimeEnabled
        prefs.setRandomTime(false); prefs.setFixedTimeEnabled(false)
        _state.value = _state.value.copy(randomTime = false, fixedTimeEnabled = false)
        if (wasFixed) { CalendarSync.deleteEvent(getApplication(), prefs.getCalendarEventId().takeIf { it > 0 }); prefs.setCalendarEventId(-1) }
        notificationHelper.scheduleNotifications(prefs)
    }
    fun enableFixedTime(calendarId: Long, calendarName: String): Boolean {
        val newId = CalendarSync.writeDailyReminder(getApplication(), prefs.getFixedTimeMin(), calendarId, prefs.getCalendarEventId().takeIf { it > 0 })
        if (newId != null) {
            prefs.setCalendarEventId(newId); prefs.setFixedTimeEnabled(true)
            prefs.setSelectedCalendarId(calendarId); prefs.setSelectedCalendarName(calendarName)
            _state.value = _state.value.copy(fixedTimeEnabled = true, selectedCalendarName = calendarName)
            notificationHelper.cancelAll()
            return true
        }
        toast(if (_state.value.language == "en") "Failed to write calendar" else if (_state.value.language == "zh-TW") "寫入日曆失敗" else "写入日历失败")
        return false
    }
    fun updateFixedTimeMin(m: Int) {
        val newId = CalendarSync.writeDailyReminder(getApplication(), m, prefs.getSelectedCalendarId().takeIf { it > 0 }, prefs.getCalendarEventId().takeIf { it > 0 })
        if (newId != null) {
            prefs.setCalendarEventId(newId); prefs.setFixedTimeMin(m); prefs.setFixedTimeEnabled(true)
            _state.value = _state.value.copy(fixedTimeMin = m, fixedTimeEnabled = true)
            toast(if (_state.value.language == "en") "Time updated in calendar" else if (_state.value.language == "zh-TW") "時間已更新到日曆" else "时间已更新到日历")
        } else {
            toast(if (_state.value.language == "en") "Failed to update calendar" else if (_state.value.language == "zh-TW") "更新日曆失敗" else "更新日历失败")
        }
    }
    fun updateNotifyStartMin(m: Int) { prefs.setNotificationStartMin(m); _state.value = _state.value.copy(notifyStartMin = m); notificationHelper.scheduleNotifications(prefs) }
    fun updateNotifyEndMin(m: Int) { prefs.setNotificationEndMin(m); _state.value = _state.value.copy(notifyEndMin = m); notificationHelper.scheduleNotifications(prefs) }
    fun setThemeColor(i: Int) { prefs.setThemeColorIndex(i); _state.value = _state.value.copy(themeColorIndex = i) }
    fun setThemeMode(m: com.woodenfish.app.ui.theme.ThemeMode) { prefs.setThemeMode(m); _state.value = _state.value.copy(themeMode = m) }
    fun setLanguage(l: String) { prefs.setLanguage(l); _state.value = _state.value.copy(language = l) }
    fun setSoundVolume(v: Float) { prefs.setSoundVolume(v); _state.value = _state.value.copy(soundVolume = v) }
    fun setVibrationIntensity(v: Float) { prefs.setVibrationIntensity(v); _state.value = _state.value.copy(vibrationIntensity = v) }
    fun setTapSpeed(v: Float) { prefs.setTapSpeed(v); _state.value = _state.value.copy(tapSpeed = v) }
    fun downloadAndInstall(context: android.content.Context, url: String) {
        toast(if (_state.value.language == "en") "Downloading update..." else if (_state.value.language == "zh-TW") "正在下載更新..." else "正在下载更新...")
        Updater.downloadApk(context, url, onProgress = {}, onResult = { file ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    if (file != null) {
                        toast(if (_state.value.language == "en") "Download complete" else if (_state.value.language == "zh-TW") "下載完成" else "下载完成")
                        Updater.install(context, file)
                    } else {
                        toast(if (_state.value.language == "en") "Download failed" else if (_state.value.language == "zh-TW") "下載失敗" else "下载失败")
                    }
                } catch (_: Exception) {
                }
            }
        })
    }
    fun agreeToTerms() { prefs.setAgreedTerms(); _state.value = _state.value.copy(showAgreement = false) }
    fun showLegalPage(p: String) { _state.value = _state.value.copy(showLegalPage = p) }
    fun dismissLegalPage() { _state.value = _state.value.copy(showLegalPage = null) }
    fun onVersionClick() { _state.value = _state.value.copy(aboutClickCount = _state.value.aboutClickCount + 1) }
    fun resetAboutClicks() { _state.value = _state.value.copy(aboutClickCount = 0) }
    override fun onCleared() { super.onCleared(); celebrationJob?.cancel(); try { soundPool?.release() } catch (_: Exception) {} }
}
