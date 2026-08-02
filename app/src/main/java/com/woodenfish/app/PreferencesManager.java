package com.woodenfish.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

public class PreferencesManager {
    private static final String PREFS_NAME = "doki_prefs";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_COUNT = "count";
    private static final String KEY_TOTAL_COUNT = "total_count";
    private static final String KEY_CELEBRATED = "celebrated_today";
    private static final String KEY_NOTIFY_ENABLED = "notify_enabled";
    private static final String KEY_NOTIFY_HOUR = "notify_hour";
    private static final String KEY_NOTIFY_MINUTE = "notify_minute";
    private static final String KEY_NOTIFY_COUNT = "notify_count";
    private static final String KEY_RANDOM_TIME = "random_time";
    private static final String KEY_NOTIFY_START = "notify_start";
    private static final String KEY_NOTIFY_END = "notify_end";
    private static final String KEY_AGREED_TERMS = "agreed_terms";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String today() { return LocalDate.now().toString(); }

    public int getCount() {
        String saved = prefs.getString(KEY_LAST_DATE, "");
        return saved.equals(today()) ? prefs.getInt(KEY_COUNT, 0) : 0;
    }

    public int incrementCount() {
        String t = today();
        String saved = prefs.getString(KEY_LAST_DATE, "");
        if (!saved.equals(t)) {
            prefs.edit().putString(KEY_LAST_DATE, t).putInt(KEY_COUNT, 1)
                    .putBoolean(KEY_CELEBRATED, false).apply();
            return 1;
        }
        int c = prefs.getInt(KEY_COUNT, 0) + 1;
        prefs.edit().putInt(KEY_COUNT, c).apply();
        return c;
    }

    public long getTotalCount() { return prefs.getLong(KEY_TOTAL_COUNT, 0L); }
    public void incrementTotal() {
        prefs.edit().putLong(KEY_TOTAL_COUNT, getTotalCount() + 1).apply();
    }

    public boolean hasCelebratedToday() {
        return prefs.getString(KEY_LAST_DATE, "").equals(today())
                && prefs.getBoolean(KEY_CELEBRATED, false);
    }

    public void markCelebrated() {
        prefs.edit().putBoolean(KEY_CELEBRATED, true).apply();
    }

    public boolean hasAgreedTerms() { return prefs.getBoolean(KEY_AGREED_TERMS, false); }
    public void setAgreedTerms() { prefs.edit().putBoolean(KEY_AGREED_TERMS, true).apply(); }

    public boolean isNotifyEnabled() { return prefs.getBoolean(KEY_NOTIFY_ENABLED, false); }
    public void setNotifyEnabled(boolean v) { prefs.edit().putBoolean(KEY_NOTIFY_ENABLED, v).apply(); }

    public int getNotifyHour() { return prefs.getInt(KEY_NOTIFY_HOUR, 9); }
    public void setNotifyHour(int h) { prefs.edit().putInt(KEY_NOTIFY_HOUR, h).apply(); }

    public int getNotifyMinute() { return prefs.getInt(KEY_NOTIFY_MINUTE, 0); }
    public void setNotifyMinute(int m) { prefs.edit().putInt(KEY_NOTIFY_MINUTE, m).apply(); }

    public int getNotifyCount() { return prefs.getInt(KEY_NOTIFY_COUNT, 3); }
    public void setNotifyCount(int c) { prefs.edit().putInt(KEY_NOTIFY_COUNT, c).apply(); }

    public boolean isRandomTime() { return prefs.getBoolean(KEY_RANDOM_TIME, true); }
    public void setRandomTime(boolean v) { prefs.edit().putBoolean(KEY_RANDOM_TIME, v).apply(); }

    public int getNotifyStart() { return prefs.getInt(KEY_NOTIFY_START, 8); }
    public void setNotifyStart(int h) { prefs.edit().putInt(KEY_NOTIFY_START, h).apply(); }

    public int getNotifyEnd() { return prefs.getInt(KEY_NOTIFY_END, 21); }
    public void setNotifyEnd(int h) { prefs.edit().putInt(KEY_NOTIFY_END, h).apply(); }
}
