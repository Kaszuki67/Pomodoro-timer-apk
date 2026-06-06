package com.simplepomodoro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplepomodoro.domain.SessionType
import com.simplepomodoro.domain.TimerState
import com.simplepomodoro.service.TimerService

@Composable
fun TimerHeroCard(
    timerData: TimerService.TimerServiceData,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            SessionIndicator(completedSessions = timerData.completedSessions)

            CircularTimer(
                timeRemaining = timerData.timeRemaining,
                totalDuration = timerData.totalDuration,
                sessionType = timerData.sessionType
            )

            LinearProgressIndicator(
                progress = progressPercent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = sessionColor(timerData.sessionType),
                trackColor = sessionColor(timerData.sessionType).copy(alpha = 0.18f)
            )

            Text(
                text = "$progressPercent% completed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CircularTimer(
    timeRemaining: Int,
    totalDuration: Int,
    sessionType: SessionType,
    modifier: Modifier = Modifier
) {
    val rawProgress = if (totalDuration > 0) {
        timeRemaining.toFloat() / totalDuration.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)

    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 450),
        label = "timer-progress"
    )

    val ringColor = sessionColor(sessionType)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(260.dp)
    ) {
        Surface(
            shape = CircleShape,
            tonalElevation = 4.dp,
            color = ringColor.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxSize()
        ) {}

        Canvas(modifier = Modifier.size(240.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = ringColor.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        ringColor.copy(alpha = 0.65f),
                        ringColor,
                        ringColor.copy(alpha = 0.65f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SessionPill(sessionType)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatTime(timeRemaining),
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when (sessionType) {
                    SessionType.WORK -> "Deep focus"
                    SessionType.SHORT_BREAK -> "Quick recharge"
                    SessionType.LONG_BREAK -> "Long recovery"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionPill(sessionType: SessionType) {
    Surface(
        shape = CircleShape,
        color = sessionColor(sessionType).copy(alpha = 0.14f)
    ) {
        Text(
            text = sessionType.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = sessionColor(sessionType),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
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
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        Button(
            onClick = if (currentState is TimerState.Running) onPause else onStart,
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (currentState is TimerState.Running) "PAUSE" else "START FOCUS",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("RESET")
            }

            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("SKIP")
            }
        }
    }
}

@Composable
fun SessionIndicator(
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    val activeSession = completedSessions % 4

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        repeat(4) { index ->
            val isComplete = index < activeSession
            val isCurrent = index == activeSession

            Surface(
                shape = CircleShape,
                color = when {
                    isComplete -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(if (isCurrent) 16.dp else 12.dp)
            ) {}
        }
    }
}

@Composable
fun PresetButtons(
    selectedSessionType: SessionType,
    onSessionSelected: (SessionType) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Choose mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SessionType.values().forEach { type ->
                    FilterChip(
                        selected = selectedSessionType == type,
                        onClick = { onSessionSelected(type) },
                        label = {
                            Text(
                                text = when (type) {
                                    SessionType.WORK -> "Work"
                                    SessionType.SHORT_BREAK -> "Short"
                                    SessionType.LONG_BREAK -> "Long"
                                },
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = sessionColor(type).copy(alpha = 0.18f),
                            selectedLabelColor = sessionColor(type)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FocusHintCard(
    sessionType: SessionType,
    modifier: Modifier = Modifier
) {
    val hint = when (sessionType) {
        SessionType.WORK -> "Put your phone away, pick one task, and protect this focus block."
        SessionType.SHORT_BREAK -> "Stand up, drink water, stretch your neck, then come back fresh."
        SessionType.LONG_BREAK -> "Move around properly. Your brain earns the bigger reset after four focus rounds."
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = sessionColor(sessionType).copy(alpha = 0.10f),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(18.dp)
        )
    }
}

private fun sessionColor(sessionType: SessionType): Color {
    return when (sessionType) {
        SessionType.WORK -> Color(0xFFE85D75)
        SessionType.SHORT_BREAK -> Color(0xFF00A896)
        SessionType.LONG_BREAK -> Color(0xFF3A86FF)
    }
}

fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val mins = safeSeconds / 60
    val secs = safeSeconds % 60
    return "%02d:%02d".format(mins, secs)
}
