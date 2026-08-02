package com.woodenfish.app

import android.content.Context
import android.content.SharedPreferences
import com.woodenfish.app.ui.theme.ThemeMode
import java.time.LocalDate

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("doki_prefs", Context.MODE_PRIVATE)

    private fun today() = LocalDate.now().toString()

    // --- Count ---
    fun getCount(): Int {
        val d = prefs.getString(KEY_LAST_DATE, "")
        return if (d == today()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    fun incrementCount(): Int {
        val t = today()
        if (prefs.getString(KEY_LAST_DATE, "") != t) {
            prefs.edit().putString(KEY_LAST_DATE, t).putInt(KEY_COUNT, 1)
                .putBoolean(KEY_CELEBRATED, false).apply()
            return 1
        }
        val c = prefs.getInt(KEY_COUNT, 0) + 1
        prefs.edit().putInt(KEY_COUNT, c).apply()
        return c
    }

    fun getTotalCount() = prefs.getLong(KEY_TOTAL_COUNT, 0L)
    fun incrementTotal() = prefs.edit().putLong(KEY_TOTAL_COUNT, getTotalCount() + 1).apply()

    fun hasCelebratedToday() = prefs.getString(KEY_LAST_DATE, "") == today()
            && prefs.getBoolean(KEY_CELEBRATED, false)
    fun markCelebrated() = prefs.edit().putBoolean(KEY_CELEBRATED, true).apply()

    // --- Agreement ---
    fun hasAgreedTerms() = prefs.getBoolean(KEY_AGREED_TERMS, false)
    fun setAgreedTerms() = prefs.edit().putBoolean(KEY_AGREED_TERMS, true).apply()

    // --- Notifications ---
    fun isNotificationEnabled() = prefs.getBoolean(KEY_NOTIFY_ENABLED, false)
    fun setNotificationEnabled(v: Boolean) = prefs.edit().putBoolean(KEY_NOTIFY_ENABLED, v).apply()

    fun getNotifyIntervalValue() = prefs.getInt(KEY_INTERVAL_VAL, 1)
    fun setNotifyIntervalValue(v: Int) = prefs.edit().putInt(KEY_INTERVAL_VAL, v).apply()

    fun getNotifyIntervalUnit() = prefs.getString(KEY_INTERVAL_UNIT, "小时") ?: "小时"
    fun setNotifyIntervalUnit(u: String) = prefs.edit().putString(KEY_INTERVAL_UNIT, u).apply()

    fun isRandomTime() = prefs.getBoolean(KEY_RANDOM_TIME, true)
    fun setRandomTime(v: Boolean) = prefs.edit().putBoolean(KEY_RANDOM_TIME, v).apply()

    fun getNotificationStartHour() = prefs.getInt(KEY_NOTIFY_START, 8)
    fun setNotificationStartHour(h: Int) = prefs.edit().putInt(KEY_NOTIFY_START, h).apply()

    fun getNotificationEndHour() = prefs.getInt(KEY_NOTIFY_END, 21)
    fun setNotificationEndHour(h: Int) = prefs.edit().putInt(KEY_NOTIFY_END, h).apply()

    fun isFirstLaunch() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun markLaunched() = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()

    // --- Theme ---
    fun getThemeMode(): ThemeMode = try {
        ThemeMode.valueOf(prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM")
    } catch (_: Exception) { ThemeMode.SYSTEM }
    fun setThemeMode(m: ThemeMode) = prefs.edit().putString(KEY_THEME, m.name).apply()

    // --- Language ---
    fun getLanguage() = prefs.getString(KEY_LANG, "zh-CN") ?: "zh-CN"
    fun setLanguage(l: String) = prefs.edit().putString(KEY_LANG, l).apply()

    companion object {
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_COUNT = "count"
        private const val KEY_TOTAL_COUNT = "total_count"
        private const val KEY_CELEBRATED = "celebrated_today"
        private const val KEY_AGREED_TERMS = "agreed_terms"
        private const val KEY_NOTIFY_ENABLED = "notify_enabled"
        private const val KEY_INTERVAL_VAL = "interval_val"
        private const val KEY_INTERVAL_UNIT = "interval_unit"
        private const val KEY_RANDOM_TIME = "random_time"
        private const val KEY_NOTIFY_START = "notify_start"
        private const val KEY_NOTIFY_END = "notify_end"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANG = "language"
    }
}
