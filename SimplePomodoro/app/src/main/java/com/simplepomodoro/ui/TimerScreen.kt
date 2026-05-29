package com.simplepomodoro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.simplepomodoro.service.TimerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSkipTimer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onWorkSelected: () -> Unit,
    onShortBreakSelected: () -> Unit,
    onLongBreakSelected: () -> Unit,
    timerData: TimerService.TimerServiceData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simple Pomodoro") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SessionIndicator(completedSessions = timerData.completedSessions)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CircularTimer(
                timeRemaining = timerData.timeRemaining,
                totalDuration = timerData.totalDuration,
                sessionType = timerData.sessionType
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TimerButtons(
                currentState = timerData.currentState,
                onStart = onStartTimer,
                onPause = onPauseTimer,
                onReset = onResetTimer,
                onSkip = onSkipTimer
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PresetButtons(
                onWorkSelected = onWorkSelected,
                onShortBreakSelected = onShortBreakSelected,
                onLongBreakSelected = onLongBreakSelected
            )
        }
    }
}
