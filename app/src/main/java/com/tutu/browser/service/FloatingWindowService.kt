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
import android.util.Log
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
        private const val TAG = "FloatingWindowService"
        
        // Hide everything except the video player
        private val YOUTUBE_MINIMAL_CSS = """
            (function(){
                var s=document.createElement('style');
                s.textContent='ytd-masthead,#masthead-container,ytd-watch-next-secondary-results-renderer,#secondary,ytd-comments,#comments,ytd-app>ytd-page-manager>ytd-watch-flexy #below,paper-tooltip,ytd-engagement-panel-section-list-renderer{display:none!important;}#player-container,#movie_player,video{width:100vw!important;height:100vh!important;max-width:100vw!important;max-height:100vh!important;position:fixed!important;top:0!important;left:0!important;z-index:9999!important;}';
                document.head.appendChild(s);
            })();
        """.trimIndent()
    }

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var floatingWebView: WebView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: WebViewHolder.currentUrl
        val timestamp = intent?.getLongExtra(EXTRA_TIMESTAMP, 0L) ?: 0L

        if (url.isBlank()) {
            Log.w(TAG, "No URL provided, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        showFloatingWindow(url, timestamp)
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun showFloatingWindow(url: String, timestamp: Long) {
        Log.d(TAG, "Showing floating window: $url at ${timestamp}ms")

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val container = FrameLayout(this)
        container.setBackgroundColor(0xFF000000.toInt())

        // New WebView — same URL, seeks to saved timestamp
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
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    // 1. Hide all YouTube UI — show only video
                    view.evaluateJavascript(YOUTUBE_MINIMAL_CSS, null)
                    // 2. Seek to saved timestamp
                    if (timestamp > 0) {
                        view.evaluateJavascript(
                            "setTimeout(function(){" +
                            "var v=document.querySelector('video');" +
                            "if(v){v.currentTime=${timestamp / 1000.0};v.play();v.muted=false;v.volume=1.0;}" +
                            "}, 1500);", null
                        )
                    } else {
                        view.evaluateJavascript(
                            "setTimeout(function(){" +
                            "var v=document.querySelector('video');" +
                            "if(v){v.play();v.muted=false;v.volume=1.0;}" +
                            "}, 1500);", null
                        )
                    }
                }
            }
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            loadUrl(url)
        }

        // Add WebView to container
        container.addView(floatingWebView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Drag handle — sits on TOP of WebView, intercepts touch in top strip
        val dragHandle = View(this).apply {
            setBackgroundColor(0xAA222222.toInt())
        }

        // Drag handle strip: full width, 28dp tall, at top
        val handleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 56
        ).apply { gravity = Gravity.TOP }
        container.addView(dragHandle, handleParams)

        // Close button on top of drag handle
        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(0xCC000000.toInt())
            setPadding(6, 6, 6, 6)
            setOnClickListener { stopSelf() }
        }
        container.addView(closeBtn, FrameLayout.LayoutParams(60, 56).apply {
            gravity = Gravity.TOP or Gravity.END
        })

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        // Change flags to allow touch input to reach drag handle
        val params = WindowManager.LayoutParams(
            307, 192, type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Touch listener on the drag handle — not the container
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    ix = params.x; iy = params.y
                    tx = event.rawX; ty = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = ix + (event.rawX - tx).toInt()
                    params.y = iy + (event.rawY - ty).toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(container, params)
        Log.d(TAG, "Floating window added to screen")
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying floating window")
        floatingWebView?.destroy()
        floatingView?.let { windowManager?.removeView(it) }
        floatingView = null
        floatingWebView = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Floating Window",
                NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0,
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
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
