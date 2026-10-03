package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.TaskFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        if (taskId == -1L) return

        val notificationHelper = NotificationHelper(context)
        notificationHelper.cancelNotification(taskId)

        when (intent.action) {
            "com.example.ACTION_NOTIFICATION_COMPLETE" -> {
                val app = context.applicationContext as? TaskFlowApplication
                if (app != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        app.taskRepository.setTaskCompleted(taskId, true)
                    }
                }
            }
            "com.example.ACTION_NOTIFICATION_SNOOZE" -> {
                val title = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Task Reminder"
                val desc = intent.getStringExtra("EXTRA_TASK_DESC")
                val snoozeTime = System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins
                val reminderScheduler = ReminderScheduler(context)
                reminderScheduler.scheduleReminder(taskId, title, desc, snoozeTime)
            }
        }
    }
}
