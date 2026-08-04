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
    // 主模式：0=木鱼 1=签筒抽签 2=骰子 3=转盘
    val mode: Int = 0,
    // 求签：phase 0=静置 1=摇晃 2=签弹出 3=已翻面
    val fortunePhase: Int = 0,
    val fortuneStick: FortuneStick? = null,
    val fortuneTick: Int = 0,
    val fortuneTriggerMode: String = "tap",   // "tap" 点按 / "shake" 摇一摇
    // 骰子
    val diceTriggerMode: String = "tap",      // "tap" / "shake"
    val diceWeights: List<Int> = listOf(1, 1, 1, 1, 1, 1),
    val diceLabels: List<String> = listOf("", "", "", "", "", ""),
    val diceFace: Int = 5,                    // 当前显示面
    val diceResult: Int = 5,                  // 本次结果
    val diceRolling: Boolean = false,
    val diceTick: Int = 0,
    // 转盘
    val spinnerSegments: List<SpinnerSegment> = defaultSegments(),
    val spinnerAngle: Float = 0f,        // 当前旋转角度
    val spinnerSpinning: Boolean = false,
    val spinnerResult: Int = -1,         // -1=未转 0..n-1=结果分区
    val spinnerTick: Int = 0,
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
    private var shakeDetector: ShakeDetector? = null

    // 常驻 SoundPool：音效预加载，敲击即时播放（低延迟、快速连敲可叠加）
    private var soundPool: SoundPool? = null
    private var woodSoundId = 0
    @Volatile private var soundLoaded = false

    /** 自动跟随系统语言：用户手动设置过则优先，否则按系统 Locale 映射 */
    private fun detectSystemLanguage(): String {
        val l = java.util.Locale.getDefault()
        return when (l.language) {
            "zh" -> if (l.script == "Hant" || l.country == "TW" || l.country == "HK" || l.country == "MO") "zh-TW" else "zh-CN"
            "en" -> "en"
            "fr" -> "fr"
            "ru" -> "ru"
            "es" -> "es"
            else -> "en"
        }
    }

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
            themeMode = prefs.getThemeMode(), language = prefs.getLanguage() ?: detectSystemLanguage(),
            soundVolume = prefs.getSoundVolume(), vibrationIntensity = prefs.getVibrationIntensity(), tapSpeed = prefs.getTapSpeed(),
            vibrationSupported = vibrator.hasVibrator(), soundSupported = true,
            selectedCalendarName = prefs.getSelectedCalendarName(),
            fortuneTriggerMode = prefs.getFortuneTriggerMode(), diceTriggerMode = prefs.getDiceTriggerMode(),
            diceWeights = prefs.getDiceWeights(), diceLabels = prefs.getDiceLabels(),
            spinnerSegments = prefs.getSpinnerSegments())
        setupSoundPool(application)
        // 摇一摇传感器：仅当当前模式+触发方式需要时开启（updateShakeListener 会按需 start/stop）
        shakeDetector = ShakeDetector(application) { onShakeDetected() }
    }

    /** 传感器回调（工作线程）：按当前模式和触发方式分发到抽签 / 掷骰 */
    private fun onShakeDetected() {
        val s = _state.value
        when {
            s.mode == 1 && s.fortuneTriggerMode == "shake" && s.fortunePhase == 0 -> tapFortuneTube()
            s.mode == 2 && s.diceTriggerMode == "shake" && !s.diceRolling -> rollDice()
        }
    }

    /** 按需开关摇一摇监听：只有"当前模式用摇一摇触发"时才耗电 */
    private fun updateShakeListener() {
        val s = _state.value
        val needShake = (s.mode == 1 && s.fortuneTriggerMode == "shake") || (s.mode == 2 && s.diceTriggerMode == "shake")
        if (needShake) shakeDetector?.start() else shakeDetector?.stop()
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
            // 播放速率跟随互动速度：0.5→0.8 低沉、1.0→1.0、1.25→1.1 清亮
            val rate = 0.6f + 0.4f * _state.value.tapSpeed
            sp.play(woodSoundId, vol, vol, 1, 0, rate)
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

    // ─── 模式切换（0=木鱼 1=签筒 2=骰子 3=转盘，左右箭头循环）───
    fun switchMode(delta: Int) {
        val newMode = ((_state.value.mode + delta) % 4 + 4) % 4
        _state.value = _state.value.copy(
            mode = newMode,
            fortunePhase = 0, fortuneStick = null,
            fortuneTick = _state.value.fortuneTick + 1,
            diceRolling = false, spinnerSpinning = false
        )
        updateShakeListener()
    }

    /** 点击签筒（点按模式）或摇一摇（shake 模式）触发：摇晃 1.4s 后弹出木签 */
    fun tapFortuneTube() {
        if (_state.value.fortunePhase != 0) return
        val stick = FortuneData.draw()
        _state.value = _state.value.copy(fortunePhase = 1, fortuneStick = stick, fortuneTick = _state.value.fortuneTick + 1)
        // 摇一摇模式：摇晃期间模拟签筒内签碰撞的连续震动
        if (_state.value.fortuneTriggerMode == "shake") vibrateShakeTube()
        viewModelScope.launch {
            delay(1400)
            if (_state.value.fortunePhase == 1) {
                vibrate(40)
                playWoodSound(_state.value.soundVolume)
                _state.value = _state.value.copy(fortunePhase = 2, fortuneTick = _state.value.fortuneTick + 1)
            }
        }
    }

    /** 点击弹出的木签：翻面显示签文 */
    fun flipFortuneStick() {
        if (_state.value.fortunePhase == 2) {
            _state.value = _state.value.copy(fortunePhase = 3, fortuneTick = _state.value.fortuneTick + 1)
        }
    }

    /** 再抽一签：回到静置 */
    fun resetFortune() {
        _state.value = _state.value.copy(fortunePhase = 0, fortuneStick = null, fortuneTick = _state.value.fortuneTick + 1)
    }

    // ─── 掷骰子 ───
    /** 掷骰：按权重随机结果，播放桌面弹跳震动；动画由 UI 层根据 diceTick 驱动 */
    fun rollDice() {
        if (_state.value.diceRolling) return
        val weights = _state.value.diceWeights
        val total = weights.sum().coerceAtLeast(1)
        var r = Random.nextInt(total)
        var face = 1
        for (i in 0 until 6) {
            r -= weights[i].coerceAtLeast(0)
            if (r < 0) { face = i + 1; break }
        }
        vibrateDiceBounce()
        _state.value = _state.value.copy(diceRolling = true, diceResult = face, diceTick = _state.value.diceTick + 1)
        viewModelScope.launch {
            delay(1300)
            _state.value = _state.value.copy(diceRolling = false, diceFace = face)
        }
    }

    // ─── 抽签 / 掷骰触发模式 ───
    fun setFortuneTriggerMode(m: String) {
        prefs.setFortuneTriggerMode(m)
        _state.value = _state.value.copy(fortuneTriggerMode = m)
        updateShakeListener()
    }

    fun setDiceTriggerMode(m: String) {
        prefs.setDiceTriggerMode(m)
        _state.value = _state.value.copy(diceTriggerMode = m)
        updateShakeListener()
    }

    // ─── 骰子设置 ───
    fun setDiceWeight(i: Int, w: Int) {
        prefs.setDiceWeight(i, w)
        _state.value = _state.value.copy(diceWeights = prefs.getDiceWeights())
    }

    fun setDiceLabel(i: Int, label: String) {
        prefs.setDiceLabel(i, label)
        _state.value = _state.value.copy(diceLabels = prefs.getDiceLabels())
    }

    fun resetDiceSettings() {
        prefs.resetDiceSettings()
        _state.value = _state.value.copy(diceWeights = prefs.getDiceWeights(), diceLabels = prefs.getDiceLabels())
    }

    // ─── 转盘 ───
    /** 转动转盘：按权重选结果分区，目标角度 = 指针停在该分区中心（多圈+定位），动画由 UI 层驱动 */
    fun spinSpinner() {
        if (_state.value.spinnerSpinning) return
        val segs = _state.value.spinnerSegments
        if (segs.isEmpty()) return
        val total = segs.sumOf { it.weight.coerceAtLeast(0) }.coerceAtLeast(1)
        var r = Random.nextInt(total)
        var idx = 0
        for (i in segs.indices) {
            r -= segs[i].weight.coerceAtLeast(0)
            if (r < 0) { idx = i; break }
        }
        // 目标角度：指针在顶部（-90°），分区 i 中心角 = (i+0.5)*360/N（从右侧顺时针）
        val n = segs.size
        val center = (idx + 0.5f) * (360f / n)
        val current = _state.value.spinnerAngle
        val delta = ((270f - ((center + current) % 360f)) % 360f + 360f) % 360f
        val target = current + 5 * 360f + delta
        vibrate(30)
        _state.value = _state.value.copy(
            spinnerSpinning = true, spinnerResult = idx, spinnerTick = _state.value.spinnerTick + 1,
            spinnerAngle = target
        )
        viewModelScope.launch {
            delay(3600)
            _state.value = _state.value.copy(spinnerSpinning = false)
        }
    }

    fun addSpinnerSegment() {
        val segs = _state.value.spinnerSegments
        if (segs.size >= 8) return
        val newList = segs + SpinnerSegment("", 1)
        prefs.setSpinnerSegments(newList)
        _state.value = _state.value.copy(spinnerSegments = newList, spinnerResult = -1)
    }

    fun removeSpinnerSegment(i: Int) {
        val segs = _state.value.spinnerSegments
        if (segs.size <= 2) return
        val newList = segs.filterIndexed { idx, _ -> idx != i }
        prefs.setSpinnerSegments(newList)
        _state.value = _state.value.copy(spinnerSegments = newList, spinnerResult = -1)
    }

    fun updateSpinnerSegment(i: Int, name: String? = null, weight: Int? = null) {
        val segs = _state.value.spinnerSegments
        if (i !in segs.indices) return
        val newList = segs.mapIndexed { idx, s ->
            if (idx == i) SpinnerSegment(name ?: s.name, weight ?: s.weight) else s
        }
        prefs.setSpinnerSegments(newList)
        _state.value = _state.value.copy(spinnerSegments = newList, spinnerResult = -1)
    }

    fun resetSpinner() {
        prefs.setSpinnerSegments(defaultSegments())
        _state.value = _state.value.copy(spinnerSegments = defaultSegments(), spinnerResult = -1, spinnerAngle = 0f)
    }

    // ─── 细化震动 ───
    /** 摇签筒震动（仅摇一摇抽签）：连续短震模拟筒内签碰撞，力度先强后缓 */
    private fun vibrateShakeTube() {
        if (!vibrator.hasVibrator()) return
        val v = (255 * _state.value.vibrationIntensity).toInt().coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createWaveform(
            longArrayOf(0, 55, 45, 60, 45, 70, 45, 75, 45, 80, 50, 85, 50, 90, 55, 85, 55, 70, 50, 55, 40),
            intArrayOf(0, (v * 0.5f).toInt(), 0, (v * 0.65f).toInt(), 0, (v * 0.8f).toInt(), 0, (v * 0.9f).toInt(), 0, (v * 1.0f).toInt(), 0, (v * 1.0f).toInt(), 0, (v * 0.85f).toInt(), 0, (v * 0.7f).toInt(), 0, (v * 0.55f).toInt(), 0, (v * 0.4f).toInt(), 0),
            -1
        ))
    }

    /** 掷骰震动：模拟骰子在桌面上连续弹跳，力度逐次衰减 */
    private fun vibrateDiceBounce() {
        if (!vibrator.hasVibrator()) return
        val v = (255 * _state.value.vibrationIntensity).toInt().coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createWaveform(
            longArrayOf(0, 40, 60, 50, 65, 60, 75, 75, 90, 90, 110, 110, 130, 140, 160, 170),
            intArrayOf(0, (v * 1.0f).toInt(), 0, (v * 0.85f).toInt(), 0, (v * 0.7f).toInt(), 0, (v * 0.55f).toInt(), 0, (v * 0.4f).toInt(), 0, (v * 0.3f).toInt(), 0, (v * 0.2f).toInt(), 0, (v * 0.12f).toInt()),
            -1
        ))
    }

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

    fun agreeToTerms() { prefs.setAgreedTerms(); _state.value = _state.value.copy(showAgreement = false) }
    fun showLegalPage(p: String) { _state.value = _state.value.copy(showLegalPage = p) }
    fun dismissLegalPage() { _state.value = _state.value.copy(showLegalPage = null) }
    fun onVersionClick() { _state.value = _state.value.copy(aboutClickCount = _state.value.aboutClickCount + 1) }
    fun resetAboutClicks() { _state.value = _state.value.copy(aboutClickCount = 0) }
    override fun onCleared() { super.onCleared(); celebrationJob?.cancel(); shakeDetector?.stop(); try { soundPool?.release() } catch (_: Exception) {} }
}
