package com.simplepomodoro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplepomodoro.domain.SessionType
import com.simplepomodoro.domain.TimerState
import com.simplepomodoro.service.TimerService

@Composable
fun CircularTimer(
    timeRemaining: Int,
    totalDuration: Int,
    sessionType: SessionType,
    modifier: Modifier = Modifier
) {
    val progress = if (totalDuration > 0) {
        timeRemaining.toFloat() / totalDuration.toFloat()
    } else {
        0f
    }
    
    val ringColor = when (sessionType) {
        SessionType.WORK -> Color(0xFFFF6B6B)
        SessionType.SHORT_BREAK -> Color(0xFF4ECDC4)
        SessionType.LONG_BREAK -> Color(0xFF45B7D1)
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(280.dp)
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            
            // Background circle
            drawCircle(
                color = ringColor.copy(alpha = 0.2f),
                radius = diameter / 2,
                style = Stroke(width = strokeWidth)
            )
            
            // Progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = sessionType.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTime(timeRemaining),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TimerButtons(
    currentState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("RESET", fontSize = 14.sp)
        }
        
        if (currentState is TimerState.Running) {
            Button(
                onClick = onPause,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.weight(2f)
            ) {
                Text("PAUSE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(2f)
            ) {
                Text("START", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("SKIP", fontSize = 14.sp)
        }
    }
}

@Composable
fun SessionIndicator(
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = "Session ",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${(completedSessions % 4) + 1}/4",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun PresetButtons(
    onWorkSelected: () -> Unit,
    onShortBreakSelected: () -> Unit,
    onLongBreakSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        AssistChip(
            onClick = onWorkSelected,
            label = { Text("25:00 Work", fontSize = 12.sp) }
        )
        
        AssistChip(
            onClick = onShortBreakSelected,
            label = { Text("5:00 Short", fontSize = 12.sp) }
        )
        
        AssistChip(
            onClick = onLongBreakSelected,
            label = { Text("15:00 Long", fontSize = 12.sp) }
        )
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
