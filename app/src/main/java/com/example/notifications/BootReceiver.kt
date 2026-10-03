package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.TaskFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? TaskFlowApplication ?: return
            val reminderScheduler = ReminderScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()
                val activeTasks = app.database.taskDao().getActiveReminders(now)
                for (task in activeTasks) {
                    val reminderTime = task.reminderTime ?: continue
                    reminderScheduler.scheduleReminder(
                        taskId = task.id,
                        title = task.title,
                        description = task.description,
                        triggerTime = reminderTime
                    )
                }
            }
        }
    }
}
