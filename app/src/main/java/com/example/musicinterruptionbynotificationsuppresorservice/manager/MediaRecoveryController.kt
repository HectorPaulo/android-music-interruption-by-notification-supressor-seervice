package com.example.musicinterruptionbynotificationsuppresorservice.manager

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

class MediaRecoveryController(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun forceResumeMusic() {
        // Retraso de 50ms para permitir que el sistema intente pausar,
        // e inmediatamente inyectar la orden de Play para contrarrestarlo.
        Handler(Looper.getMainLooper()).postDelayed({
            val keyEventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
            val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)

            audioManager.dispatchMediaKeyEvent(keyEventDown)
            audioManager.dispatchMediaKeyEvent(keyEventUp)
        }, 50)
    }
}
