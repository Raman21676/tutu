package com.tutu.browser.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.tutu.browser.MainActivity
import com.tutu.browser.R

/**
 * Floating Window Service - Creates a floating WebView that stays on top of other apps.
 * This provides Picture-in-Picture-like functionality for Android < 8 (API 26).
 */
class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowSvc"
        private const val CHANNEL_ID = "tutu_floating_window"
        private const val CHANNEL_NAME = "Floating Window"
        private const val NOTIFICATION_ID = 1002
        
        const val ACTION_START = "com.tutu.browser.action.START_FLOATING"
        const val ACTION_STOP = "com.tutu.browser.action.STOP_FLOATING"
        const val ACTION_EXPAND = "com.tutu.browser.action.EXPAND_FLOATING"
        
        // Default window size (in pixels)
        private const val DEFAULT_WIDTH = 400
        private const val DEFAULT_HEIGHT = 225  // 16:9 aspect ratio
        
        @Volatile
        var isRunning = false
            private set
        
        @Volatile
        var currentUrl: String = ""
            private set
    }

    private val binder = LocalBinder()
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var webView: WebView? = null
    private var params: WindowManager.LayoutParams? = null
    
    // Window state
    private var isExpanded = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var screenWidth = 0
    private var screenHeight = 0

    inner class LocalBinder : Binder() {
        fun getService(): FloatingWindowService = this@FloatingWindowService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        
        // Get screen dimensions
        val display = windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        screenWidth = size.x
        screenHeight = size.y
        
        // Default position: bottom-right corner
        initialX = screenWidth - DEFAULT_WIDTH - 20
        initialY = screenHeight - DEFAULT_HEIGHT - 200
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_STOP -> {
                stopFloatingWindow()
                return START_NOT_STICKY
            }
            ACTION_EXPAND -> {
                toggleExpand()
                return START_STICKY
            }
            else -> {
                // Start floating window
                val url = intent?.getStringExtra("url") ?: currentUrl
                val title = intent?.getStringExtra("title") ?: "TuTu Browser"
                startFloatingWindow(url, title)
            }
        }
        
        return START_STICKY
    }

    private fun startFloatingWindow(url: String, title: String) {
        if (isRunning && floatingView != null) {
            // Already running, just load new URL
            if (url.isNotBlank() && url != currentUrl) {
                currentUrl = url
                webView?.loadUrl(url)
            }
            return
        }
        
        Log.d(TAG, "Starting floating window with URL: $url")
        currentUrl = url
        isRunning = true
        
        // Create and show the floating window
        createFloatingView(url, title)
        
        // Start as foreground service
        val notification = createNotification(title)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        Log.d(TAG, "Floating window service started")
    }

    private fun createFloatingView(url: String, title: String) {
        // Inflate the floating window layout
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_window_layout, null)
        
        // Setup WebView
        webView = floatingView?.findViewById(R.id.floating_webview)
        setupWebView(webView, url)
        
        // Setup controls
        setupControls(floatingView, title)
        
        // Setup window parameters
        params = WindowManager.LayoutParams(
            DEFAULT_WIDTH,
            DEFAULT_HEIGHT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        
        // Add touch listener for dragging
        setupDragListener()
        
        // Add to window
        try {
            windowManager?.addView(floatingView, params)
            Log.d(TAG, "Floating view added to window")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view", e)
            stopFloatingWindow()
        }
    }

    private fun setupWebView(webView: WebView?, url: String) {
        webView?.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                mediaPlaybackRequiresUserGesture = false
                
                // Use mobile UA for better compatibility in small window
                userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
            }
            
            // Enable cookies
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            
            webViewClient = WebViewClient()
            
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    // Update loading progress if needed
                }
            }
            
            if (url.isNotBlank()) {
                loadUrl(url)
            }
        }
    }

    private fun setupControls(floatingView: View?, title: String) {
        // Title text
        val titleText = floatingView?.findViewById<TextView>(R.id.floating_title)
        titleText?.text = title.takeIf { it.isNotBlank() } ?: "TuTu Browser"
        
        // Close button
        floatingView?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            stopFloatingWindow()
        }
        
        // Expand/Collapse button
        floatingView?.findViewById<ImageButton>(R.id.btn_expand)?.setOnClickListener {
            toggleExpand()
        }
        
        // Pause/Resume button
        floatingView?.findViewById<ImageButton>(R.id.btn_pause)?.setOnClickListener {
            // Toggle video playback
            webView?.evaluateJavascript(
                """
                (function() {
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(v) {
                        if (v.paused) v.play(); else v.pause();
                    });
                })();
                """.trimIndent(),
                null
            )
        }
    }

    private fun setupDragListener() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Single tap - expand the window
                toggleExpand()
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Double tap - open in main app
                openInMainApp()
                return true
            }
        })
        
        floatingView?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    
                    // Update window position
                    if (floatingView != null && params != null) {
                        windowManager?.updateViewLayout(floatingView, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpand() {
        if (!isRunning || floatingView == null || params == null) return
        
        isExpanded = !isExpanded
        
        if (isExpanded) {
            // Expand to larger size
            params?.width = (screenWidth * 0.8).toInt()
            params?.height = (screenHeight * 0.6).toInt()
            
            // Center the window
            params?.x = (screenWidth - params!!.width) / 2
            params?.y = (screenHeight - params!!.height) / 2
            
            // Make focusable when expanded
            params?.flags = params?.flags?.and(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
                ?: WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            // Collapse back to small size
            params?.width = DEFAULT_WIDTH
            params?.height = DEFAULT_HEIGHT
            
            // Return to not focusable
            params?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }
        
        // Update the view
        try {
            windowManager?.updateViewLayout(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update window layout", e)
        }
        
        // Update expand button icon
        val expandBtn = floatingView?.findViewById<ImageButton>(R.id.btn_expand)
        expandBtn?.setImageResource(if (isExpanded) android.R.drawable.arrow_down_float else android.R.drawable.arrow_up_float)
    }

    private fun openInMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("restore_url", currentUrl)
        }
        startActivity(intent)
        
        // Stop floating window after opening main app
        Handler(Looper.getMainLooper()).postDelayed({
            stopFloatingWindow()
        }, 500)
    }

    private fun stopFloatingWindow() {
        Log.d(TAG, "Stopping floating window")
        isRunning = false
        
        // Remove floating view
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view", e)
            }
            floatingView = null
        }
        
        // Clean up WebView
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        webView = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Log.d(TAG, "Floating window stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating window is active"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String): Notification {
        // Intent to open the app when notification is tapped
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("restore_url", currentUrl)
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop floating window
        val stopIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to expand floating window
        val expandIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_EXPAND
        }
        val expandPendingIntent = PendingIntent.getService(
            this,
            2,
            expandIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title.takeIf { it.isNotBlank() } ?: "Floating Window")
            .setContentText("Tap to return to TuTu Browser")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .addAction(android.R.drawable.arrow_up_float, "Expand", expandPendingIntent)
            .build()
    }

    fun updateUrl(url: String) {
        currentUrl = url
        if (url.isNotBlank() && webView != null) {
            webView?.loadUrl(url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isRunning = false
        
        // Cleanup
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // Ignore
            }
        }
        webView?.destroy()
    }
}
