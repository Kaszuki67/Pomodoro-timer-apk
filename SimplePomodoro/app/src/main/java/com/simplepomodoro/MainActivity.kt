package com.simplepomodoro

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
                    if (settings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val timerData by TimerService.timerData.collectAsState()
                    val settings by settingsRepository.settingsFlow.collectAsState(initial = null)
                    
                    if (settings != null) {
                        if (isSettingsVisible) {
                            SettingsScreen(
                                settings = settings!!,
                                onWorkDurationChanged = { seconds ->
                                    launch {
                                        settingsRepository.updateWorkDuration(seconds)
                                        updateServiceSettings(settings!!)
                                    }
                                },
                                onShortBreakDurationChanged = { seconds ->
                                    launch {
                                        settingsRepository.updateShortBreakDuration(seconds)
                                        updateServiceSettings(settings!!)
                                    }
                                },
                                onLongBreakDurationChanged = { seconds ->
                                    launch {
                                        settingsRepository.updateLongBreakDuration(seconds)
                                        updateServiceSettings(settings!!)
                                    }
                                },
                                onKeepScreenOnChanged = { enabled ->
                                    launch { settingsRepository.updateKeepScreenOn(enabled) }
                                },
                                onNavigateBack = { isSettingsVisible = false }
                            )
                        } else {
                            TimerScreen(
                                onStartTimer = { startTimer() },
                                onPauseTimer = { pauseTimer() },
                                onResetTimer = { resetTimer() },
                                onSkipTimer = { skipTimer() },
                                onNavigateToSettings = { isSettingsVisible = true },
                                onWorkSelected = { selectPreset(SessionType.WORK, settings!!) },
                                onShortBreakSelected = { selectPreset(SessionType.SHORT_BREAK, settings!!) },
                                onLongBreakSelected = { selectPreset(SessionType.LONG_BREAK, settings!!) },
                                timerData = timerData,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun updateServiceSettings(settings: com.simplepomodoro.data.Settings) {
        TimerService.setDurations(settings.workDuration, settings.shortBreakDuration, settings.longBreakDuration)
        val intent = Intent(this, TimerService::class.java).apply {
            putExtra("work_duration", settings.workDuration)
            putExtra("short_break_duration", settings.shortBreakDuration)
            putExtra("long_break_duration", settings.longBreakDuration)
        }
        try {
            startService(intent)
        } catch (e: Exception) { }
    }
    
    private fun selectPreset(sessionType: SessionType, settings: com.simplepomodoro.data.Settings) {
        val duration = when (sessionType) {
            SessionType.WORK -> settings.workDuration
            SessionType.SHORT_BREAK -> settings.shortBreakDuration
            SessionType.LONG_BREAK -> settings.longBreakDuration
        }
        
        // Reset timer with selected preset
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        startService(intent)
    }
    
    private fun startTimer() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        startForegroundService(intent)
    }
    
    private fun pauseTimer() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        startService(intent)
    }
    
    private fun resetTimer() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        startService(intent)
    }
    
    private fun skipTimer() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_SKIP
        }
        startService(intent)
    }
}
