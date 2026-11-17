# Music Interruption Suppressor

**Music Interruption Suppressor** es una utilidad limpia, ligera y optimizada para Android que soluciona el molesto problema de las pausas automáticas en auriculares Bluetooth multipunto.

## El Problema 🎧
Muchos auriculares Bluetooth modernos (como los *Steren* y otros que manejan perfiles agresivos Hands-Free / A2DP) causan una interrupción forzosa. Cuando tu teléfono recibe una notificación del sistema operativo (un mensaje, un correo, etc.), los auriculares asumen erróneamente que entra una alerta crítica o llamada. Automáticamente cambian de canal de audio, pausando tu música, tus podcasts o el video que estabas reproduciendo. Desafortunadamente, nunca la vuelven a reproducir, obligándote a encender la pantalla del teléfono o tocar el botón del audífono para continuar tu reproducción.

## La Solución 🚀
Esta aplicación funciona bajo una arquitectura **"Headless" (sin interfaz gráfica)**. Monitorea las notificaciones y envía una instrucción inmediata y calculada para decirle al teléfono: *"Sigue reproduciendo"*.

* **Inyección en tiempo real:** Cuando percibe la notificación, la app espera un margen minúsculo de milisegundos (`50ms`) hasta que el auricular lanza el comando de pausa. ¡E inmediatamente contrarresta inyectando el comando multimedia de "Reproducir" (`KEYCODE_MEDIA_PLAY`)!
* **Arquitectura Transparente:** La app carece de pantallas. Al abrirla desde el launcher, arrancará el proceso y desaparecerá en milisegundos sin molestar tu experiencia de usuario.
* **Respeto a tu Batería:** No hace *polling* (revisión de ciclo) que drene tu batería. Utiliza el `NotificationListenerService` nativo del sistema para reaccionar solamente cuando ocurre un evento de sistema. 
* **Servicio Foregound (Primer plano):** Para evitar que el sistema mate la app, se ancla en tu panel de notificaciones de forma silenciosa. Si en cualquier momento ya no quieres utilizarla, basta con expandir tu panel de notificaciones y tocar el botón "Detener".

## Requisitos
* Android 8.0 (Oreo) - API Nivel 26 o superior.
* Otorgar permiso de interceptación de notificaciones (`Notification Listener`).
* Otorgar permiso de notificaciones (Android 13+).

## Instalación y Configuración

1. **Descargar e Instalar:** 
   Ve a los **Releases** de este repositorio, descarga el archivo `app-debug.apk` más reciente e instálalo en tu dispositivo Android.
2. **Iniciar la aplicación:**
   Toca el ícono **Music Interruption Suppressor** en tu lanzador de aplicaciones.
3. **Conceder Permisos:**
   * La app te mandará a las opciones de "Acceso a notificaciones".
   * Busca "Music Interruption Suppressor" en la lista y habilita su interruptor.
4. **Activación de Servicio:**
   Vuelve a tocar el ícono de la app. Observarás un discreto cartel (Toast) que dice **"Supresor activado"**. Al bajar tu panel de notificaciones, notarás un ícono de auriculares persistente que confirma que el protector de audio está corriendo silenciosamente en segundo plano.

¡Listo! Ponte tus auriculares y despreocúpate por las notificaciones invasivas.

## Compilación para Desarrolladores

Si deseas clonar el proyecto, revisarlo o adaptarlo:

```bash
git clone https://github.com/TU-USUARIO/android-music-interruption-by-notification-supressor-seervice.git
cd android-music-interruption-by-notification-supressor-seervice
./gradlew assembleDebug
```
El archivo compilado lo hallarás en `app/build/outputs/apk/debug/app-debug.apk`.

## Arquitectura

* **Activity Trampolín:** `MainActivity` hereda de `@android:style/Theme.Translucent.NoTitleBar` garantizando una ejecución silenciosa orientada a eventos.
* **NotificationMonitorService:** Recibe de forma pasiva mediante el `StatusBarNotification` cualquier interrupción y llama a su controlador asíncrono.
* **MediaRecoveryController:** Envuelve el `AudioManager` invocando la simulación del botón hardware `ACTION_DOWN` / `ACTION_UP` con un leve retraso `Looper` contra-medida.

## Licencia
Este proyecto es código abierto y está disponible bajo los términos de la Licencia MIT.
