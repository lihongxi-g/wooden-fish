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
    fun incrementCount(): Int {
        val td = t()
        if (p.getString("last_date", "") != td) { p.edit().putString("last_date", td).putInt("count", 1).putBoolean("celebrated_today", false).apply(); return 1 }
        val c = p.getInt("count", 0) + 1; p.edit().putInt("count", c).apply(); return c
    }
    fun getTotalCount() = p.getLong("total_count", 0L)
    fun incrementTotal() = p.edit().putLong("total_count", getTotalCount() + 1).apply()
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

    // ─── First launch ───
    fun isFirstLaunch() = p.getBoolean("first_launch", true)
    fun markLaunched() = p.edit().putBoolean("first_launch", false).apply()

    // ─── Theme ───
    fun getThemeColorIndex() = p.getInt("theme_color", THEME_BROWN)
    fun setThemeColorIndex(i: Int) = p.edit().putInt("theme_color", i).apply()
    fun getThemeMode(): ThemeMode = try { ThemeMode.valueOf(p.getString("theme_mode", "SYSTEM") ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
    fun setThemeMode(m: ThemeMode) = p.edit().putString("theme_mode", m.name).apply()

    // ─── Language ───
    fun getLanguage() = p.getString("language", "zh-CN") ?: "zh-CN"
    fun setLanguage(l: String) = p.edit().putString("language", l).apply()

    // ─── Sound & Vibration ───
    fun getSoundVolume() = p.getFloat("sound_vol", 0.7f)
    fun setSoundVolume(v: Float) = p.edit().putFloat("sound_vol", v).apply()
    fun getVibrationIntensity() = p.getFloat("vib_intensity", 0.8f)
    fun setVibrationIntensity(v: Float) = p.edit().putFloat("vib_intensity", v).apply()
}
