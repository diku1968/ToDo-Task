package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "taskflow_reminders_channel"
        const val CHANNEL_NAME = "Task Reminders"
        const val CHANNEL_DESC = "Notifications for scheduled task reminders and due dates"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        taskId: Long,
        title: String,
        description: String?
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Complete
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_NOTIFICATION_COMPLETE"
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 15 minutes
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_NOTIFICATION_SNOOZE"
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_TITLE", title)
            putExtra("EXTRA_TASK_DESC", description ?: "")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(if (!description.isNullOrBlank()) description else "Task due now")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(tapPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 15m", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), builder.build())
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun cancelNotification(taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}
