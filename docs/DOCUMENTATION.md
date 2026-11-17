# Documentación Técnica: Music Interruption by Notification Suppressor Service (Audio Guard)

## Resumen del Proyecto
**Audio Guard** (Music Interruption by Notification Suppressor Service) es una aplicación minimalista y altamente optimizada para Android, diseñada para resolver un problema específico del hardware Bluetooth multipunto: las interrupciones forzadas de audio.

Auriculares como los *Steren* (y otras marcas con perfiles de Hands-Free y A2DP agresivos), al percibir la llegada de una notificación del sistema operativo, cambian el perfil del canal Bluetooth, pausando drásticamente el streaming multimedia en curso (música, videos, etc.) y requiriendo muchas veces la reanudación manual por parte del usuario.

Esta aplicación intercepta de manera segura los eventos del sistema y genera un evento de recuperación auditiva (Play) automático con un retraso calculado para contrarrestar la orden de pausa nativa.

---

## Arquitectura y Patrones de Diseño

El proyecto fue reestructurado desde un enfoque monolítico hacia uno de **separación de responsabilidades** basado en los componentes principales de Android (`Services`, `Broadcast/Events` y `Managers`). Se descartó la implementación del patrón MVVM y el uso de Jetpack Compose debido a que el proyecto no requiere una Interfaz de Usuario (UI) navegable, optando por una arquitectura tipo **"Servicio Sin Cabeza" (Headless Service)**.

### Decisiones Arquitectónicas Justificadas:

1. **Activity como Trampolín Invisible (`Trampoline Activity`)**:
   * *Decisión:* La clase `MainActivity` hereda de `ComponentActivity` pero carece del método `setContentView` y está configurada en el `AndroidManifest.xml` con el tema `@android:style/Theme.Translucent.NoTitleBar`.
   * *Por qué:* El usuario no requiere iterar sobre controles; solo necesita arrancar el servicio. Las aplicaciones que despliegan una UI para un solo botón generan fricción. Este patrón de trampolín solo se hace visible si los permisos faltan; si están concedidos, delega el trabajo al `Service` en cuestión de milisegundos y muere (`finish()`), liberando memoria de la pila visual.

2. **Foreground Service (`AudioGuardService.kt`)**:
   * *Decisión:* Se utilizó un servicio en primer plano (`startForeground()`) con el tipo de permiso `FOREGROUND_SERVICE_MEDIA_PLAYBACK` y flag `START_STICKY`.
   * *Por qué:* Desde Android 8.0 (Oreo), las optimizaciones de batería ("Doze Mode") destruyen cualquier proceso en segundo plano rápidamente. Al atarlo a una Notificación Persistente (`Ongoing`), el sistema operativo clasifica nuestra aplicación en una jerarquía de memoria casi inmatable. El flag `START_STICKY` instruye a Android a revivir el servicio si llegase a ser aniquilado por escasez de RAM extrema. Además, provee al usuario un "Control Remoto" vía un `PendingIntent` atado a un botón en la notificación para destruir el servicio y evitar pérdida de recursos cuando ya no necesite la app.

3. **Inyección Inversa de Eventos (`MediaRecoveryController.kt`)**:
   * *Decisión:* Se instancia el `AudioManager` y se utiliza un `Handler` anclado al `MainLooper` con un `postDelayed(50ms)` para inyectar eventos artificiales `KeyEvent.KEYCODE_MEDIA_PLAY` usando `dispatchMediaKeyEvent()`.
   * *Por qué:* En Android moderno, tomar control sobre el foco de audio (`AudioFocusRequest`) de otras aplicaciones (como Spotify) ha sido altamente restringido por seguridad. La forma infalible de reanudar el audio es simulando un click de hardware a través del `AudioManager`. 
   * *¿Por qué el retraso de 50ms?* Hay un problema de concurrencia temporal (Race Condition): el auricular detecta la notificación y ordena "pausa" al mismo tiempo que el celular registra la notificación. Si disparamos nuestro "Play" de forma inmediata, ocurrirá *antes* o *durante* la pausa del auricular. Al retrasarlo 50 milisegundos garantizamos que nuestro "Play" llegue inmediatamente **después** del pulso de pausa del hardware.

4. **El Interceptor (`NotificationMonitorService.kt`)**:
   * *Decisión:* Se extendió `NotificationListenerService`, implementando `onNotificationPosted`.
   * *Por qué:* Se requería una forma pasiva (que no drene batería mediante *polling*) para saber en qué momento exacto entra una notificación. `NotificationListenerService` es invocado nativamente por el Framework (System Server). Funciona como un canal directo. Su inicialización temprana en `onCreate()` permite tener el `MediaRecoveryController` instanciado y listo sin generar picos de procesamiento en tiempo de ejecución.

---

## Detalle Técnico de los Archivos Clave

### 1. `MainActivity.kt`
Opera mediante el contrato `registerForActivityResult`.
* Primero, en Android 13+ (`TIRAMISU`), solicita el permiso en tiempo real `POST_NOTIFICATIONS` necesario para poder dibujar la notificación de control del Foreground Service.
* Inmediatamente, la función `isNotificationListenerGranted` consulta mediante `NotificationManagerCompat` si el usuario ha habilitado en las configuraciones profundas la capacidad de la app para leer notificaciones. Si no lo está, lanza el `Intent` del sistema `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
* Si todo lo anterior está habilitado, invoca el servicio en segundo plano `AudioGuardService` y expulsa la actividad.

### 2. `AudioGuardService.kt`
Gestiona el ciclo de vida del *Supervisor*.
* Usa *Notification Channels* (requerido desde API 26+) configurando su nivel a `IMPORTANCE_LOW` para que no emita sonidos o aparezcan banners, actuando verdaderamente de forma silenciosa.
* Maneja un flujo bidireccional de control en el bloque `onStartCommand`: 
  1. Construye el Foreground Service en la entrada estándar.
  2. Mediante el uso del `ACTION_STOP_SERVICE`, atiende la llamada del propio usuario (cuando este toca "Detener" en la notificación), y ejecuta `stopForeground(STOP_FOREGROUND_REMOVE)` para limpiar su rastro y luego `stopSelf()` para matarse legalmente, liberando la memoria.

### 3. `NotificationMonitorService.kt`
El recolector pasivo de estado.
* Es una clase privilegiada que debe ser registrada en el `AndroidManifest.xml` con el permiso blindado `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` y con su filtro explícito `android.service.notification.NotificationListenerService`. Esto evita que aplicaciones de terceros puedan falsificar sus intents.
* En `onNotificationPosted()`, desenvuelve el paquete `StatusBarNotification (sbn)` interceptando el origen, lo cual abre a futuro la escalabilidad de implementar listas blancas o negras (ej. no pauso la música con un mensaje de WhatsApp, pero sí en una llamada).

### 4. `MediaRecoveryController.kt`
El manipulador del hardware auditivo.
* Actúa como fachada para la inyección de las teclas en el `AudioManager`. Emula físicamente lo que ocurre cuando presionas el botón central de tus manos libres mediante la instanciación de dos eventos consecutivos de hardware requeridos por el bus del sistema de Android:
  1. `ACTION_DOWN` (Botón presionado).
  2. `ACTION_UP` (Botón liberado).

---

## Instalación e Instrucciones de Uso

### Requisitos Previos:
- Un dispositivo Android con sistema operativo **Android 8.0 (API 26) o superior**.
- Habilitar la instalación de "Fuentes Desconocidas" en caso de instalar un APK generado.

### Instalación:
1. **Compilación (Si lo haces desde código fuente):**
   Abre el proyecto en Android Studio. Ejecuta un `Sync Project with Gradle Files`. Posteriormente ve al menú superior *Build* > *Build Bundle(s) / APK(s)* > *Build APK(s)*. Localiza tu archivo en la carpeta `/app/build/outputs/apk/debug/`.
2. **Transferencia e instalación:**
   Pasa el archivo `.apk` a tu dispositivo e instálalo normalmente.

### Uso y Configuración Inicial:
Debido a la naturaleza silenciosa de la aplicación y el uso de permisos especiales, requiere una inicialización de 3 pasos:

1. Busca el ícono de **Audio Guard** en tu panel de aplicaciones y tócalo.
2. La pantalla parecerá no hacer nada, pero emergerá un mensaje pidiendo autorización de envío de Notificaciones. **Acepta.**
3. En la gran mayoría de casos, de forma automática se abrirá una pantalla de Configuraciones del sistema llamada **"Acceso a Notificaciones"** o **"Dispositivos y apps"**. 
   * Localiza **Supresor de Interrupciones de Audio** o **Audio Guard** en la lista.
   * Enciende el interruptor para permitir que la app intercepte notificaciones y concédele el permiso en el diálogo de advertencia de Android.
4. Toca atrás o vuelve a tocar el ícono de la app.
5. Emergerá un mensaje estilo Toast en la parte inferior diciendo: `"AudioGuard activado en segundo plano"`. 
6. Si bajas la barra de estado de tu teléfono, verás una notificación silenciosa que dice: **Supresor de Interrupciones Activo**.

¡Listo! Ya puedes utilizar tus auriculares Bluetooth. Cuando entre una notificación, percibirás posiblemente una micro-pausa de una fracción de segundo, e inmediatamente la música o el audio del video se reanudará de manera automática sin que toques tus audífonos.

### ¿Cómo lo apago?
Si no quieres usar los audífonos y deseas apagar la aplicación, simplemente desliza tu barra de notificaciones, busca la notificación activa de "Audio Guard" y presiona el botón que dice **"Detener"**. Todo proceso en segundo plano morirá inmediatamente.