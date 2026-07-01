package com.example.util

import com.example.model.TimerConfig
import com.example.model.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerManager {
    private val _state = MutableStateFlow(TimerState())
    val state = _state.asStateFlow()

    fun updateState(newState: TimerState) {
        _state.value = newState
    }

    fun updateConfig(config: TimerConfig) {
        val currentState = _state.value
        _state.value = currentState.copy(
            config = config,
            currentIntervalIndex = 0,
            elapsedSeconds = 0,
            isRunning = false,
            isPaused = false
        )
    }

    fun updateIntervalIndex(index: Int) {
        _state.value = _state.value.copy(
            currentIntervalIndex = index,
            elapsedSeconds = 0
        )
    }

    fun updateElapsedSeconds(seconds: Int) {
        _state.value = _state.value.copy(
            elapsedSeconds = seconds
        )
    }

    fun setRunning(running: Boolean) {
        _state.value = _state.value.copy(
            isRunning = running
        )
    }

    fun setPaused(paused: Boolean) {
        _state.value = _state.value.copy(
            isPaused = paused
        )
    }

    fun reset() {
        val config = _state.value.config
        _state.value = TimerState(config = config)
    }
}
