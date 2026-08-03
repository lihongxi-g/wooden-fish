package com.woodenfish.app

import android.content.Context
import android.content.SharedPreferences
import com.woodenfish.app.ui.theme.THEME_BROWN
import com.woodenfish.app.ui.theme.ThemeMode
import java.time.LocalDate

class PreferencesManager(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("doki_prefs", Context.MODE_PRIVATE)
    private fun t() = LocalDate.now().toString()

    fun getCount(): Int { val d = p.getString(K_DATE, ""); return if (d == t()) p.getInt(K_COUNT, 0) else 0 }
    fun incrementCount(): Int {
        val td = t()
        if (p.getString(K_DATE, "") != td) { p.edit().putString(K_DATE, td).putInt(K_COUNT, 1).putBoolean(K_CEL, false).apply(); return 1 }
        val c = p.getInt(K_COUNT, 0) + 1; p.edit().putInt(K_COUNT, c).apply(); return c
    }
    fun getTotalCount() = p.getLong(K_TOTAL, 0L)
    fun incrementTotal() = p.edit().putLong(K_TOTAL, getTotalCount() + 1).apply()
    fun hasCelebratedToday() = p.getString(K_DATE, "") == t() && p.getBoolean(K_CEL, false)
    fun markCelebrated() = p.edit().putBoolean(K_CEL, true).apply()
    fun hasAgreedTerms() = p.getBoolean(K_AGREED, false)
    fun setAgreedTerms() = p.edit().putBoolean(K_AGREED, true).apply()
    fun isNotificationEnabled() = p.getBoolean(K_NEN, false)
    fun setNotificationEnabled(v: Boolean) = p.edit().putBoolean(K_NEN, v).apply()
    fun getNotifyIntervalValue() = p.getInt(K_IVAL, 1)
    fun setNotifyIntervalValue(v: Int) = p.edit().putInt(K_IVAL, v).apply()
    fun getNotifyIntervalUnit() = p.getString(K_IUNIT, "小时") ?: "小时"
    fun setNotifyIntervalUnit(u: String) = p.edit().putString(K_IUNIT, u).apply()
    fun isRandomTime() = p.getBoolean(K_RAND, true)
    fun setRandomTime(v: Boolean) = p.edit().putBoolean(K_RAND, v).apply()
    fun getNotificationStartHour() = p.getInt(K_NS, 8)
    fun setNotificationStartHour(h: Int) = p.edit().putInt(K_NS, h).apply()
    fun getNotificationEndHour() = p.getInt(K_NE, 21)
    fun setNotificationEndHour(h: Int) = p.edit().putInt(K_NE, h).apply()
    fun isFirstLaunch() = p.getBoolean(K_FIRST, true)
    fun markLaunched() = p.edit().putBoolean(K_FIRST, false).apply()
    fun getThemeColorIndex() = p.getInt(K_TCI, THEME_BROWN)
    fun setThemeColorIndex(i: Int) = p.edit().putInt(K_TCI, i).apply()
    fun getThemeMode(): ThemeMode = try { ThemeMode.valueOf(p.getString(K_TM, "SYSTEM") ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
    fun setThemeMode(m: ThemeMode) = p.edit().putString(K_TM, m.name).apply()
    fun getLanguage() = p.getString(K_LANG, "zh-CN") ?: "zh-CN"
    fun setLanguage(l: String) = p.edit().putString(K_LANG, l).apply()
    fun getSoundVolume() = p.getFloat(K_SVOL, 0.7f)
    fun setSoundVolume(v: Float) = p.edit().putFloat(K_SVOL, v).apply()
    fun getVibrationIntensity() = p.getFloat(K_VINT, 0.8f)
    fun setVibrationIntensity(v: Float) = p.edit().putFloat(K_VINT, v).apply()

    companion object {
        private const val K_DATE = "ld"; private const val K_COUNT = "c"; private const val K_TOTAL = "tc"
        private const val K_CEL = "cel"; private const val K_AGREED = "ag"; private const val K_NEN = "ne"
        private const val K_IVAL = "iv"; private const val K_IUNIT = "iu"; private const val K_RAND = "rd"
        private const val K_NS = "ns"; private const val K_NE = "ne2"; private const val K_FIRST = "fl"
        private const val K_TCI = "tci"; private const val K_TM = "tm"; private const val K_LANG = "lg"
        private const val K_SVOL = "sv"; private const val K_VINT = "vi"
    }
}
