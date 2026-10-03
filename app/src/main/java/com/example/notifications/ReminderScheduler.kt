package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(taskId: Long, title: String, description: String?, triggerTime: Long) {
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_REMINDER_ALARM"
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_TITLE", title)
            putExtra("EXTRA_TASK_DESC", description ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled reminder for task $taskId at $triggerTime")
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "Security exception while scheduling reminder", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ignored: Exception) {}
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule reminder", e)
        }
    }

    fun cancelReminder(taskId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_REMINDER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("ReminderScheduler", "Cancelled reminder for task $taskId")
    }
}
