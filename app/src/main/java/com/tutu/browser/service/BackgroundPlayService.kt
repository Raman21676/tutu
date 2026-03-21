package com.tutu.browser.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.tutu.browser.MainActivity
import com.tutu.browser.R

/**
 * Foreground service to keep the app process alive when background play is enabled.
 * This allows WebView audio to continue playing when the app is in the background.
 */
class BackgroundPlayService : Service() {

    companion object {
        const val CHANNEL_ID = "tutu_background_play"
        const val CHANNEL_NAME = "Background Play"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.tutu.browser.ACTION_START_BACKGROUND_PLAY"
        const val ACTION_STOP = "com.tutu.browser.ACTION_STOP_BACKGROUND_PLAY"
        
        @Volatile
        var isRunning = false
            private set
    }

    private val binder = LocalBinder()
    private var audioFocusRequest: AudioFocusRequest? = null

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundPlayService = this@BackgroundPlayService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("BackgroundPlayService", "onStartCommand: ${intent?.action}")
        
        if (intent?.action == ACTION_STOP) {
            android.util.Log.d("BackgroundPlayService", "Stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Mark as running
        isRunning = true

        // Request audio focus so Android doesn't silence the audio
        requestAudioFocus()

        // Keep screen partially awake so WebView JS doesn't get throttled
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TutuBrowser::BackgroundAudio"
        )
        wakeLock?.acquire(3600000L) // 1 hour max
        android.util.Log.d("BackgroundPlayService", "WakeLock acquired")

        startForeground(NOTIFICATION_ID, createNotification())
        android.util.Log.d("BackgroundPlayService", "Foreground service started with notification")
        return START_STICKY
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        abandonAudioFocus()
        // Release WakeLock
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Allows audio to play in the background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                // CRITICAL FIX: FLAG_ACTIVITY_CLEAR_TOP was wiping the entire navigation
                // back stack (including WebScreen), sending the user back to the Home screen
                // every time they tapped the notification. REORDER_TO_FRONT brings the
                // existing app instance forward without touching the back stack.
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, BackgroundPlayService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tutu Browser")
            .setContentText("Playing in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
