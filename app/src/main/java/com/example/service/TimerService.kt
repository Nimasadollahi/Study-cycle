package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.data.TimerDatabase
import com.example.data.TimerRepository
import com.example.model.TimerPhase
import com.example.model.TimerState
import com.example.util.NotificationHelper
import com.example.util.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var tickerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var repository: TimerRepository

    companion object {
        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ACTION_RESUME"
        const val ACTION_SKIP = "com.example.ACTION_SKIP"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        val database = TimerDatabase.getDatabase(this)
        repository = TimerRepository(database.timerDao())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_SKIP -> skipInterval()
            ACTION_STOP -> stopTimerService()
            // Notification action commands mapped directly
            NotificationHelper.ACTION_PAUSE -> pauseTimer()
            NotificationHelper.ACTION_RESUME -> resumeTimer()
            NotificationHelper.ACTION_SKIP -> skipInterval()
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        if (TimerManager.state.value.isRunning) return

        TimerManager.setRunning(true)
        TimerManager.setPaused(false)
        
        val initialNotification = notificationHelper.buildTimerNotification(TimerManager.state.value, TimerService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NotificationHelper.NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(
                        NotificationHelper.NOTIFICATION_ID,
                        initialNotification
                    )
                }
            } catch (e: Exception) {
                // Fallback to standard foreground if types are restricted
                startForeground(NotificationHelper.NOTIFICATION_ID, initialNotification)
            }
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (TimerManager.state.value.isRunning) {
                delay(1000)
                val state = TimerManager.state.value
                if (!state.isPaused) {
                    val currentInterval = state.currentInterval
                    if (currentInterval != null) {
                        val elapsed = state.elapsedSeconds + 1
                        if (elapsed >= currentInterval.durationSeconds) {
                            // Current Interval Finished!
                            val completedPhase = currentInterval.phase
                            val completedLabel = currentInterval.label
                            val durationMins = currentInterval.durationSeconds / 60
                            
                            // Log to Room database
                            repository.logSession(
                                phase = completedPhase.name,
                                label = completedLabel,
                                durationMinutes = durationMins,
                                completed = true
                            )

                            // Play sweet rolling bell chime
                            com.example.util.ToneSynthesizer.playCalmChime(isStudyEnd = (completedPhase == TimerPhase.STUDY))

                            // Send high-priority complete alert
                            val nextIndex = state.currentIntervalIndex + 1
                            val nextInterval = if (nextIndex < state.intervals.size) state.intervals[nextIndex] else null
                            
                            val alertTitle = when (completedPhase) {
                                TimerPhase.STUDY -> "✍️ Study Session Finished!"
                                TimerPhase.SHORT_BREAK -> "☕ Break Ended!"
                                TimerPhase.BIG_BREAK -> "🎉 Long Break Ended!"
                            }

                            val autoStart = nextInterval?.let {
                                when (it.phase) {
                                    TimerPhase.STUDY -> state.config.autoStartStudy
                                    TimerPhase.SHORT_BREAK, TimerPhase.BIG_BREAK -> state.config.autoStartBreak
                                }
                            } ?: false
                            
                            val alertMsg = if (nextInterval != null) {
                                if (autoStart) {
                                    "Moving automatically to: ${nextInterval.label}"
                                } else {
                                    "Tap to start: ${nextInterval.label}"
                                }
                            } else {
                                "Excellent job! You completed all cycles."
                            }
                            
                            notificationHelper.sendCompletionNotification(alertTitle, alertMsg)

                            // Transition state to next
                            if (nextInterval != null) {
                                TimerManager.updateState(
                                    state.copy(
                                        currentIntervalIndex = nextIndex,
                                        elapsedSeconds = 0,
                                        isPaused = !autoStart
                                    )
                                )
                            } else {
                                // Full cycle completed!
                                TimerManager.reset()
                                stopSelf()
                                break
                            }
                        } else {
                            TimerManager.updateElapsedSeconds(elapsed)
                        }
                        
                        // Update current notification
                        updateNotification()
                    } else {
                        // Finished
                        TimerManager.reset()
                        stopSelf()
                        break
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        TimerManager.setPaused(true)
        updateNotification()
    }

    private fun resumeTimer() {
        TimerManager.setPaused(false)
        updateNotification()
    }

    private fun skipInterval() {
        serviceScope.launch {
            val state = TimerManager.state.value
            val currentInterval = state.currentInterval
            if (currentInterval != null) {
                // Log partial/interrupted session
                repository.logSession(
                    phase = currentInterval.phase.name,
                    label = currentInterval.label,
                    durationMinutes = state.elapsedSeconds / 60,
                    completed = false
                )
            }

            val nextIndex = state.currentIntervalIndex + 1
            if (nextIndex < state.intervals.size) {
                TimerManager.updateState(
                    state.copy(
                        currentIntervalIndex = nextIndex,
                        elapsedSeconds = 0,
                        isPaused = false
                    )
                )
                updateNotification()
            } else {
                TimerManager.reset()
                stopSelf()
            }
        }
    }

    private fun updateNotification() {
        val notification = notificationHelper.buildTimerNotification(TimerManager.state.value, TimerService::class.java)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        mNotificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun stopTimerService() {
        TimerManager.reset()
        stopSelf()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }
}
