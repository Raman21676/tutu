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
import com.tutu.browser.util.WebViewHolder

class FloatingWindowService : Service() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val CHANNEL_ID = "tutu_floating_window"
        const val NOTIFICATION_ID = 1002
    }
    
    private var currentTimestamp: Float = 0f

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
        currentTimestamp = intent?.getFloatExtra(EXTRA_TIMESTAMP, 0f) ?: 0f
        
        android.util.Log.d("FloatingWindow", "onStartCommand: url=$url, timestamp=$currentTimestamp")

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
                            
                            // Force video to fill screen, unmute, and seek to saved position
                            var savedTime = $currentTimestamp;
                            var seekToTime = function() {
                                var video = document.querySelector('video');
                                if (video && savedTime > 0) {
                                    video.currentTime = savedTime;
                                    console.log('Seeked to: ' + savedTime);
                                }
                                if (video) {
                                    video.style.width = '100%';
                                    video.style.height = '100%';
                                    video.style.objectFit = 'contain';
                                    video.muted = false;
                                    video.volume = 1.0;
                                    video.play();
                                }
                            };
                            
                            // Try seeking immediately and also after metadata loads
                            var video = document.querySelector('video');
                            if (video) {
                                if (video.readyState >= 1) {
                                    // Metadata already loaded
                                    seekToTime();
                                } else {
                                    // Wait for metadata
                                    video.addEventListener('loadedmetadata', seekToTime);
                                    // Also try immediately as fallback
                                    setTimeout(seekToTime, 100);
                                    // And again after a bit more time
                                    setTimeout(seekToTime, 500);
                                    setTimeout(seekToTime, 1000);
                                }
                            } else {
                                // Video not found yet, try again later
                                setTimeout(seekToTime, 500);
                                setTimeout(seekToTime, 1000);
                                setTimeout(seekToTime, 2000);
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

        // CLOSE BUTTON - Top right corner (initially hidden)
        val closeBtn = android.widget.Button(this).apply {
            text = "X"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF000000.toInt()) // Solid black
            visibility = View.GONE // Hidden by default
            setOnClickListener { stopSelf() }
        }
        val closeBtnParams = FrameLayout.LayoutParams(60, 60).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 0
            rightMargin = 0
        }

        // MAXIMIZE BUTTON - Left of close button (initially hidden)
        val maximizeBtn = android.widget.Button(this).apply {
            text = "MAX"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF333333.toInt()) // Dark gray
            visibility = View.GONE // Hidden by default
            setOnClickListener {
                android.util.Log.d("FloatingWindow", "MAX button clicked!")
                floatingWebView?.evaluateJavascript(
                    "(function() { var v = document.querySelector('video'); return v ? v.currentTime : 0; })();"
                ) { result ->
                    val cleanResult = result?.trim('"', ' ')
                    val timestamp = cleanResult?.toFloatOrNull() ?: 0f
                    WebViewHolder.currentTimestamp = timestamp
                    android.util.Log.d("FloatingWindow", "Maximize: Timestamp=$timestamp")
                    val intent = Intent(this@FloatingWindowService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("restore_url", url)
                    }
                    startActivity(intent)
                    stopSelf()
                }
            }
        }
        val maximizeBtnParams = FrameLayout.LayoutParams(80, 60).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 0
            rightMargin = 65 // Right of close button
        }

        // DRAG HANDLE - Top area for moving window (always visible but transparent)
        val dragHandle = View(this).apply {
            setBackgroundColor(0x00000000.toInt()) // Fully transparent
        }
        val dragHandleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 60
        ).apply {
            gravity = Gravity.TOP
        }

        // Add views: WebView first, then overlays on top
        container.addView(floatingWebView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // Add buttons directly to container (on top of WebView)
        container.addView(dragHandle, dragHandleParams)
        container.addView(maximizeBtn, maximizeBtnParams)
        container.addView(closeBtn, closeBtnParams)
        
        // Ensure buttons are on top
        maximizeBtn.bringToFront()
        closeBtn.bringToFront()
        
        // Auto-hide handler
        val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val hideRunnable = Runnable {
            maximizeBtn.visibility = View.GONE
            closeBtn.visibility = View.GONE
            android.util.Log.d("FloatingWindow", "Buttons auto-hidden")
        }
        
        // Function to show buttons and schedule hide
        fun showButtons() {
            maximizeBtn.visibility = View.VISIBLE
            closeBtn.visibility = View.VISIBLE
            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 3000) // Hide after 3 seconds
            android.util.Log.d("FloatingWindow", "Buttons shown")
        }
        
        // Tap detector to show buttons
        container.setOnClickListener {
            if (maximizeBtn.visibility == View.VISIBLE) {
                // If already visible, hide them
                maximizeBtn.visibility = View.GONE
                closeBtn.visibility = View.GONE
                hideHandler.removeCallbacks(hideRunnable)
            } else {
                // Show buttons
                showButtons()
            }
        }
        
        // WindowManager layout params - MUST be defined before touch listener
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

        // Initialize drag variables before they're used in the touch listener
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        
        // Also show buttons when drag handle is touched
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    showButtons()
                    // Also handle drag
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
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
