package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val title = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Task Reminder"
        val description = intent.getStringExtra("EXTRA_TASK_DESC")

        if (taskId != -1L) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showReminderNotification(taskId, title, description)
        }
    }
}
