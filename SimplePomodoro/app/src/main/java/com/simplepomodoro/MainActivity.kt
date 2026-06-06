package com.simplepomodoro

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.simplepomodoro.data.Settings
import com.simplepomodoro.data.SettingsRepository
import com.simplepomodoro.domain.SessionType
import com.simplepomodoro.service.TimerService
import com.simplepomodoro.ui.SettingsScreen
import com.simplepomodoro.ui.TimerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private var isSettingsVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settingsFlow.collect { settings ->
                    applyScreenFlag(settings.keepScreenOn)
                    TimerService.setDurations(
                        settings.workDuration,
                        settings.shortBreakDuration,
                        settings.longBreakDuration
                    )
                    updateServiceSettings(settings)
                }
            }
        }

        setContent {
            MaterialTheme {
                val timerData by TimerService.timerData.collectAsState()
                val settings by settingsRepository.settingsFlow.collectAsState(initial = null)

                if (settings == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (isSettingsVisible) {
                    SettingsScreen(
                        settings = settings!!,
                        onWorkDurationChanged = { seconds ->
                            val updated = settings!!.copy(workDuration = seconds)
                            lifecycleScope.launch { settingsRepository.updateWorkDuration(seconds) }
                            TimerService.setDurations(updated.workDuration, updated.shortBreakDuration, updated.longBreakDuration)
                            updateServiceSettings(updated)
                        },
                        onShortBreakDurationChanged = { seconds ->
                            val updated = settings!!.copy(shortBreakDuration = seconds)
                            lifecycleScope.launch { settingsRepository.updateShortBreakDuration(seconds) }
                            TimerService.setDurations(updated.workDuration, updated.shortBreakDuration, updated.longBreakDuration)
                            updateServiceSettings(updated)
                        },
                        onLongBreakDurationChanged = { seconds ->
                            val updated = settings!!.copy(longBreakDuration = seconds)
                            lifecycleScope.launch { settingsRepository.updateLongBreakDuration(seconds) }
                            TimerService.setDurations(updated.workDuration, updated.shortBreakDuration, updated.longBreakDuration)
                            updateServiceSettings(updated)
                        },
                        onKeepScreenOnChanged = { enabled ->
                            lifecycleScope.launch { settingsRepository.updateKeepScreenOn(enabled) }
                            applyScreenFlag(enabled)
                        },
                        onNavigateBack = { isSettingsVisible = false }
                    )
                } else {
                    TimerScreen(
                        onStartTimer = { startTimer(settings!!) },
                        onPauseTimer = { pauseTimer(settings!!) },
                        onResetTimer = { resetTimer(settings!!) },
                        onSkipTimer = { skipTimer(settings!!) },
                        onNavigateToSettings = { isSettingsVisible = true },
                        onSessionSelected = { sessionType -> selectPreset(sessionType, settings!!) },
                        timerData = timerData,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun applyScreenFlag(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun updateServiceSettings(settings: Settings) {
        val intent = baseTimerIntent(settings).apply {
            action = TimerService.ACTION_UPDATE_SETTINGS
        }
        try {
            startService(intent)
        } catch (_: Exception) {
            // The in-memory companion state is already updated; ignore service start failures here.
        }
    }

    private fun selectPreset(sessionType: SessionType, settings: Settings) {
        val intent = baseTimerIntent(settings).apply {
            action = TimerService.ACTION_SELECT_SESSION
            putExtra(TimerService.EXTRA_SESSION_TYPE, sessionType.name)
        }
        startService(intent)
    }

    private fun startTimer(settings: Settings) {
        val intent = baseTimerIntent(settings).apply {
            action = TimerService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun pauseTimer(settings: Settings) {
        val intent = baseTimerIntent(settings).apply {
            action = TimerService.ACTION_PAUSE
        }
        startService(intent)
    }

    private fun resetTimer(settings: Settings) {
        val intent = baseTimerIntent(settings).apply {
            action = TimerService.ACTION_RESET
        }
        startService(intent)
    }

    private fun skipTimer(settings: Settings) {
        val intent = baseTimerIntent(settings).apply {
            action = TimerService.ACTION_SKIP
        }
        startService(intent)
    }

    private fun baseTimerIntent(settings: Settings): Intent {
        return Intent(this, TimerService::class.java).apply {
            putExtra(TimerService.EXTRA_WORK_DURATION, settings.workDuration)
            putExtra(TimerService.EXTRA_SHORT_BREAK_DURATION, settings.shortBreakDuration)
            putExtra(TimerService.EXTRA_LONG_BREAK_DURATION, settings.longBreakDuration)
        }
    }
}
