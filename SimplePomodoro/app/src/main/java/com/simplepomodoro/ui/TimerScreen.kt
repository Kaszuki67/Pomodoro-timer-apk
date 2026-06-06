package com.simplepomodoro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.simplepomodoro.domain.SessionType
import com.simplepomodoro.service.TimerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSkipTimer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSessionSelected: (SessionType) -> Unit,
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val progressPercent = if (timerData.totalDuration > 0) {
        ((timerData.totalDuration - timerData.timeRemaining) * 100 / timerData.totalDuration).coerceIn(0, 100)
    } else {
        0
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Simple Pomodoro", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Focus session ${timerData.completedSessions % 4 + 1} of 4",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
            modifier = modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            TimerHeroCard(
                timerData = timerData,
                progressPercent = progressPercent,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            TimerButtons(
                currentState = timerData.currentState,
                onStart = onStartTimer,
                onPause = onPauseTimer,
                onReset = onResetTimer,
                onSkip = onSkipTimer,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            PresetButtons(
                selectedSessionType = timerData.sessionType,
                onSessionSelected = onSessionSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            FocusHintCard(sessionType = timerData.sessionType)
        }
    }
}
