package com.example.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TimerPhase {
    STUDY,
    SHORT_BREAK,
    BIG_BREAK
}

data class TimerConfig(
    val id: Int = 1,
    val studyDurationMinutes: Int = 90,
    val breakDurationMinutes: Int = 15,
    val cycle1Count: Int = 4,
    val bigBreakDurationMinutes: Int = 60,
    val cycle2Count: Int = 4,
    val startHour: Int = 7,
    val startMinute: Int = 0,
    val autoStartBreak: Boolean = false,
    val autoStartStudy: Boolean = false
) {
    val studyDurationSeconds: Int get() = studyDurationMinutes * 60
    val breakDurationSeconds: Int get() = breakDurationMinutes * 60
    val bigBreakDurationSeconds: Int get() = bigBreakDurationMinutes * 60
}

data class TimerInterval(
    val phase: TimerPhase,
    val durationSeconds: Int,
    val label: String,
    val indexInType: Int, // e.g. 1 of 4
    val totalInType: Int
)

data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentIntervalIndex: Int = 0,
    val elapsedSeconds: Int = 0,
    val config: TimerConfig = TimerConfig()
) {
    val intervals: List<TimerInterval> = generateIntervals(config)
    
    val currentInterval: TimerInterval? = if (currentIntervalIndex in intervals.indices) {
        intervals[currentIntervalIndex]
    } else {
        null
    }
    
    val remainingSeconds: Int = currentInterval?.let {
        (it.durationSeconds - elapsedSeconds).coerceAtLeast(0)
    } ?: 0

    val progress: Float = currentInterval?.let {
        if (it.durationSeconds > 0) {
            elapsedSeconds.toFloat() / it.durationSeconds.toFloat()
        } else {
            0f
        }
    } ?: 0f
}

fun generateIntervals(config: TimerConfig): List<TimerInterval> {
    val list = mutableListOf<TimerInterval>()
    
    // Cycle 1
    for (i in 1..config.cycle1Count) {
        list.add(
            TimerInterval(
                phase = TimerPhase.STUDY,
                durationSeconds = config.studyDurationSeconds,
                label = "Study Session $i/${config.cycle1Count}",
                indexInType = i,
                totalInType = config.cycle1Count
            )
        )
        // Add short break only if it is in between study sessions (i.e., not after the last one)
        if (i < config.cycle1Count) {
            list.add(
                TimerInterval(
                    phase = TimerPhase.SHORT_BREAK,
                    durationSeconds = config.breakDurationSeconds,
                    label = "Short Break $i/${config.cycle1Count - 1}",
                    indexInType = i,
                    totalInType = config.cycle1Count - 1
                )
            )
        }
    }
    
    // Big Break
    if (config.bigBreakDurationSeconds > 0) {
        list.add(
            TimerInterval(
                phase = TimerPhase.BIG_BREAK,
                durationSeconds = config.bigBreakDurationSeconds,
                label = "Big Break",
                indexInType = 1,
                totalInType = 1
            )
        )
    }
    
    // Cycle 2
    for (i in 1..config.cycle2Count) {
        list.add(
            TimerInterval(
                phase = TimerPhase.STUDY,
                durationSeconds = config.studyDurationSeconds,
                label = "Study Session $i/${config.cycle2Count} (C2)",
                indexInType = i,
                totalInType = config.cycle2Count
            )
        )
        // Add short break only if it is in between study sessions (i.e., not after the last one)
        if (i < config.cycle2Count) {
            list.add(
                TimerInterval(
                    phase = TimerPhase.SHORT_BREAK,
                    durationSeconds = config.breakDurationSeconds,
                    label = "Short Break $i/${config.cycle2Count - 1} (C2)",
                    indexInType = i,
                    totalInType = config.cycle2Count - 1
                )
            )
        }
    }
    
    return list
}

data class TimelineItem(
    val label: String,
    val phase: TimerPhase,
    val durationText: String,
    val isCurrent: Boolean = false,
    val isPast: Boolean = false
)

fun calculateTimeline(config: TimerConfig, currentIntervalIndex: Int, isRunning: Boolean): List<TimelineItem> {
    val intervals = generateIntervals(config)
    
    return intervals.mapIndexed { index, interval ->
        val minutesToAdd = (interval.durationSeconds / 60).toLong()
        
        TimelineItem(
            label = interval.label,
            phase = interval.phase,
            durationText = "${minutesToAdd} min",
            isCurrent = isRunning && index == currentIntervalIndex,
            isPast = index < currentIntervalIndex
        )
    }
}
