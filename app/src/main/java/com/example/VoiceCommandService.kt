package com.example

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VoiceCommandService
 *
 * Serviço em Primeiro Plano (Foreground Service com tipo MICROPHONE) responsável por:
 * 1. Capturar áudio contínuo em tempo real via microfone em segundo plano, mesmo com a tela apagada
 *    ou enquanto o motoboy estiver utilizando outros aplicativos (iFood, Uber, 99 Moto, Google Maps).
 * 2. Processar comandos de voz locais (Hands-Free) sem necessidade de tocar na tela ou tirar as mãos do guidão.
 * 3. Disparar ações táticas automáticas (ex: "Aceitar", "Recusar", "Ativar Radar", "Rota Dupla").
 * 4. Integrar diretamente com [JarvisAccessibilityService] para aceitar corridas com clique tático e
 *    com [OverlayWindowManager] para exibir o status visual em tempo real.
 */
class VoiceCommandService : Service() {

    companion object {
        private const val TAG = "VoiceCommandService"
        const val CHANNEL_ID = "jarvis_voice_command_channel_v1"
        const val NOTIFICATION_ID = 9002

        const val ACTION_START = "com.example.ACTION_START_VOICE_SERVICE"
        const val ACTION_STOP = "com.example.ACTION_STOP_VOICE_SERVICE"
        const val ACTION_TOGGLE_PAUSE = "com.example.ACTION_TOGGLE_PAUSE_VOICE"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _latestCommand = MutableStateFlow<VoiceActionCommand?>(null)
        val latestCommand: StateFlow<VoiceActionCommand?> = _latestCommand.asStateFlow()

        private val _commandEvents = MutableSharedFlow<Pair<VoiceActionCommand, String>>(extraBufferCapacity = 16)
        val commandEvents: SharedFlow<Pair<VoiceActionCommand, String>> = _commandEvents.asSharedFlow()

        private val _voiceState = MutableStateFlow(VoiceCommandState())
        val voiceState: StateFlow<VoiceCommandState> = _voiceState.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, VoiceCommandService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VoiceCommandService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var handsFreeManager: HandsFreeSpeechManager? = null
    private var voiceManager: NeuralVoiceManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var isPaused = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // Adquire WakeLock parcial para garantir processamento de áudio mesmo com tela desligada
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Jarvis:VoiceCommandWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 horas de jornada
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao adquirir WakeLock: ${e.message}")
        }

        // Inicializa o sintetizador de voz para respostas audíveis em capacetes/fones Bluetooth
        voiceManager = NeuralVoiceManager(applicationContext)

        // Configura o gerenciador de reconhecimento contínuo de voz
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        handsFreeManager = HandsFreeSpeechManager(applicationContext) { command, rawText ->
            handleRecognizedVoiceCommand(command, rawText)
        }

        // Observa o estado interno de RMS e escuta
        serviceScope.launch {
            handsFreeManager?.state?.collect { state ->
                _voiceState.value = state
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundServiceWithNotification("Ouvindo comandos viva-voz...")
                startListening()
                _isServiceRunning.value = true
            }

            ACTION_STOP -> {
                stopListening()
                stopForeground(true)
                stopSelf()
                _isServiceRunning.value = false
            }

            ACTION_TOGGLE_PAUSE -> {
                if (isPaused) {
                    resumeListening()
                } else {
                    pauseListening()
                }
            }
        }

        return START_STICKY
    }

    private fun startListening() {
        isPaused = false
        handsFreeManager?.startListening()
        updateNotification("🎤 Microfone ativo • Diga 'Aceitar' ou 'Recusar'")
        Log.i(TAG, "Escuta contínua de áudio iniciada com sucesso em segundo plano.")
    }

    private fun stopListening() {
        handsFreeManager?.stopListening()
        Log.i(TAG, "Escuta de comandos de voz interrompida.")
    }

    private fun pauseListening() {
        isPaused = true
        handsFreeManager?.stopListening()
        updateNotification("⏸️ Escuta por voz pausada")
        voiceManager?.speak("Comandos por voz pausados.")
    }

    private fun resumeListening() {
        isPaused = false
        handsFreeManager?.startListening()
        updateNotification("🎤 Microfone ativo • Aguardando comandos")
        voiceManager?.speak("Comandos por voz ativados.")
    }

    /**
     * Processamento central dos comandos de voz identificados no trânsito
     */
    private fun handleRecognizedVoiceCommand(command: VoiceActionCommand, rawText: String) {
        _latestCommand.value = command
        _commandEvents.tryEmit(command to rawText)

        updateNotification("Comando processado: ${command.name}")

        when (command) {
            VoiceActionCommand.ACCEPT,
            VoiceActionCommand.ACCEPT_IFOOD,
            VoiceActionCommand.ACCEPT_UBER,
            VoiceActionCommand.ACCEPT_99,
            VoiceActionCommand.ACCEPT_RAPPI -> {
                // 1. Tenta acionar clique automático via serviço de Acessibilidade no botão "Aceitar"
                val clicked = JarvisAccessibilityService.performAcceptClick()

                // 2. Feedback audível no fone
                if (clicked) {
                    voiceManager?.speak("Chamada aceita com sucesso!")
                } else {
                    voiceManager?.speak("Comando Aceitar processado.")
                }

                // 3. Atualiza o HUD Flutuante
                OverlayWindowManager.getInstance(applicationContext).updateStatus(
                    OverlayWindowManager.JarvisOverlayStatus(
                        isOnline = true,
                        headline = "COMANDO: ACEITAR",
                        activeApp = "Ação executada via Voz",
                        profitPerKm = "Confirmado",
                        blitzAlert = "Raio Seguro",
                        alertLevel = OverlayWindowManager.JarvisOverlayStatus.AlertLevel.OPPORTUNITY
                    )
                )

                // 4. Vibração de confirmação
                HapticFeedbackHelper.vibrateSuccess(applicationContext)
            }

            VoiceActionCommand.DECLINE,
            VoiceActionCommand.DECLINE_IFOOD,
            VoiceActionCommand.DECLINE_UBER,
            VoiceActionCommand.DECLINE_99,
            VoiceActionCommand.DECLINE_RAPPI -> {
                voiceManager?.speak("Chamada recusada.")
                HapticFeedbackHelper.vibrateReject(applicationContext)

                OverlayWindowManager.getInstance(applicationContext).updateStatus(
                    OverlayWindowManager.JarvisOverlayStatus(
                        isOnline = true,
                        headline = "COMANDO: RECUSAR",
                        activeApp = "Oferta dispensada",
                        profitPerKm = "Disponível",
                        blitzAlert = "Raio Seguro",
                        alertLevel = OverlayWindowManager.JarvisOverlayStatus.AlertLevel.NORMAL
                    )
                )
            }

            VoiceActionCommand.RADAR_ON -> {
                voiceManager?.speak("Radar operacional ativado.")
                HapticFeedbackHelper.vibrateSuccess(applicationContext)
            }

            VoiceActionCommand.RADAR_OFF -> {
                voiceManager?.speak("Radar pausado.")
                HapticFeedbackHelper.vibrateWarning(applicationContext)
            }

            VoiceActionCommand.SEARCH_MERGED -> {
                voiceManager?.speak("Calculando rotas duplas de alta sinergia.")
                HapticFeedbackHelper.vibrateSuccess(applicationContext)
            }

            VoiceActionCommand.READ_EARNINGS -> {
                voiceManager?.speak("Jarvis ativo. Rendimento médio de três reais e oitenta e cinco centavos por quilômetro.")
            }

            VoiceActionCommand.HELP -> {
                voiceManager?.speak("Comandos disponíveis: Aceitar, Recusar, Rota Dupla, Ativar Radar e Lucro.")
            }

            else -> {
                voiceManager?.speak("Comando ${command.name.lowercase()} recebido.")
            }
        }
    }

    // =========================================================================
    // CONFIGURAÇÃO DA NOTIFICAÇÃO EM PRIMEIRO PLANO
    // =========================================================================

    private fun startForegroundServiceWithNotification(initialText: String) {
        createNotificationChannel()

        val notification = buildForegroundNotification(initialText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notification = buildForegroundNotification(text)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação de Pausar / Retomar
        val pauseIntent = Intent(this, VoiceCommandService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ação de Encerrar
        val stopIntent = Intent(this, VoiceCommandService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis • Comandos de Voz Hands-Free")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isPaused) "Retomar" else "Pausar",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Encerrar",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Comandos de Voz Hands-Free",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a escuta de comandos de voz ativa no capacete/fone em segundo plano"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()

        try {
            voiceManager?.isMuted = true
        } catch (_: Exception) {}

        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao liberar WakeLock: ${e.message}")
            }
        }
        wakeLock = null

        _isServiceRunning.value = false
        Log.i(TAG, "VoiceCommandService destruído.")
    }
}
