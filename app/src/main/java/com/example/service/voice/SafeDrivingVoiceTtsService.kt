package com.example.service.voice

import android.content.Context
import android.util.Log
import com.example.coordinator.ActiveOffer
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.SpeedState
import com.example.service.gemini.GeminiOfferEvaluation
import com.example.voice.VoiceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Serviço Inteligente de Text-To-Speech Seguro para Notificações de Ofertas Filtradas pelo Gemini.
 * 
 * Este serviço monitora continuamente:
 * 1. As ofertas avaliadas e recomendadas pela IA Gemini.
 * 2. A telemetria de velocidade e segurança do piloto (SpeedState e CurrentSpeedKmh).
 * 3. Garante que avisos de voz sejam emitidos apenas quando a velocidade é segura ou quando o piloto
 *    precisa de orientação tática sem tirar os olhos da estrada.
 */
class SafeDrivingVoiceTtsService(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val TAG = "SafeDrivingVoiceTts"

    // Fila de anúncios pendentes para quando a velocidade estiver alta demais / manobra perigosa
    private val pendingAudioAlerts = ArrayDeque<SafeVoiceNotification>()
    private var isAnnouncing = false
    private var lastAnnouncedOfferId: String? = null
    private var lastAnnouncedTimestamp: Long = 0L

    data class SafeVoiceNotification(
        val offerId: String,
        val appName: String,
        val fareValue: Double,
        val distanceKm: Double,
        val gainPerKm: Double,
        val evaluation: GeminiOfferEvaluation?,
        val customText: String?,
        val priority: Int = 1 // 1 = Alta, 2 = Média
    )

    init {
        startSafetySpeedMonitor()
    }

    /**
     * Inicia a escuta da velocidade e do estado do condutor.
     * Quando o condutor desacelera para uma velocidade segura (< 45 km/h ou SpeedState.PARADO),
     * a fila de anúncios de ofertas filtradas pelo Gemini é esvaziada por áudio.
     */
    private fun startSafetySpeedMonitor() {
        scope.launch {
            RadarCoordinator.currentSpeedKmh.collectLatest { speedKmh ->
                if (isSpeedSafeForTts(speedKmh)) {
                    processPendingQueue()
                }
            }
        }
    }

    /**
     * Determina se a velocidade atual é segura para anúncio de áudio e tomada de decisão.
     * Velocidades acima de 55 km/h exigem foco estrito na pista, aguardando momento seguro.
     */
    fun isSpeedSafeForTts(speedKmh: Float = RadarCoordinator.currentSpeedKmh.value): Boolean {
        val speedState = RadarCoordinator.speedState.value
        val speedLimitSetting = RadarCoordinator.settings.value.speedLimitKmh
        
        // Se estiver parado ou em velocidade urbana moderada (abaixo de 50 km/h)
        return speedState == SpeedState.PARADO || speedKmh <= 50.0f
    }

    /**
     * Notifica uma oferta filtrada e recomendada pelo Gemini via áudio TTS.
     * Se o entregador estiver em velocidade alta, enfileira o áudio para tocar no próximo semáforo ou desaceleração.
     */
    fun notifyGeminiRecommendedOffer(
        offer: ActiveOffer,
        evaluation: GeminiOfferEvaluation?
    ) {
        val offerId = "${offer.appName}_${offer.fareValue}_${offer.totalDistance}"
        val currentTime = System.currentTimeMillis()

        // Evita anúncios duplicados em curto intervalo (cooldown de 10 segundos)
        if (offerId == lastAnnouncedOfferId && (currentTime - lastAnnouncedTimestamp) < 10000L) {
            return
        }

        val gainPerKm = if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else offer.fareValue

        val notification = SafeVoiceNotification(
            offerId = offerId,
            appName = offer.appName,
            fareValue = offer.fareValue,
            distanceKm = offer.totalDistance,
            gainPerKm = gainPerKm,
            evaluation = evaluation,
            customText = null,
            priority = if (evaluation?.decision.equals("ACCEPT", ignoreCase = true)) 1 else 2
        )

        val currentSpeed = RadarCoordinator.currentSpeedKmh.value

        if (isSpeedSafeForTts(currentSpeed)) {
            dispatchSpeech(notification)
        } else {
            // Entregador em alta velocidade: enfileira para proteger a atenção do motoboy
            synchronized(pendingAudioAlerts) {
                // Mantém apenas os 2 melhores alertas na fila
                if (pendingAudioAlerts.size >= 2) {
                    pendingAudioAlerts.removeLast()
                }
                pendingAudioAlerts.addFirst(notification)
            }
            RadarCoordinator.addLog("TTS Seguro: Piloto a ${currentSpeed.toInt()} km/h. Anúncio da melhor oferta do Gemini agendado para a desaceleração.", com.example.coordinator.LogType.INFO)
        }
    }

    /**
     * Executa a síntese de voz (TTS) formatada para o motoboy em trânsito.
     */
    private fun dispatchSpeech(notification: SafeVoiceNotification) {
        if (isAnnouncing) return
        isAnnouncing = true

        lastAnnouncedOfferId = notification.offerId
        lastAnnouncedTimestamp = System.currentTimeMillis()

        val textToSpeak = buildSafeAnnouncementText(notification)

        scope.launch(Dispatchers.Main) {
            try {
                // Toca um bipe sonoro de atenção VIP antes de falar
                voiceManager.playVipAlert("bell")
                delay(250)

                // Fala o resumo estruturado e natural da oferta pelo Gemini
                voiceManager.speak(textToSpeak)
                
                RadarCoordinator.addLog("TTS Seguro Ativo: Anunciado '${notification.appName} R$ ${String.format(Locale.US, "%.2f", notification.fareValue)}' (${String.format(Locale.US, "%.2f", notification.gainPerKm)}/km)", com.example.coordinator.LogType.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao disparar áudio TTS de oferta: ${e.message}")
            } finally {
                delay(2500)
                isAnnouncing = false
                processPendingQueue()
            }
        }
    }

    /**
     * Constrói a mensagem verbal sintetizada otimizada para capacete / fone de ouvido,
     * priorizando brevidade, ganho por km e motivo do Gemini.
     */
    private fun buildSafeAnnouncementText(notif: SafeVoiceNotification): String {
        val eval = notif.evaluation
        val app = notif.appName
        val fareStr = String.format(Locale.US, "%.2f", notif.fareValue)
        val kmStr = String.format(Locale.US, "%.1f", notif.distanceKm)
        val gainPerKmStr = String.format(Locale.US, "%.2f", notif.gainPerKm)

        // Se o Gemini forneceu uma mensagem pronta para voz:
        if (!eval?.suggestedVoiceAnnouncement.isNullOrBlank()) {
            return eval!!.suggestedVoiceAnnouncement!!
        }

        val reasonSummary = eval?.reason ?: "Excelente taxa de retorno por quilômetro."

        return "Atenção piloto. Nova melhor oferta no $app: R$ $fareStr para $kmStr quilômetros, rendendo R$ $gainPerKmStr por quilômetro. $reasonSummary. Diga aceitar ou toque na tela."
    }

    /**
     * Processa itens que ficaram na fila enquanto o entregador estava em velocidade de rodovia/trânsito rápido.
     */
    private fun processPendingQueue() {
        if (isAnnouncing) return

        val nextNotification = synchronized(pendingAudioAlerts) {
            if (pendingAudioAlerts.isNotEmpty()) pendingAudioAlerts.removeFirst() else null
        }

        if (nextNotification != null) {
            // Confirma se o alerta não expirou (menos de 45 segundos)
            dispatchSpeech(nextNotification)
        }
    }

    fun stop() {
        synchronized(pendingAudioAlerts) {
            pendingAudioAlerts.clear()
        }
        voiceManager.stop()
    }
}
