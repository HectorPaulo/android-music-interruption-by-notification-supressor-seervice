package com.example.musicinterruptionbynotificationsuppresorservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.musicinterruptionbynotificationsuppresorservice.service.AudioGuardService

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkListenerAndStartService()
        } else {
            Toast.makeText(this, "Se requiere permiso de notificaciones", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trampolín: Esta activity no tiene UI.
        // Solo comprueba/pide permisos, lanza el servicio y se cierra.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        checkListenerAndStartService()
    }

    private fun checkListenerAndStartService() {
        if (!isNotificationListenerGranted(this)) {
            Toast.makeText(this, "Por favor, habilita el acceso a notificaciones para AudioGuard", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            finish()
        } else {
            val intent = Intent(this, AudioGuardService::class.java)
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "AudioGuard activado", Toast.LENGTH_SHORT).show()
            finish() // Cierra la interfaz inmediatamente
        }
    }

    private fun isNotificationListenerGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }
}
