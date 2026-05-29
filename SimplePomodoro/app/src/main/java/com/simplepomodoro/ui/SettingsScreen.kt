package com.simplepomodoro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var workMinutes by remember { mutableStateOf(settings.workDuration / 60) }
    var shortBreakMinutes by remember { mutableStateOf(settings.shortBreakDuration / 60) }
    var longBreakMinutes by remember { mutableStateOf(settings.longBreakDuration / 60) }
    var keepScreenOn by remember { mutableStateOf(settings.keepScreenOn) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Timer Durations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    DurationSelector(
                        label = "Work Duration",
                        minutes = workMinutes,
                        onMinutesChanged = { 
                            workMinutes = it
                            onWorkDurationChanged(it * 60)
                        }
                    )
                    
                    DurationSelector(
                        label = "Short Break",
                        minutes = shortBreakMinutes,
                        onMinutesChanged = {
                            shortBreakMinutes = it
                            onShortBreakDurationChanged(it * 60)
                        }
                    )
                    
                    DurationSelector(
                        label = "Long Break",
                        minutes = longBreakMinutes,
                        onMinutesChanged = {
                            longBreakMinutes = it
                            onLongBreakDurationChanged(it * 60)
                        }
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep Screen On",
                        fontSize = 16.sp
                    )
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = {
                            keepScreenOn = it
                            onKeepScreenOnChanged(it)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Changes are saved automatically",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DurationSelector(
    label: String,
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { if (minutes > 1) onMinutesChanged(minutes - 1) },
                label = { Text("-") }
            )
            
            Text(
                text = "$minutes min",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(60.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            AssistChip(
                onClick = { onMinutesChanged(minutes + 1) },
                label = { Text("+") }
            )
        }
    }
}
