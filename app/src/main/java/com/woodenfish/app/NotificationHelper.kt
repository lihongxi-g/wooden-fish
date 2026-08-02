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
        const val CHANNEL_ID = "woodenfish_reminder"
        const val CHANNEL_NAME = "木鱼提醒"
        const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "敲木鱼提醒通知"
                enableVibration(true)
                setSound(null, null) // silent channel, we vibrate only
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendReminder() {
        if (!hasPermission()) return

        createChannel(context)

        val prefs = PreferencesManager(context)
        val todayCount = prefs.getCount()

        val messages = listOf(
            "阿弥陀佛 🙏 该敲木鱼了",
            "心静自然凉，敲几下木鱼吧",
            "忙里偷闲，来敲一下？",
            "今天才敲了 ${todayCount} 下，再接再厉",
            "木鱼等了你好久了 🪵",
            "放下手机，敲敲木鱼",
            "功德 +1 的机会来了",
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("木鱼")
            .setContentText(messages.random())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun scheduleNotifications(prefs: PreferencesManager) {
        if (!prefs.isNotificationEnabled()) {
            cancelAll()
            return
        }

        cancelAll()

        val count = prefs.getNotificationCount()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val useRandom = prefs.isRandomTime()

        // If fixed time
        if (!useRandom) {
            val triggerTime = calculateNextTrigger(
                prefs.getNotificationHour(),
                prefs.getNotificationMinute()
            )
            scheduleAlarm(alarmManager, triggerTime, 0)
            return
        }

        // Random times within [startHour, endHour]
        val startHour = prefs.getNotificationStartHour()
        val endHour = prefs.getNotificationEndHour()

        if (startHour >= endHour) return

        val hoursRange = endHour - startHour
        if (hoursRange <= 0) return

        // Distribute count notifications across the range
        val segmentSize = (hoursRange * 60) / count
        val now = System.currentTimeMillis()

        for (i in 0 until count) {
            val baseMinutes = startHour * 60 + i * segmentSize
            val randomOffset = Random.nextInt(0, segmentSize.coerceAtLeast(1))
            val totalMinutes = baseMinutes + randomOffset

            val triggerTime = calculateNextTrigger(totalMinutes / 60, totalMinutes % 60)

            if (triggerTime > now) {
                scheduleAlarm(alarmManager, triggerTime, i)
            }
        }
    }

    private fun calculateNextTrigger(hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        // If time already passed today, schedule for tomorrow
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerTime: Long, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback: use inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            )
        }
    }

    fun cancelAll() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0..9) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleBootReceiver() {
        // This is called after reboot to re-schedule notifications
        val prefs = PreferencesManager(context)
        if (prefs.isNotificationEnabled()) {
            scheduleNotifications(prefs)
        }
    }
}

/**
 * BroadcastReceiver that fires when an alarm goes off.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper(context).sendReminder()
        // Re-schedule for next day
        NotificationHelper(context).scheduleNotifications(PreferencesManager(context))
    }
}

/**
 * Boot receiver to re-schedule alarms after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper(context).scheduleBootReceiver()
        }
    }
}
