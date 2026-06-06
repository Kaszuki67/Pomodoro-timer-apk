package com.simplepomodoro.service

import android.app.Notification
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
        const val ACTION_UPDATE_SETTINGS = "com.simplepomodoro.UPDATE_SETTINGS"
        const val ACTION_SELECT_SESSION = "com.simplepomodoro.SELECT_SESSION"

        const val EXTRA_WORK_DURATION = "work_duration"
        const val EXTRA_SHORT_BREAK_DURATION = "short_break_duration"
        const val EXTRA_LONG_BREAK_DURATION = "long_break_duration"
        const val EXTRA_SESSION_TYPE = "session_type"

        private const val MIN_DURATION_SECONDS = 60
        private const val WAKE_LOCK_BUFFER_SECONDS = 60

        private var _workDuration = 25 * 60
        private var _shortBreakDuration = 5 * 60
        private var _longBreakDuration = 15 * 60

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

        fun setDurations(work: Int, shortBreak: Int, longBreak: Int) {
            _workDuration = work.coerceAtLeast(MIN_DURATION_SECONDS)
            _shortBreakDuration = shortBreak.coerceAtLeast(MIN_DURATION_SECONDS)
            _longBreakDuration = longBreak.coerceAtLeast(MIN_DURATION_SECONDS)

            val current = _timerData.value
            if (current.currentState == TimerState.Idle || current.currentState == TimerState.Paused) {
                val newDuration = durationFor(current.sessionType)
                _timerData.value = current.copy(
                    timeRemaining = newDuration,
                    totalDuration = newDuration
                )
            }
        }

        private fun durationFor(type: SessionType): Int {
            return when (type) {
                SessionType.WORK -> _workDuration
                SessionType.SHORT_BREAK -> _shortBreakDuration
                SessionType.LONG_BREAK -> _longBreakDuration
            }
        }
    }

    private var countDownTimer: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var vibrator: Vibrator

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
        applyDurationExtras(intent)

        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
            ACTION_SKIP -> skipSession()
            ACTION_SELECT_SESSION -> selectSession(intent.getStringExtra(EXTRA_SESSION_TYPE))
            ACTION_UPDATE_SETTINGS -> updateNotificationIfRunning()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?) = null

    private fun applyDurationExtras(intent: Intent?) {
        if (intent == null) return

        val work = intent.getIntExtra(EXTRA_WORK_DURATION, _workDuration)
        val shortBreak = intent.getIntExtra(EXTRA_SHORT_BREAK_DURATION, _shortBreakDuration)
        val longBreak = intent.getIntExtra(EXTRA_LONG_BREAK_DURATION, _longBreakDuration)
        setDurations(work, shortBreak, longBreak)
    }

    private fun startTimer() {
        if (_timerData.value.currentState == TimerState.Running) return
        if (_timerData.value.timeRemaining <= 0) resetTimer()

        _timerData.value = _timerData.value.copy(currentState = TimerState.Running)
        startForeground(NOTIFICATION_ID, createNotification())

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(
            _timerData.value.timeRemaining.toLong() * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000L).toInt().coerceAtLeast(0)
                _timerData.value = _timerData.value.copy(timeRemaining = secondsRemaining)
                updateNotification()
            }

            override fun onFinish() {
                onTimerComplete()
            }
        }.start()

        acquireWakeLockForCurrentSession()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        _timerData.value = _timerData.value.copy(currentState = TimerState.Paused)
        releaseWakeLock()
        updateNotificationIfRunning()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        val duration = getDurationForSession(_timerData.value.sessionType)
        _timerData.value = _timerData.value.copy(
            currentState = TimerState.Idle,
            timeRemaining = duration,
            totalDuration = duration
        )
        releaseWakeLock()
        stopForegroundNotification()
    }

    private fun skipSession() {
        countDownTimer?.cancel()
        releaseWakeLock()
        moveToNextSession()
    }

    private fun selectSession(sessionTypeName: String?) {
        val selectedType = runCatching { SessionType.valueOf(sessionTypeName.orEmpty()) }
            .getOrDefault(SessionType.WORK)
        val duration = getDurationForSession(selectedType)

        countDownTimer?.cancel()
        _timerData.value = _timerData.value.copy(
            currentState = TimerState.Idle,
            sessionType = selectedType,
            timeRemaining = duration,
            totalDuration = duration
        )
        releaseWakeLock()
        stopForegroundNotification()
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
        releaseWakeLock()
        moveToNextSession()
    }

    private fun moveToNextSession() {
        val currentType = _timerData.value.sessionType
        val completed = _timerData.value.completedSessions

        val nextType = when {
            currentType == SessionType.WORK && completed > 0 && completed % 4 == 0 -> SessionType.LONG_BREAK
            currentType == SessionType.WORK -> SessionType.SHORT_BREAK
            else -> SessionType.WORK
        }

        val nextDuration = getDurationForSession(nextType)
        _timerData.value = _timerData.value.copy(
            sessionType = nextType,
            timeRemaining = nextDuration,
            totalDuration = nextDuration,
            currentState = TimerState.Idle
        )
        startTimer()
    }

    private fun getDurationForSession(type: SessionType): Int {
        return when (type) {
            SessionType.WORK -> _workDuration
            SessionType.SHORT_BREAK -> _shortBreakDuration
            SessionType.LONG_BREAK -> _longBreakDuration
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val startPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TimerService::class.java).setAction(ACTION_START),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, TimerService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val resetPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, TimerService::class.java).setAction(ACTION_RESET),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val skipPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, TimerService::class.java).setAction(ACTION_SKIP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseLabel = if (_timerData.value.currentState == TimerState.Running) "Pause" else "Start"
        val playPauseIcon = if (_timerData.value.currentState == TimerState.Running) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseIntent = if (_timerData.value.currentState == TimerState.Running) pausePendingIntent else startPendingIntent

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(_timerData.value.sessionType.displayName)
            .setContentText(formatTime(_timerData.value.timeRemaining))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(_timerData.value.currentState == TimerState.Running)
            .setOnlyAlertOnce(true)
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Skip", skipPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun updateNotificationIfRunning() {
        if (_timerData.value.currentState == TimerState.Running || _timerData.value.currentState == TimerState.Paused) {
            updateNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

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

    private fun acquireWakeLockForCurrentSession() {
        releaseWakeLock()
        val timeoutMillis = (_timerData.value.timeRemaining + WAKE_LOCK_BUFFER_SECONDS).toLong() * 1000L
        wakeLock?.acquire(timeoutMillis)
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
    }

    private fun stopForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun playNotificationSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, alarmUri)
            ringtone.play()
            android.os.Handler(mainLooper).postDelayed({
                if (ringtone.isPlaying) ringtone.stop()
            }, 5000L)
        } catch (_: Exception) {
            // Keep timer transitions reliable even if the device cannot play a sound.
        }
    }

    private fun vibrate() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500L)
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }
}
