package com.simplepomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simplepomodoro.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onWorkDurationChanged: (Int) -> Unit,
    onShortBreakDurationChanged: (Int) -> Unit,
    onLongBreakDurationChanged: (Int) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workMinutes by remember(settings.workDuration) { mutableStateOf(settings.workDuration / 60) }
    var shortBreakMinutes by remember(settings.shortBreakDuration) { mutableStateOf(settings.shortBreakDuration / 60) }
    var longBreakMinutes by remember(settings.longBreakDuration) { mutableStateOf(settings.longBreakDuration / 60) }
    var keepScreenOn by remember(settings.keepScreenOn) { mutableStateOf(settings.keepScreenOn) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Settings", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Tune your focus rhythm",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Timer durations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    DurationSelector(
                        label = "Work",
                        helper = "Main focus block",
                        minutes = workMinutes,
                        onMinutesChanged = {
                            workMinutes = it
                            onWorkDurationChanged(it * 60)
                        }
                    )

                    Divider()

                    DurationSelector(
                        label = "Short break",
                        helper = "Quick reset between work blocks",
                        minutes = shortBreakMinutes,
                        onMinutesChanged = {
                            shortBreakMinutes = it
                            onShortBreakDurationChanged(it * 60)
                        }
                    )

                    Divider()

                    DurationSelector(
                        label = "Long break",
                        helper = "Recovery after four work blocks",
                        minutes = longBreakMinutes,
                        onMinutesChanged = {
                            longBreakMinutes = it
                            onLongBreakDurationChanged(it * 60)
                        }
                    )
                }
            }

            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep screen on",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Useful while studying from the timer screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = {
                            keepScreenOn = it
                            onKeepScreenOnChanged(it)
                        }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Changes are saved automatically. Keep work longer than breaks for a classic Pomodoro flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DurationSelector(
    label: String,
    helper: String,
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = { if (minutes > 1) onMinutesChanged(minutes - 1) },
                shape = CircleShape
            ) {
                Text("−", fontWeight = FontWeight.Bold)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$minutes min",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalIconButton(
                onClick = { if (minutes < 180) onMinutesChanged(minutes + 1) },
                shape = CircleShape
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
    }
}
