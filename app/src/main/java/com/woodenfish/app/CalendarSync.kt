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

    /** 写入每日固定时间提醒（每日重复事件 + 准时提醒），返回新事件ID；失败返回 null。写入前先删除旧事件。 */
    fun writeDailyReminder(context: Context, minute: Int, oldEventId: Long?): Long? {
        return try {
            val cr = context.contentResolver
            if (oldEventId != null && oldEventId > 0) deleteEvent(cr, oldEventId)
            val calId = findWritableCalendar(cr) ?: return null
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
