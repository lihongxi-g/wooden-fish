package com.woodenfish.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "doki_reminder"
        const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "木鱼提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "敲木鱼提醒"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun sendReminder() {
        if (!hasPermission()) return
        val prefs = PreferencesManager(context)
        val todayCount = prefs.getCount()
        val messages = listOf("阿弥陀佛 🙏 该敲木鱼了", "心静自然凉，敲几下木鱼吧", "忙里偷闲，来敲一下？", "今天才敲了 $todayCount 下，再接再厉", "木鱼等了你好久了 🪵", "放下手机，敲敲木鱼")
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Doki")
            .setContentText(messages.random())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }

    fun scheduleNotifications(prefs: PreferencesManager) {
        cancelAll()
        if (!prefs.isNotificationEnabled()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (prefs.isRandomTime()) {
            // Random times within range
            val startMin = prefs.getNotificationStartMin()
            val endMin = prefs.getNotificationEndMin()
            if (startMin >= endMin) return
            val count = 3
            val segment = (endMin - startMin) / count
            val now = System.currentTimeMillis()
            for (i in 0 until count) {
                val mins = startMin + i * segment + Random.nextInt(0, segment.coerceAtLeast(1))
                val trigger = calcTrigger(mins / 60, mins % 60)
                if (trigger > now) scheduleAlarm(alarmManager, trigger, i)
            }
        } else {
            // Interval-based: schedule next alarm
            val value = prefs.getNotifyIntervalValue()
            val unit = prefs.getNotifyIntervalUnit()
            val intervalMs = when {
                unit.contains("分钟") || unit.contains("min") -> value * 60_000L
                unit.contains("小时") || unit.contains("hr") -> value * 3_600_000L
                else -> value * 86_400_000L
            }
            val trigger = System.currentTimeMillis() + intervalMs
            scheduleAlarm(alarmManager, trigger, 0)
        }
    }

    private fun calcTrigger(hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun scheduleAlarm(am: AlarmManager, trigger: Long, code: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // 5分钟窗口非精确闹钟：无需 SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM 特殊权限，
        // 安装时不提示"闹钟和提醒"，系统到点自动拉起，App 无需后台常驻
        am.setWindow(AlarmManager.RTC_WAKEUP, trigger, 5 * 60_000L, pi)
    }

    fun cancelAll() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0..9) {
            val pi = PendingIntent.getBroadcast(context, i, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pi)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper(context).sendReminder()
        NotificationHelper(context).scheduleNotifications(PreferencesManager(context))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper(context).scheduleNotifications(PreferencesManager(context))
        }
    }
}
