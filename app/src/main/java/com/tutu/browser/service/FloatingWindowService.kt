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
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.tutu.browser.MainActivity
import com.tutu.browser.R

class FloatingWindowService : Service() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val CHANNEL_ID = "tutu_floating_window"
        const val NOTIFICATION_ID = 1002
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var floatingWebView: WebView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: "https://www.youtube.com"

        startForeground(NOTIFICATION_ID, buildNotification())
        showFloatingWindow(url)

        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun showFloatingWindow(url: String) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Root container
        val container = FrameLayout(this)
        container.setBackgroundColor(0xFF000000.toInt())

        // WebView inside the floating window
        floatingWebView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Inject CSS to hide YouTube UI elements - show only video
                    view?.evaluateJavascript(
                        """
                        (function() {
                            var style = document.createElement('style');
                            style.textContent = `
                                /* Hide YouTube header, search, navigation */
                                #masthead-container, .ytd-masthead,
                                .ytp-chrome-top, .ytp-show-cards-title,
                                .ytp-title, .ytp-title-channel,
                                .ytp-gradient-top, .ytp-gradient-bottom,
                                #related, #comments, #secondary,
                                .ytd-watch-flexy #secondary,
                                .ytd-watch-flexy #related,
                                .ytd-comments,
                                #ticket-shelf, #merch-shelf,
                                #header, #search-container,
                                .ytp-pause-overlay,
                                .ytp-watermark,
                                .annotation,
                                .video-annotations,
                                .iv-branding,
                                .ytp-chrome-bottom .ytp-button:not(.ytp-play-button):not(.ytp-mute-button):not(.ytp-fullscreen-button),
                                .ytp-settings-menu {
                                    display: none !important;
                                    visibility: hidden !important;
                                    opacity: 0 !important;
                                }
                                
                                /* Make video container fullscreen */
                                #player-container, .html5-video-player,
                                .ytd-player, #movie_player,
                                video {
                                    width: 100% !important;
                                    height: 100% !important;
                                    position: fixed !important;
                                    top: 0 !important;
                                    left: 0 !important;
                                    object-fit: contain !important;
                                }
                                
                                /* Hide body scrollbars */
                                body {
                                    overflow: hidden !important;
                                    background: black !important;
                                }
                                
                                /* Keep video controls visible */
                                .ytp-chrome-bottom {
                                    opacity: 1 !important;
                                    display: block !important;
                                }
                            `;
                            document.head.appendChild(style);
                            
                            // Force video to fill screen and unmute
                            var video = document.querySelector('video');
                            if (video) {
                                video.style.width = '100%';
                                video.style.height = '100%';
                                video.style.objectFit = 'contain';
                                video.muted = false;
                                video.volume = 1.0;
                                video.play();
                            }
                            
                            // Hide all elements except player
                            var allElements = document.body.children;
                            for (var i = 0; i < allElements.length; i++) {
                                var el = allElements[i];
                                if (!el.querySelector('video') && !el.querySelector('#player')) {
                                    el.style.display = 'none';
                                }
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }

        // Close button — top-right corner (scaled down)
        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(0xCC000000.toInt())
            setPadding(4, 4, 4, 4)
            setOnClickListener { stopSelf() }
        }

        // Close button: scaled down to match smaller window (80x80 -> 40x40)
        val closeBtnParams = FrameLayout.LayoutParams(40, 40).apply {
            gravity = Gravity.TOP or Gravity.END
        }

        container.addView(floatingWebView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Drag handle — transparent strip at top for moving window
        val dragHandle = View(this).apply {
            setBackgroundColor(0x55000000.toInt()) // Semi-transparent black
        }
        val dragHandleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 30
        ).apply {
            gravity = Gravity.TOP
        }
        container.addView(dragHandle, dragHandleParams)
        container.addView(closeBtn, closeBtnParams)

        // WindowManager layout params
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Window size: half of original (800x500 -> 400x250)
        val params = WindowManager.LayoutParams(
            400, 250,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Make the window draggable via drag handle
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(container, params)
    }

    override fun onDestroy() {
        floatingWebView?.destroy()
        floatingView?.let { windowManager?.removeView(it) }
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
