package com.auxiliofx.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var panel: View? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(7, notification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching {
            applicationContext.startService(Intent(applicationContext, OverlayService::class.java))
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        panel?.let { view -> runCatching { windowManager.removeView(view) } }
        panel = null
        super.onDestroy()
    }

    private fun notification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("fx_overlay", "Painel FX flutuante", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "fx_overlay")
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Auxílio FX")
            .setContentText("Painel flutuante ativo sobre o jogo")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    private fun showPanel() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 24
        params.y = 160

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#EE071022"))
        root.setPadding(24, 16, 24, 20)

        val title = TextView(this)
        title.text = "AUXÍLIO FX  ≡"
        title.setTextColor(Color.parseColor("#2F9BFF"))
        title.textSize = 13f
        title.setPadding(8, 8, 8, 16)
        root.addView(title)

        title.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (event.rawX - touchX).toInt()
                        params.y = startY + (event.rawY - touchY).toInt()
                        runCatching { windowManager.updateViewLayout(root, params) }
                    }
                }
                return true
            }
        })

        root.addView(actionButton("SENSI 100%") { FxCommands.boostSensi() })
        root.addView(actionButton("OTIMIZAR") { FxCommands.performance() })
        root.addView(actionButton("ABRIR FF") { FxCommands.openGame(this) })
        root.addView(actionButton("FECHAR") { stopSelf() })

        panel = root
        windowManager.addView(root, params)
    }

    private fun actionButton(label: String, action: () -> String): Button {
        val button = Button(this)
        button.text = label
        button.textSize = 11f
        button.isAllCaps = false
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(Color.parseColor("#132A4A"))
        button.setOnClickListener {
            val result = runCatching { action() }.getOrDefault("ERRO")
            Toast.makeText(this, result.take(80), Toast.LENGTH_SHORT).show()
        }
        return button
    }
}
