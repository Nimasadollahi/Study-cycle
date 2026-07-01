package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TimerDatabase
import com.example.data.TimerRepository
import com.example.model.TimerConfig
import com.example.model.TimerState
import com.example.service.TimerService
import com.example.util.TimerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TimerRepository

    private val _editStudyMinutes = MutableStateFlow("90")
    val editStudyMinutes = _editStudyMinutes.asStateFlow()

    private val _editBreakMinutes = MutableStateFlow("15")
    val editBreakMinutes = _editBreakMinutes.asStateFlow()

    private val _editCycle1Count = MutableStateFlow("4")
    val editCycle1Count = _editCycle1Count.asStateFlow()

    private val _editBigBreakMinutes = MutableStateFlow("60")
    val editBigBreakMinutes = _editBigBreakMinutes.asStateFlow()

    private val _editCycle2Count = MutableStateFlow("4")
    val editCycle2Count = _editCycle2Count.asStateFlow()

    private val _editStartHour = MutableStateFlow(7)
    val editStartHour = _editStartHour.asStateFlow()

    private val _editStartMinute = MutableStateFlow(0)
    val editStartMinute = _editStartMinute.asStateFlow()

    private val _editAutoStartBreak = MutableStateFlow(false)
    val editAutoStartBreak = _editAutoStartBreak.asStateFlow()

    private val _editAutoStartStudy = MutableStateFlow(false)
    val editAutoStartStudy = _editAutoStartStudy.asStateFlow()

    val timerState: StateFlow<TimerState> = TimerManager.state

    init {
        val database = TimerDatabase.getDatabase(application)
        repository = TimerRepository(database.timerDao())

        viewModelScope.launch {
            val savedConfig = repository.configFlow.first()
            TimerManager.updateConfig(savedConfig)
            
            _editStudyMinutes.value = savedConfig.studyDurationMinutes.toString()
            _editBreakMinutes.value = savedConfig.breakDurationMinutes.toString()
            _editCycle1Count.value = savedConfig.cycle1Count.toString()
            _editBigBreakMinutes.value = savedConfig.bigBreakDurationMinutes.toString()
            _editCycle2Count.value = savedConfig.cycle2Count.toString()
            _editStartHour.value = savedConfig.startHour
            _editStartMinute.value = savedConfig.startMinute
            _editAutoStartBreak.value = savedConfig.autoStartBreak
            _editAutoStartStudy.value = savedConfig.autoStartStudy
        }
    }

    val history = repository.historyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateFormStudy(value: String) { _editStudyMinutes.value = value }
    fun updateFormBreak(value: String) { _editBreakMinutes.value = value }
    fun updateFormCycle1(value: String) { _editCycle1Count.value = value }
    fun updateFormBigBreak(value: String) { _editBigBreakMinutes.value = value }
    fun updateFormCycle2(value: String) { _editCycle2Count.value = value }
    fun updateFormStartTime(hour: Int, minute: Int) {
        _editStartHour.value = hour
        _editStartMinute.value = minute
    }
    fun updateFormAutoStartBreak(value: Boolean) { _editAutoStartBreak.value = value }
    fun updateFormAutoStartStudy(value: Boolean) { _editAutoStartStudy.value = value }

    fun applyAndSaveSettings() {
        val study = _editStudyMinutes.value.toIntOrNull()?.coerceIn(1, 1440) ?: 90
        val brk = _editBreakMinutes.value.toIntOrNull()?.coerceIn(1, 1440) ?: 15
        val c1 = _editCycle1Count.value.toIntOrNull()?.coerceIn(1, 100) ?: 4
        val bbrk = _editBigBreakMinutes.value.toIntOrNull()?.coerceIn(0, 1440) ?: 60
        val c2 = _editCycle2Count.value.toIntOrNull()?.coerceIn(1, 100) ?: 4
        val sh = _editStartHour.value
        val sm = _editStartMinute.value
        val asb = _editAutoStartBreak.value
        val ass = _editAutoStartStudy.value

        val newConfig = TimerConfig(
            studyDurationMinutes = study,
            breakDurationMinutes = brk,
            cycle1Count = c1,
            bigBreakDurationMinutes = bbrk,
            cycle2Count = c2,
            startHour = sh,
            startMinute = sm,
            autoStartBreak = asb,
            autoStartStudy = ass
        )

        viewModelScope.launch {
            repository.saveConfig(newConfig)
            TimerManager.updateConfig(newConfig)
        }
    }

    fun startTimer() {
        applyAndSaveSettings()
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        getApplication<Application>().startService(intent)
    }

    fun pauseTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }

    fun skipInterval() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_SKIP
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
