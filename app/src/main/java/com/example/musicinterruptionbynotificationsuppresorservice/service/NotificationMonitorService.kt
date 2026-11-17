package com.example.musicinterruptionbynotificationsuppresorservice.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.musicinterruptionbynotificationsuppresorservice.manager.MediaRecoveryController

class NotificationMonitorService : NotificationListenerService() {

    private var recoveryController: MediaRecoveryController? = null

    override fun onCreate() {
        super.onCreate()
        recoveryController = MediaRecoveryController(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            val packageName = notification.packageName
            val title = notification.notification.extras.getString("android.title") ?: "Sin título"

            Log.d("AudioGuard", "Notificación detectada de: $packageName | Título: $title")

            // Aquí disparamos la recuperación inmediatamente tras detectar una notificación
            // Se puede agregar lógica adicional en el futuro para filtrar por paquete si se desea
            recoveryController?.forceResumeMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recoveryController = null
    }
}
