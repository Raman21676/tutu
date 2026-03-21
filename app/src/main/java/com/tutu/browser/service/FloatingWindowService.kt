package com.tutu.browser.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.tutu.browser.MainActivity
import com.tutu.browser.R
import com.tutu.browser.util.WebViewHolder

class FloatingWindowService : Service() {

    companion object {
        const val CHANNEL_ID = "tutu_floating_window"
        const val NOTIFICATION_ID = 1002
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var webView: WebView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FloatingWindowService", "onStartCommand called")
        // Get the existing WebView from WebViewHolder
        val wv = WebViewHolder.webView
        Log.d("FloatingWindowService", "WebView from holder: $wv")
        if (wv != null) {
            attachWebView(wv)
            startForeground(NOTIFICATION_ID, buildNotification())
        } else {
            Log.e("FloatingWindowService", "WebView is null, stopping service")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachWebView(wv: WebView) {
        webView = wv
        
        // Remove from any existing parent first
        (wv.parent as? ViewGroup)?.removeView(wv)
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Build container with WebView and close button
        val container = buildContainer(wv)

        // WindowManager layout params
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Window size: 307x192 pixels (15% bigger than one-third)
        val params = WindowManager.LayoutParams(
            307, 192,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        makeDraggable(container, params)
        floatingView = container
        windowManager?.addView(container, params)
        
        // Unmute video
        wv.evaluateJavascript(
            "document.querySelectorAll('video').forEach(v=>{v.muted=false;v.volume=1.0;});",
            null
        )
    }

    private fun buildContainer(wv: WebView): FrameLayout {
        val container = FrameLayout(this)
        container.setBackgroundColor(0xFF000000.toInt())

        // Close button — top-right corner (30x30)
        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(0xCC000000.toInt())
            setPadding(4, 4, 4, 4)
            setOnClickListener { stopSelf() }
        }

        val closeBtnParams = FrameLayout.LayoutParams(30, 30).apply {
            gravity = Gravity.TOP or Gravity.END
        }

        container.addView(wv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        container.addView(closeBtn, closeBtnParams)

        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeDraggable(container: FrameLayout, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                    }
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        // Remove view from WindowManager but DON'T destroy WebView
        // It belongs to the Activity
        floatingView?.let {
            windowManager?.removeView(it)
        }
        webView = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Floating Window",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TuTu Browser")
            .setContentText("Floating window active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
