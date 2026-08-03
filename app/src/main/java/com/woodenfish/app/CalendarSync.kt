package com.woodenfish.app

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import java.util.Calendar
import java.util.TimeZone

object CalendarSync {

    data class CalendarInfo(val id: Long, val name: String, val isGoogle: Boolean)

    /** 列出所有可写的日历（系统本地日历 / Google 日历等账号），用于让用户选择提醒日历 */
    fun listCalendars(context: Context): List<CalendarInfo> {
        val result = mutableListOf<CalendarInfo>()
        fun query(selection: String?) {
            try {
                val projection = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME)
                context.contentResolver.query(Calendars.CONTENT_URI, projection, selection, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val display = c.getString(1) ?: "日历"
                        val account = c.getString(2) ?: ""
                        val isGoogle = account.contains("google", true) || display.contains("google", true)
                        result.add(CalendarInfo(id, display, isGoogle))
                    }
                }
            } catch (_: Exception) {}
        }
        // 优先可写日历
        query("${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}")
        // 兜底：任意日历
        if (result.isEmpty()) query(null)
        return result
    }

    /** 写入每日固定时间提醒（每日重复事件 + 准时提醒），返回新事件ID；失败返回 null。写入前先删除旧事件。 */
    fun writeDailyReminder(context: Context, minute: Int, calendarId: Long?, oldEventId: Long?): Long? {
        return try {
            val cr = context.contentResolver
            if (oldEventId != null && oldEventId > 0) deleteEvent(cr, oldEventId)
            val calId = calendarId?.takeIf { it > 0 } ?: findWritableCalendar(cr) ?: return null
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minute / 60)
                set(Calendar.MINUTE, minute % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI)
                .withValue(Events.CALENDAR_ID, calId)
                .withValue(Events.TITLE, "Doki 敲木鱼")
                .withValue(Events.DESCRIPTION, "每日敲木鱼，积累功德")
                .withValue(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                .withValue(Events.DTSTART, start)
                .withValue(Events.DTEND, start + 60_000L)
                .withValue(Events.RRULE, "FREQ=DAILY")
                .build())
            ops.add(ContentProviderOperation.newInsert(Reminders.CONTENT_URI)
                .withValueBackReference(Reminders.EVENT_ID, 0)
                .withValue(Reminders.MINUTES, 0)
                .withValue(Reminders.METHOD, Reminders.METHOD_ALERT)
                .build())
            val results = cr.applyBatch(CalendarContract.AUTHORITY, ops)
            results.getOrNull(0)?.uri?.lastPathSegment?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun deleteEvent(context: Context, eventId: Long?) {
        if (eventId == null || eventId <= 0) return
        try { context.contentResolver.delete(Events.CONTENT_URI, "${Events._ID}=?", arrayOf(eventId.toString())) } catch (_: Exception) {}
    }

    private fun deleteEvent(cr: ContentResolver, eventId: Long) {
        try { cr.delete(Events.CONTENT_URI, "${Events._ID}=?", arrayOf(eventId.toString())) } catch (_: Exception) {}
    }

    /** 是否存在可写的日历（用于区分"没有可用日历"和"写入失败"） */
    fun hasWritableCalendar(context: Context): Boolean = findWritableCalendar(context.contentResolver) != null

    /** 找一个可写的日历（用户账号日历或本地日历） */
    private fun findWritableCalendar(cr: ContentResolver): Long? {
        val projection = arrayOf(Calendars._ID)
        // 优先：可写日历
        try {
            val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ${Calendars.CAL_ACCESS_CONTRIBUTOR}"
            cr.query(Calendars.CONTENT_URI, projection, selection, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
        } catch (_: Exception) {}
        // 兜底：任意日历
        try {
            cr.query(Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
        } catch (_: Exception) {}
        return null
    }
}
