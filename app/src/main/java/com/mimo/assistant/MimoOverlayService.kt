package com.mimo.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MimoOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: LinearLayout? = null
    private lateinit var messageView: TextView

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SHOW_MESSAGE -> showMessage(intent.getStringExtra(EXTRA_MESSAGE).orEmpty())
            else -> showMessage("Mimo is ready. Open Mimo to talk.")
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, MimoNotifications.readyNotification(this))
        createOverlay()
    }

    override fun onDestroy() {
        overlayView?.let(windowManager::removeView)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlay() {
        val density = resources.displayMetrics.density
        val padding = (10 * density).toInt()
        val size = (72 * density).toInt()
        messageView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(padding, padding / 2, padding, padding / 2)
            text = "Mimo is ready"
        }
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.rgb(62, 11, 68))
            addView(ImageView(this@MimoOverlayService).apply {
                setImageResource(R.drawable.mimo_logo)
                layoutParams = LinearLayout.LayoutParams(size, size)
            })
            addView(messageView)
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = padding
            y = (80 * density).toInt()
        }
        windowManager.addView(overlayView, layoutParams)
    }

    private fun showMessage(message: String) {
        if (::messageView.isInitialized && message.isNotBlank()) messageView.text = message.take(120)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mimo screen bubble",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.mimo.assistant.STOP_OVERLAY"
        const val ACTION_SHOW_MESSAGE = "com.mimo.assistant.SHOW_OVERLAY_MESSAGE"
        const val EXTRA_MESSAGE = "message"
        const val CHANNEL_ID = "mimo_overlay"
        const val NOTIFICATION_ID = 8001
    }
}
