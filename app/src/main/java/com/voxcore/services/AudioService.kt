package com.voxcore.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.voxcore.audio.VoxEngine

/** Mikrofon ses motorunu ön planda çalıştırır; ekran kapansa da kesilmez. */
class AudioService : Service() {

    override fun onCreate() {
        super.onCreate()
        VoxEngine.onStopped = { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Ses motoru", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("VOXCORE")
            .setContentText("Mikrofon işleniyor")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        ServiceCompat.startForeground(this, 1, notification, type)
        VoxEngine.start()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        VoxEngine.stop()
        VoxEngine.onStopped = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { private const val CHANNEL = "voxcore_engine" }
}
