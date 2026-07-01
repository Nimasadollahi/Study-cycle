package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.TimerPhase
import com.example.model.TimerState

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val RUNNING_CHANNEL_ID = "study_cycle_running_channel"
        const val ALERT_CHANNEL_ID = "study_cycle_alert_channel"
        const val NOTIFICATION_ID = 404
        
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ACTION_RESUME"
        const val ACTION_SKIP = "com.example.ACTION_SKIP"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for ongoing countdown timer (silent / low importance so it doesn't beep every second)
            val runningChannel = NotificationChannel(
                RUNNING_CHANNEL_ID,
                "Ongoing Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time countdown progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(runningChannel)

            // Channel for alarms/alerts (high priority with custom alarm sound)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Timer Completion Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you when study or break periods finish"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun buildTimerNotification(state: TimerState, serviceClass: Class<*>): Notification {
        val currentInterval = state.currentInterval
        val label = currentInterval?.label ?: "Ready to Focus"
        val remaining = state.remainingSeconds
        val hours = remaining / 3600
        val minutes = (remaining % 3600) / 60
        val seconds = remaining % 60
        
        val timeStr = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val phaseEmoji = when (currentInterval?.phase) {
            TimerPhase.STUDY -> "✍️"
            TimerPhase.SHORT_BREAK -> "☕"
            TimerPhase.BIG_BREAK -> "🎉"
            else -> "⏱️"
        }

        // Open App Intent
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Actions
        val pauseIntent = Intent(context, serviceClass).apply { action = ACTION_PAUSE }
        val resumeIntent = Intent(context, serviceClass).apply { action = ACTION_RESUME }
        val skipIntent = Intent(context, serviceClass).apply { action = ACTION_SKIP }

        val pausePendingIntent = PendingIntent.getService(
            context, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resumePendingIntent = PendingIntent.getService(
            context, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipPendingIntent = PendingIntent.getService(
            context, 3, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // Standard system play icon or a simple drawable
            .setContentTitle("$phaseEmoji $label")
            .setContentText(if (state.isPaused) "Paused • $timeStr left" else "Time Remaining: $timeStr")
            .setContentIntent(contentIntent)
            .setOngoing(state.isRunning && !state.isPaused)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Play/Pause Action
        if (state.isPaused) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        }

        // Skip Action
        builder.addAction(android.R.drawable.ic_media_next, "Skip", skipPendingIntent)

        // Set Progress Bar
        val total = currentInterval?.durationSeconds ?: 1
        builder.setProgress(total, state.elapsedSeconds, false)

        return builder.build()
    }

    fun sendCompletionNotification(title: String, message: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
