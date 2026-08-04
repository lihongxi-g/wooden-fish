package com.woodenfish.app

import android.content.Context
import android.content.SharedPreferences
import com.woodenfish.app.ui.theme.THEME_BROWN
import com.woodenfish.app.ui.theme.ThemeMode
import java.time.LocalDate

class PreferencesManager(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("doki_prefs", Context.MODE_PRIVATE)
    private fun t() = LocalDate.now().toString()

    // ─── Count (keys unchanged for backward compat) ───
    fun getCount(): Int { val d = p.getString("last_date", ""); return if (d == t()) p.getInt("count", 0) else 0 }
    fun incrementCountAndTotal(): Int {
        val td = t()
        if (p.getString("last_date", "") != td) {
            p.edit().putString("last_date", td).putInt("count", 1).putBoolean("celebrated_today", false).putLong("total_count", getTotalCount() + 1).apply()
            return 1
        }
        val c = p.getInt("count", 0) + 1
        p.edit().putInt("count", c).putLong("total_count", getTotalCount() + 1).apply()
        return c
    }
    fun getTotalCount() = p.getLong("total_count", 0L)
    fun hasCelebratedToday() = p.getString("last_date", "") == t() && p.getBoolean("celebrated_today", false)
    fun markCelebrated() = p.edit().putBoolean("celebrated_today", true).apply()

    // ─── Agreement ───
    fun hasAgreedTerms() = p.getBoolean("agreed_terms", false)
    fun setAgreedTerms() = p.edit().putBoolean("agreed_terms", true).apply()

    // ─── Notifications ───
    fun isNotificationEnabled() = p.getBoolean("notify_enabled", false)
    fun setNotificationEnabled(v: Boolean) = p.edit().putBoolean("notify_enabled", v).apply()
    fun getNotifyIntervalValue() = p.getInt("interval_val", 1)
    fun setNotifyIntervalValue(v: Int) = p.edit().putInt("interval_val", v).apply()
    fun getNotifyIntervalUnit() = p.getString("interval_unit", "小时") ?: "小时"
    fun setNotifyIntervalUnit(u: String) = p.edit().putString("interval_unit", u).apply()
    fun isRandomTime() = p.getBoolean("random_time", true)
    fun setRandomTime(v: Boolean) = p.edit().putBoolean("random_time", v).apply()
    fun getNotificationStartMin(): Int {
        return if (p.contains("notify_start_min")) p.getInt("notify_start_min", 480)
        else p.getInt("notify_start", 8) * 60
    }
    fun setNotificationStartMin(m: Int) = p.edit().putInt("notify_start_min", m).apply()
    fun getNotificationEndMin(): Int {
        return if (p.contains("notify_end_min")) p.getInt("notify_end_min", 1260)
        else p.getInt("notify_end", 21) * 60
    }
    fun setNotificationEndMin(m: Int) = p.edit().putInt("notify_end_min", m).apply()

    // ─── Fixed time (calendar) ───
    fun isFixedTimeEnabled() = p.getBoolean("fixed_time_enabled", false)
    fun setFixedTimeEnabled(v: Boolean) = p.edit().putBoolean("fixed_time_enabled", v).apply()
    fun getFixedTimeMin() = p.getInt("fixed_time_min", 540)
    fun setFixedTimeMin(m: Int) = p.edit().putInt("fixed_time_min", m).apply()
    fun getCalendarEventId() = p.getLong("calendar_event_id", -1L)
    fun setCalendarEventId(id: Long) = p.edit().putLong("calendar_event_id", id).apply()
    fun getSelectedCalendarId() = p.getLong("selected_calendar_id", -1L)
    fun setSelectedCalendarId(id: Long) = p.edit().putLong("selected_calendar_id", id).apply()
    fun getSelectedCalendarName() = p.getString("selected_calendar_name", null)
    fun setSelectedCalendarName(n: String?) = p.edit().putString("selected_calendar_name", n).apply()

    // ─── First launch ───
    fun isFirstLaunch() = p.getBoolean("first_launch", true)
    fun markLaunched() = p.edit().putBoolean("first_launch", false).apply()

    // ─── Theme ───
    fun getThemeColorIndex() = p.getInt("theme_color", THEME_BROWN)
    fun setThemeColorIndex(i: Int) = p.edit().putInt("theme_color", i).apply()
    fun getThemeMode(): ThemeMode = try { ThemeMode.valueOf(p.getString("theme_mode", "SYSTEM") ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
    fun setThemeMode(m: ThemeMode) = p.edit().putString("theme_mode", m.name).apply()

    // ─── Language ───
    /** null = 用户从未手动设置过语言（此时自动跟随系统语言） */
    fun getLanguage(): String? = p.getString("language", null)
    fun setLanguage(l: String) = p.edit().putString("language", l).apply()

    // ─── Sound & Vibration ───
    fun getSoundVolume() = p.getFloat("sound_vol", 0.7f)
    fun setSoundVolume(v: Float) = p.edit().putFloat("sound_vol", v).apply()
    fun getVibrationIntensity() = p.getFloat("vib_intensity", 0.8f)
    fun setVibrationIntensity(v: Float) = p.edit().putFloat("vib_intensity", v).apply()

    // ─── Interaction speed ───
    fun getTapSpeed() = p.getFloat("tap_speed", 1.0f)
    fun setTapSpeed(v: Float) = p.edit().putFloat("tap_speed", v).apply()

    // ─── Fortune trigger mode ("tap" / "shake") ───
    fun getFortuneTriggerMode() = p.getString("fortune_trigger", "tap") ?: "tap"
    fun setFortuneTriggerMode(m: String) = p.edit().putString("fortune_trigger", m).apply()

    // ─── Dice trigger mode ("tap" / "shake") ───
    fun getDiceTriggerMode() = p.getString("dice_trigger", "tap") ?: "tap"
    fun setDiceTriggerMode(m: String) = p.edit().putString("dice_trigger", m).apply()

    // ─── Dice weights (1-6, 权重制，默认全 1 即均等) ───
    fun getDiceWeights(): List<Int> = (1..6).map { p.getInt("dice_weight_$it", 1).coerceIn(0, 100) }
    fun setDiceWeight(i: Int, w: Int) = p.edit().putInt("dice_weight_${i + 1}", w.coerceIn(0, 100)).apply()

    // ─── Dice labels (1-6 自定义定义，如 1=打篮球) ───
    fun getDiceLabels(): List<String> = (1..6).map { p.getString("dice_label_$it", "") ?: "" }
    fun setDiceLabel(i: Int, label: String) = p.edit().putString("dice_label_${i + 1}", label.take(10)).apply()

    fun resetDiceSettings() {
        val e = p.edit()
        for (i in 1..6) { e.putInt("dice_weight_$i", 1); e.putString("dice_label_$i", "") }
        e.apply()
    }
}
