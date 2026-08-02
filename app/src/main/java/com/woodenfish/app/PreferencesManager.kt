package com.woodenfish.app

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("woodenfish_prefs", Context.MODE_PRIVATE)

    // --- Daily count ---
    fun getTodayDate(): String = LocalDate.now().toString()

    fun getCount(): Int {
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        return if (savedDate == getTodayDate()) {
            prefs.getInt(KEY_COUNT, 0)
        } else {
            0
        }
    }

    fun incrementCount(): Int {
        val today = getTodayDate()
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        if (savedDate != today) {
            // New day — reset
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_COUNT, 1)
                .putBoolean(KEY_CELEBRATED_TODAY, false)
                .apply()
            return 1
        }
        val newCount = prefs.getInt(KEY_COUNT, 0) + 1
        prefs.edit().putInt(KEY_COUNT, newCount).apply()
        return newCount
    }

    // --- Celebration ---
    fun hasCelebratedToday(): Boolean {
        val savedDate = prefs.getString(KEY_LAST_DATE, "")
        if (savedDate != getTodayDate()) return false
        return prefs.getBoolean(KEY_CELEBRATED_TODAY, false)
    }

    fun markCelebratedToday() {
        prefs.edit().putBoolean(KEY_CELEBRATED_TODAY, true).apply()
    }

    // --- Total lifetime count ---
    fun getTotalCount(): Long = prefs.getLong(KEY_TOTAL_COUNT, 0L)

    fun incrementTotalCount() {
        prefs.edit().putLong(KEY_TOTAL_COUNT, getTotalCount() + 1).apply()
    }

    // --- Notification settings ---
    fun isNotificationEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFY_ENABLED, false)
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_ENABLED, enabled).apply()
    }

    fun getNotificationHour(): Int = prefs.getInt(KEY_NOTIFY_HOUR, 9)
    fun setNotificationHour(hour: Int) {
        prefs.edit().putInt(KEY_NOTIFY_HOUR, hour).apply()
    }

    fun getNotificationMinute(): Int = prefs.getInt(KEY_NOTIFY_MINUTE, 0)
    fun setNotificationMinute(minute: Int) {
        prefs.edit().putInt(KEY_NOTIFY_MINUTE, minute).apply()
    }

    fun getNotificationCount(): Int = prefs.getInt(KEY_NOTIFY_COUNT, 3)
    fun setNotificationCount(count: Int) {
        prefs.edit().putInt(KEY_NOTIFY_COUNT, count).apply()
    }

    fun isRandomTime(): Boolean = prefs.getBoolean(KEY_RANDOM_TIME, true)
    fun setRandomTime(random: Boolean) {
        prefs.edit().putBoolean(KEY_RANDOM_TIME, random).apply()
    }

    fun getNotificationStartHour(): Int = prefs.getInt(KEY_NOTIFY_START, 8)
    fun setNotificationStartHour(hour: Int) {
        prefs.edit().putInt(KEY_NOTIFY_START, hour).apply()
    }

    fun getNotificationEndHour(): Int = prefs.getInt(KEY_NOTIFY_END, 21)
    fun setNotificationEndHour(hour: Int) {
        prefs.edit().putInt(KEY_NOTIFY_END, hour).apply()
    }

    // --- First launch ---
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun markLaunched() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    companion object {
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_COUNT = "count"
        private const val KEY_TOTAL_COUNT = "total_count"
        private const val KEY_CELEBRATED_TODAY = "celebrated_today"
        private const val KEY_NOTIFY_ENABLED = "notify_enabled"
        private const val KEY_NOTIFY_HOUR = "notify_hour"
        private const val KEY_NOTIFY_MINUTE = "notify_minute"
        private const val KEY_NOTIFY_COUNT = "notify_count"
        private const val KEY_RANDOM_TIME = "random_time"
        private const val KEY_NOTIFY_START = "notify_start"
        private const val KEY_NOTIFY_END = "notify_end"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
