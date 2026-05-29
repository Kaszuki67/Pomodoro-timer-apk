package com.simplepomodoro.domain

sealed class TimerState {
    object Idle : TimerState()
    object Running : TimerState()
    object Paused : TimerState()
}

enum class SessionType(val displayName: String) {
    WORK("Work"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class TimerData(
    val currentState: TimerState = TimerState.Idle,
    val sessionType: SessionType = SessionType.WORK,
    val timeRemaining: Int = 25 * 60,
    val totalDuration: Int = 25 * 60,
    val completedSessions: Int = 0,
    val sessionsUntilLongBreak: Int = 4
)
