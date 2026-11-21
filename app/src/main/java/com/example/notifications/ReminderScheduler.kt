package com.example.notifications

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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object ReminderChannels {
    const val CHANNEL_ID = "satyen_reminders"
    const val EXTRA_TITLE = "reminder_title"
    const val EXTRA_NOTES = "reminder_notes"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Satyen Meta AI Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Scheduled personal reminders and alerts"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderChannels.ensureChannel(context)
        if (!ReminderChannels.hasNotificationPermission(context)) {
            Log.w("ReminderReceiver", "Notification skipped: POST_NOTIFICATIONS permission not granted.")
            return
        }
        val title = intent.getStringExtra(ReminderChannels.EXTRA_TITLE) ?: "Satyen Meta AI"
        val notes = intent.getStringExtra(ReminderChannels.EXTRA_NOTES).orEmpty()
        val notificationId = intent.getIntExtra(ReminderChannels.EXTRA_NOTIFICATION_ID, 1001)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderChannels.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { "Tap to open Satyen Meta AI" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }
}

class ReminderScheduler(private val context: Context) {
    fun scheduleReminder(reminderId: String, title: String, notes: String?, triggerAtMillis: Long): String {
        ReminderChannels.ensureChannel(context)
        if (!ReminderChannels.hasNotificationPermission(context)) {
            return "PERMISSION_DENIED"
        }
        val now = System.currentTimeMillis()
        if (triggerAtMillis <= now) {
            return "SCHEDULED"
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return "SKIPPED"
        val requestCode = reminderId.hashCode()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderChannels.EXTRA_TITLE, title)
            putExtra(ReminderChannels.EXTRA_NOTES, notes ?: "")
            putExtra(ReminderChannels.EXTRA_NOTIFICATION_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            "SCHEDULED"
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm", e)
            "SKIPPED"
        }
    }

    fun cancelReminder(reminderId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
