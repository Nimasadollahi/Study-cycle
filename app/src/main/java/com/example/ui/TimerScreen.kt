package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.TimerPhase
import com.example.model.TimerState
import com.example.model.calculateTimeline

// Sophisticated Dark Color Palette
val SophisticatedBg = Color(0xFF0A0A0A)
val SophisticatedCard = Color(0xFF161618)
val SophisticatedButtonBg = Color(0xFF1C1C1E)
val SophisticatedBorder = Color(0xFF252525)
val SophisticatedTextPrimary = Color(0xFFF0F0F0)
val SophisticatedTextSecondary = Color(0xFFA0A0A0)
val SophisticatedAccentBlue = Color(0xFF60A5FA)
val SophisticatedAccentOrange = Color(0xFFF59E0B)

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val timerState by viewModel.timerState.collectAsState()
    val historyList by viewModel.history.collectAsState()
    
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isStandbyActive by remember { mutableStateOf(false) }

    // Notification permission helper for target SDK 33+ (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startTimer()
        } else {
            Toast.makeText(context, "Notification permission is required to sound study and break transition alerts.", Toast.LENGTH_LONG).show()
        }
    }

    fun handleStartClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.startTimer()
            }
        } else {
            viewModel.startTimer()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SophisticatedBg)
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                LandscapeLayout(
                    state = timerState,
                    historyList = historyList,
                    viewModel = viewModel,
                    onStart = { handleStartClick() },
                    onStandbyClick = { isStandbyActive = true }
                )
            } else {
                PortraitLayout(
                    state = timerState,
                    historyList = historyList,
                    viewModel = viewModel,
                    onStart = { handleStartClick() },
                    onStandbyClick = { isStandbyActive = true }
                )
            }

            if (isStandbyActive && timerState.isRunning) {
                StandbyModeOverlay(
                    state = timerState,
                    onDismiss = { isStandbyActive = false }
                )
            }
        }
    }
}

@Composable
fun PortraitLayout(
    state: TimerState,
    historyList: List<com.example.data.SessionHistoryEntity>,
    viewModel: TimerViewModel,
    onStart: () -> Unit,
    onStandbyClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Timeline", "Cycles Config", "History Logs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sophisticated Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            val currentInterval = state.currentInterval
            val cycleText = if (state.isRunning && currentInterval != null) {
                "${currentInterval.phase.name} • Session ${currentInterval.indexInType} of ${currentInterval.totalInType}"
            } else {
                "Offline • Timer Standing By"
            }
            Text(
                text = cycleText.uppercase(),
                color = if (currentInterval?.phase == TimerPhase.STUDY) SophisticatedAccentBlue else SophisticatedAccentOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Study Cycle",
                color = SophisticatedTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp
            )
        }

        // Main Timer Card
        ActiveTimerCard(
            state = state,
            viewModel = viewModel,
            onStart = onStart,
            onStandbyClick = onStandbyClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .padding(bottom = 20.dp)
        )

        // Floating Action/Navigation Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SophisticatedCard,
            contentColor = SophisticatedTextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SophisticatedAccentBlue
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(48.dp),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SophisticatedCard)
                .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> TimelineSection(state)
                1 -> SettingsSection(viewModel, state.isRunning)
                2 -> HistorySection(historyList, onClear = { viewModel.clearHistory() })
            }
        }
    }
}

@Composable
fun LandscapeLayout(
    state: TimerState,
    historyList: List<com.example.data.SessionHistoryEntity>,
    viewModel: TimerViewModel,
    onStart: () -> Unit,
    onStandbyClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Timeline", "Settings", "History")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Timer details
        ActiveTimerCard(
            state = state,
            viewModel = viewModel,
            onStart = onStart,
            onStandbyClick = onStandbyClick,
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        )

        // Right Column: Configuration & Timeline Tabs
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SophisticatedCard,
                contentColor = SophisticatedTextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SophisticatedAccentBlue
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.height(40.dp),
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SophisticatedCard)
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> TimelineSection(state)
                    1 -> SettingsSection(viewModel, state.isRunning)
                    2 -> HistorySection(historyList, onClear = { viewModel.clearHistory() })
                }
            }
        }
    }
}

@Composable
fun ActiveTimerCard(
    state: TimerState,
    viewModel: TimerViewModel,
    onStart: () -> Unit,
    onStandbyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentInterval = state.currentInterval
    val label = currentInterval?.label ?: "Cycle Timer Ready"
    val isStudy = currentInterval?.phase == TimerPhase.STUDY

    // Animate color scheme based on study or break phase
    val accentColor by animateColorAsState(
        targetValue = if (!state.isRunning) {
            SophisticatedTextSecondary
        } else if (isStudy) {
            SophisticatedAccentBlue
        } else {
            SophisticatedAccentOrange
        },
        label = "AccentColor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, SophisticatedBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header information with Standby button
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label.uppercase(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.isRunning) {
                            if (state.isPaused) "PAUSED" else "ACTIVE"
                        } else {
                            "OFFLINE"
                        },
                        color = SophisticatedTextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (state.isRunning) {
                    IconButton(
                        onClick = onStandbyClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .background(SophisticatedButtonBg, CircleShape)
                            .border(1.dp, SophisticatedBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Standby Mode",
                            tint = SophisticatedAccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // High aesthetic digital thin readout with auto-scaling to fit box
            val remaining = state.remainingSeconds
            val hrs = remaining / 3600
            val mins = (remaining % 3600) / 60
            val secs = remaining % 60
            
            val countdownText = if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Determine text size based on length of text and screen width class
                val textLength = countdownText.length
                val baseFontSize = if (maxWidth > 350.dp) 72 else 54
                val adjustedFontSize = when {
                    textLength > 5 -> (baseFontSize * 0.72f).sp
                    else -> baseFontSize.sp
                }

                Text(
                    text = countdownText,
                    color = SophisticatedTextPrimary,
                    fontSize = adjustedFontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-2).sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("timer_countdown_text")
                )
            }

            // Dynamic progress bar (Sophisticated Dark custom glow and left-to-right fill)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                val elapsedMin = state.elapsedSeconds / 60
                val totalMin = (currentInterval?.durationSeconds ?: 0) / 60
                
                // Thin progress track
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress,
                    label = "TimerProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(SophisticatedBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (isStudy) {
                                        listOf(SophisticatedAccentBlue, SophisticatedAccentBlue.copy(alpha = 0.8f))
                                    } else {
                                        listOf(SophisticatedAccentOrange, SophisticatedAccentOrange.copy(alpha = 0.8f))
                                    }
                                )
                            )
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = accentColor,
                                spotColor = accentColor
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$elapsedMin min elapsed",
                        color = SophisticatedTextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$totalMin min total",
                        color = SophisticatedTextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.isRunning) {
                    // Modern OneUI 8.5 Pill start button
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = SophisticatedAccentBlue),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(54.dp)
                            .testTag("start_button"),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Session",
                            tint = SophisticatedBg,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START STUDY SESSION",
                            color = SophisticatedBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // Modern layout containing 3 stylish circular controls
                    IconButton(
                        onClick = { viewModel.stopTimer() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(SophisticatedButtonBg, CircleShape)
                            .border(1.dp, SophisticatedBorder, CircleShape)
                            .testTag("stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            tint = SophisticatedTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    val isPaused = state.isPaused
                    IconButton(
                        onClick = {
                            if (isPaused) viewModel.resumeTimer() else viewModel.pauseTimer()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(if (isPaused) SophisticatedAccentBlue else SophisticatedButtonBg, CircleShape)
                            .border(1.dp, if (isPaused) SophisticatedAccentBlue else SophisticatedBorder, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.HourglassEmpty,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = if (isPaused) SophisticatedBg else SophisticatedTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(
                        onClick = { viewModel.skipInterval() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(SophisticatedButtonBg, CircleShape)
                            .border(1.dp, SophisticatedBorder, CircleShape)
                            .testTag("skip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Interval",
                            tint = SophisticatedTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineSection(state: TimerState) {
    val timelineItems = calculateTimeline(state.config, state.currentIntervalIndex, state.isRunning)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAY TIMELINE",
                color = SophisticatedTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timelineItems) { item ->
                val cardBorderColor = if (item.isCurrent) {
                    if (item.phase == TimerPhase.STUDY) SophisticatedAccentBlue else SophisticatedAccentOrange
                } else {
                    Color.Transparent
                }

                val itemBg = if (item.isCurrent) {
                    SophisticatedButtonBg
                } else if (item.isPast) {
                    SophisticatedBg.copy(alpha = 0.5f)
                } else {
                    SophisticatedBg
                }

                val textOpacity = if (item.isPast) 0.4f else 1.0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(itemBg)
                        .border(1.dp, if (item.isCurrent) cardBorderColor else SophisticatedBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when (item.phase) {
                                        TimerPhase.STUDY -> SophisticatedAccentBlue
                                        TimerPhase.SHORT_BREAK -> SophisticatedAccentOrange
                                        TimerPhase.BIG_BREAK -> Color(0xFFC084FC) // Lavender
                                    }.copy(alpha = textOpacity),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = item.label,
                                color = SophisticatedTextPrimary.copy(alpha = textOpacity),
                                fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Text(
                                text = item.durationText,
                                color = SophisticatedTextSecondary.copy(alpha = textOpacity),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (item.isCurrent) {
                            Text(
                                text = "NOW RUNNING",
                                color = if (item.phase == TimerPhase.STUDY) SophisticatedAccentBlue else SophisticatedAccentOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(viewModel: TimerViewModel, isTimerRunning: Boolean) {
    val studyMins by viewModel.editStudyMinutes.collectAsState()
    val breakMins by viewModel.editBreakMinutes.collectAsState()
    val cycle1Count by viewModel.editCycle1Count.collectAsState()
    val bigBreakMins by viewModel.editBigBreakMinutes.collectAsState()
    val cycle2Count by viewModel.editCycle2Count.collectAsState()
    
    val autoStartBreak by viewModel.editAutoStartBreak.collectAsState()
    val autoStartStudy by viewModel.editAutoStartStudy.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERVAL TIMES (MINUTES)",
                    color = SophisticatedTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                if (isTimerRunning) {
                    Text(
                        text = "LOCKED",
                        color = SophisticatedAccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Row 1: Study Mins & Break Mins
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsTextField(
                    label = "Study (Min)",
                    value = studyMins,
                    onValueChange = {
                        viewModel.updateFormStudy(it)
                        viewModel.applyAndSaveSettings()
                    },
                    enabled = !isTimerRunning,
                    modifier = Modifier.weight(1f)
                )
                SettingsTextField(
                    label = "Break (Min)",
                    value = breakMins,
                    onValueChange = {
                        viewModel.updateFormBreak(it)
                        viewModel.applyAndSaveSettings()
                    },
                    enabled = !isTimerRunning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Standard Cycles & Big Break Mins
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsTextField(
                    label = "Cycle 1 Count",
                    value = cycle1Count,
                    onValueChange = {
                        viewModel.updateFormCycle1(it)
                        viewModel.applyAndSaveSettings()
                    },
                    enabled = !isTimerRunning,
                    modifier = Modifier.weight(1f)
                )
                SettingsTextField(
                    label = "Big Break (Min)",
                    value = bigBreakMins,
                    onValueChange = {
                        viewModel.updateFormBigBreak(it)
                        viewModel.applyAndSaveSettings()
                    },
                    enabled = !isTimerRunning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Secondary Cycles
        item {
            SettingsTextField(
                label = "Cycle 2 Count",
                value = cycle2Count,
                onValueChange = {
                    viewModel.updateFormCycle2(it)
                    viewModel.applyAndSaveSettings()
                },
                enabled = !isTimerRunning,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Divider
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AUTOMATION",
                color = SophisticatedTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Checkboxes Section
        item {
            SettingsCheckboxRow(
                label = "Auto-start break after study",
                checked = autoStartBreak,
                onCheckedChange = {
                    viewModel.updateFormAutoStartBreak(it)
                    viewModel.applyAndSaveSettings()
                },
                enabled = !isTimerRunning
            )
        }

        item {
            SettingsCheckboxRow(
                label = "Auto-start study after break",
                checked = autoStartStudy,
                onCheckedChange = {
                    viewModel.updateFormAutoStartStudy(it)
                    viewModel.applyAndSaveSettings()
                },
                enabled = !isTimerRunning
            )
        }
    }
}

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SophisticatedAccentBlue,
            unfocusedBorderColor = SophisticatedBorder,
            disabledBorderColor = SophisticatedBg,
            focusedTextColor = SophisticatedTextPrimary,
            unfocusedTextColor = SophisticatedTextPrimary,
            disabledTextColor = SophisticatedTextSecondary,
            focusedLabelColor = SophisticatedAccentBlue,
            unfocusedLabelColor = SophisticatedTextSecondary,
            disabledLabelColor = SophisticatedTextSecondary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}

@Composable
fun HistorySection(
    historyList: List<com.example.data.SessionHistoryEntity>,
    onClear: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Logs",
                    color = SophisticatedTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all history logs? This action cannot be undone.",
                    color = SophisticatedTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onClear()
                        showConfirmDialog = false
                    }
                ) {
                    Text("Delete All", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = SophisticatedTextSecondary)
                }
            },
            containerColor = SophisticatedCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SESSION HISTORY",
                color = SophisticatedTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            if (historyList.isNotEmpty()) {
                IconButton(onClick = { showConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History",
                        tint = Color(0xFFF87171)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (historyList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Empty History",
                    tint = SophisticatedBorder,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No logs recorded yet",
                    color = SophisticatedTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Complete focus study periods to record stats.",
                    color = SophisticatedTextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { item ->
                    val isStudy = item.phase == TimerPhase.STUDY.name
                    val isCompleted = item.completed
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SophisticatedBg)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isStudy) SophisticatedAccentBlue else SophisticatedAccentOrange,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.label,
                                    color = SophisticatedTextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                                val format = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                                Text(
                                    text = format.format(java.util.Date(item.timestamp)),
                                    color = SophisticatedTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${item.durationMinutes} min",
                                color = SophisticatedTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isCompleted) "COMPLETED" else "SKIPPED",
                                color = if (isCompleted) SophisticatedAccentBlue else Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) SophisticatedButtonBg else SophisticatedBg)
            .border(1.dp, if (checked) SophisticatedAccentBlue else SophisticatedBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) SophisticatedTextPrimary else SophisticatedTextSecondary.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
        
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) SophisticatedAccentBlue else SophisticatedCard)
                .border(
                    1.dp,
                    if (checked) SophisticatedAccentBlue else SophisticatedBorder,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SophisticatedBg,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun StandbyScreensaverMask(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "screensaver")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = 20.dp.toPx()
        val crossSize = 3.dp.toPx()
        
        var y = (animOffset % step) - step
        while (y < height + step) {
            var x = (animOffset % step) - step
            while (x < width + step) {
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = androidx.compose.ui.geometry.Offset(x - crossSize, y),
                    end = androidx.compose.ui.geometry.Offset(x + crossSize, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = androidx.compose.ui.geometry.Offset(x, y - crossSize),
                    end = androidx.compose.ui.geometry.Offset(x, y + crossSize),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            y += step
        }
    }
}

@Composable
fun StandbyModeOverlay(
    state: TimerState,
    onDismiss: () -> Unit
) {
    val remaining = state.remainingSeconds
    val hrs = remaining / 3600
    val mins = (remaining % 3600) / 60
    val secs = remaining % 60
    
    val countdownText = if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }

    val currentInterval = state.currentInterval
    val label = currentInterval?.label ?: "Timer"
    val isStudy = currentInterval?.phase == TimerPhase.STUDY

    val standbyThemeColor = if (isStudy) Color(0xFFEF4444) else Color(0xFF10B981)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
    ) {
        // Progressive background color fill from bottom to top
        val progress = state.progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress)
                .align(Alignment.BottomCenter)
                .background(standbyThemeColor.copy(alpha = 0.08f))
        )

        // Pixel-mask screensaver crosses grid
        StandbyScreensaverMask()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                color = standbyThemeColor.copy(alpha = 0.45f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = countdownText,
                color = standbyThemeColor.copy(alpha = 0.85f),
                fontSize = if (countdownText.length > 5) 80.sp else 110.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                letterSpacing = (-4).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (state.isPaused) "TAP TO RESUME" else "TAP TO EXIT STANDBY",
                color = standbyThemeColor.copy(alpha = 0.3f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
