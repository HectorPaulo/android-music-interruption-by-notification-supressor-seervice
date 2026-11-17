package com.example.musicinterruptionbynotificationsuppresorservice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent

import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.musicinterruptionbynotificationsuppresorservice.R

class AudioGuardService : Service() {

    companion object {
        private const val CHANNEL_ID = "AudioGuardChannel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Intent para detener el servicio desde la propia notificación
        val stopIntent = Intent(this, AudioGuardService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Supresor de Interrupciones Activo")
            .setContentText("Toca 'Detener' para apagar.")
            .setSmallIcon(R.drawable.ic_audio_guard)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .addAction(0, "Detener", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Servicio Audio Guard",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene el supresor de interrupciones corriendo en segundo plano"
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
