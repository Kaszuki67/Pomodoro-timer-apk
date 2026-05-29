package com.simplepomodoro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.simplepomodoro.MainActivity
import com.simplepomodoro.R
import com.simplepomodoro.domain.SessionType
import com.simplepomodoro.domain.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.simplepomodoro.START"
        const val ACTION_PAUSE = "com.simplepomodoro.PAUSE"
        const val ACTION_RESET = "com.simplepomodoro.RESET"
        const val ACTION_SKIP = "com.simplepomodoro.SKIP"
        
        private var _workDuration = 25 * 60
        private var _shortBreakDuration = 5 * 60
        private var _longBreakDuration = 15 * 60
        
        fun setDurations(work: Int, shortBreak: Int, longBreak: Int) {
            _workDuration = work
            _shortBreakDuration = shortBreak
            _longBreakDuration = longBreak
        }
        
        private val _timerData = MutableStateFlow(
            TimerServiceData(
                currentState = TimerState.Idle,
                sessionType = SessionType.WORK,
                timeRemaining = 25 * 60,
                totalDuration = 25 * 60,
                completedSessions = 0
            )
        )
        val timerData: StateFlow<TimerServiceData> = _timerData.asStateFlow()
    }

    private var countDownTimer: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var vibrator: Vibrator
    private var workDuration = 25 * 60
    private var shortBreakDuration = 5 * 60
    private var longBreakDuration = 15 * 60

    data class TimerServiceData(
        val currentState: TimerState = TimerState.Idle,
        val sessionType: SessionType = SessionType.WORK,
        val timeRemaining: Int = 25 * 60,
        val totalDuration: Int = 25 * 60,
        val completedSessions: Int = 0
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimplePomodoro::TimerWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
            ACTION_SKIP -> skipSession()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent?) = null

    fun updateSettings(workDuration: Int, shortBreakDuration: Int, longBreakDuration: Int) {
        this.workDuration = workDuration
        this.shortBreakDuration = shortBreakDuration
        this.longBreakDuration = longBreakDuration
        Companion.setDurations(workDuration, shortBreakDuration, longBreakDuration)
        
        if (_timerData.value.currentState == TimerState.Idle || _timerData.value.currentState == TimerState.Paused) {
            val newDuration = getDurationForSession(_timerData.value.sessionType)
            _timerData.value = _timerData.value.copy(
                totalDuration = newDuration,
                timeRemaining = newDuration
            )
        }
    }

    private fun startTimer() {
        if (_timerData.value.currentState == TimerState.Running) return
        _timerData.value = _timerData.value.copy(currentState = TimerState.Running)
        startForeground(NOTIFICATION_ID, createNotification())
        
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(
            _timerData.value.timeRemaining.toLong() * 1000,
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                _timerData.value = _timerData.value.copy(timeRemaining = secondsRemaining)
                updateNotification()
            }

            override fun onFinish() {
                onTimerComplete()
            }
        }.start()
        wakeLock?.acquire(10*60*1000L)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        _timerData.value = _timerData.value.copy(currentState = TimerState.Paused)
        wakeLock?.release()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        val duration = getDurationForSession(_timerData.value.sessionType)
        _timerData.value = _timerData.value.copy(
            currentState = TimerState.Idle,
            timeRemaining = duration,
            totalDuration = duration
        )
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun skipSession() {
        countDownTimer?.cancel()
        moveToNextSession()
    }

    private fun onTimerComplete() {
        playNotificationSound()
        vibrate()
        
        val currentCompleted = _timerData.value.completedSessions
        val newCompleted = if (_timerData.value.sessionType == SessionType.WORK) {
            currentCompleted + 1
        } else {
            currentCompleted
        }
        _timerData.value = _timerData.value.copy(completedSessions = newCompleted)
        moveToNextSession()
    }

    private fun moveToNextSession() {
        val currentType = _timerData.value.sessionType
        val completed = _timerData.value.completedSessions
        
        val nextType = when {
            currentType == SessionType.WORK && completed % 4 == 0 -> SessionType.LONG_BREAK
            currentType == SessionType.WORK -> SessionType.SHORT_BREAK
            else -> SessionType.WORK
        }
        
        val nextDuration = getDurationForSession(nextType)
        _timerData.value = _timerData.value.copy(
            sessionType = nextType,
            timeRemaining = nextDuration,
            totalDuration = nextDuration,
            currentState = TimerState.Running
        )
        startTimer()
    }

    private fun getDurationForSession(type: SessionType): Int {
        return when (type) {
            SessionType.WORK -> workDuration
            SessionType.SHORT_BREAK -> shortBreakDuration
            SessionType.LONG_BREAK -> longBreakDuration
        }
    }

    private fun createNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val startPendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).setAction(ACTION_START),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pausePendingIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val resetPendingIntent = PendingIntent.getService(
            this, 3,
            Intent(this, TimerService::class.java).setAction(ACTION_RESET),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val skipPendingIntent = PendingIntent.getService(
            this, 4,
            Intent(this, TimerService::class.java).setAction(ACTION_SKIP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${_timerData.value.sessionType.displayName}")
            .setContentText(formatTime(_timerData.value.timeRemaining))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_play, "Start", startPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Skip", skipPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${_timerData.value.sessionType.displayName}")
            .setContentText(formatTime(_timerData.value.timeRemaining))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer notifications for Pomodoro sessions"
            enableVibration(true)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun playNotificationSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, alarmUri)
            ringtone.play()
            android.os.Handler(mainLooper).postDelayed({
                if (ringtone.isPlaying) ringtone.stop()
            }, 5000)
        } catch (e: Exception) { }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
