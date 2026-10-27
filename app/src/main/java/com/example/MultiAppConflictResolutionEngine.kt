package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * MultiAppConflictResolutionEngine
 *
 * Motor de resolução de conflitos e cancelamentos em operações multi-apps.
 *
 * 1. Race Condition ("Outro motoboy aceitou antes"):
 *    Desmonta o stack com segurança, mantém a primeira corrida ativa e
 *    recalcula a rota individual sem desvios, notificando o piloto por voz.
 *
 * 2. Cancelamento em Trânsito ("Cliente ou restaurante cancelou"):
 *    Detecta a perda, calcula a taxa de deslocamento estimada e suspende
 *    a alteração do GPS até que o piloto confirme manualmente na tela.
 */
object MultiAppConflictResolutionEngine {

    private const val TAG = "ConflictResolution"

    data class ActiveCancellationIncident(
        val incidentId: String,
        val cancelledAppName: String,
        val cancelledRestaurant: String,
        val estimatedDisplacementFeeBrl: Double,
        val remainingAppName: String,
        val remainingRestaurant: String,
        val remainingPayoutBrl: Double,
        val recalculatedKm: Double,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ConflictResolutionStatus(
        val lastResolutionMessage: String = "Monitor de conflitos ativo e em prontidão",
        val isAwaitingDriverConfirmation: Boolean = false,
        val activeIncident: ActiveCancellationIncident? = null,
        val totalConflictsResolved: Int = 0,
        val recoveredRevenueBrl: Double = 0.0
    )

    private val _engineState = MutableStateFlow(ConflictResolutionStatus())
    val engineState: StateFlow<ConflictResolutionStatus> = _engineState.asStateFlow()

    /**
     * CENÁRIO 1: O segundo aplicativo expirou ou outro entregador aceitou antes.
     */
    fun handleOfferLostRaceCondition(
        context: Context,
        lostAppName: String = "99 Food",
        remainingOffer: RadarOffer = RadarOffer(
            id = "ifood_active_stay",
            appName = "iFood",
            restaurant = "Outback Shopping Paulista",
            value = 24.50,
            distanceKm = 3.5,
            gainPerKm = 7.00
        )
    ) {
        val count = _engineState.value.totalConflictsResolved + 1
        val message = "Oferta da $lostAppName aceita por outro entregador. Stack desfeito: mantendo apenas ${remainingOffer.appName} (${remainingOffer.restaurant}) com rota direta de ${remainingOffer.distanceKm} km."

        _engineState.value = _engineState.value.copy(
            lastResolutionMessage = message,
            totalConflictsResolved = count
        )

        Log.w(TAG, "⚡ [RACE CONDITION MITIGADA] $message")
        HapticFeedbackHelper.vibrateDecline(context)

        try {
            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Atenção: o pedido da $lostAppName foi aceito por outro motoboy. Mantendo apenas o ${remainingOffer.appName} com rota direta de ${remainingOffer.distanceKm} quilômetros."
            )
        } catch (_: Exception) {}
    }

    /**
     * CENÁRIO 2: O cliente cancelou o pedido enquanto o motoboy já estava em trânsito.
     */
    fun handleInTransitCancellation(
        context: Context,
        cancelledAppName: String = "99 Food",
        cancelledRestaurant: String = "Burger King Shopping Paulista",
        remainingOffer: RadarOffer = RadarOffer(
            id = "ifood_active_stay",
            appName = "iFood",
            restaurant = "Outback Shopping Paulista",
            value = 24.50,
            distanceKm = 3.5,
            gainPerKm = 7.00
        )
    ) {
        val incident = ActiveCancellationIncident(
            incidentId = "inc_${System.currentTimeMillis() % 100000}",
            cancelledAppName = cancelledAppName,
            cancelledRestaurant = cancelledRestaurant,
            estimatedDisplacementFeeBrl = 7.50,
            remainingAppName = remainingOffer.appName,
            remainingRestaurant = remainingOffer.restaurant,
            remainingPayoutBrl = remainingOffer.value,
            recalculatedKm = remainingOffer.distanceKm
        )

        _engineState.value = _engineState.value.copy(
            isAwaitingDriverConfirmation = true,
            activeIncident = incident,
            lastResolutionMessage = "Cancelamento detectado em trânsito: aguardando confirmação do piloto na tela."
        )

        Log.w(TAG, "🚨 [CANCELAMENTO EM TRÂNSITO] $cancelledAppName cancelado. Aguardando confirmação.")
        HapticFeedbackHelper.vibrateDecline(context)

        try {
            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Atenção piloto: Pedido da $cancelledAppName foi cancelado pelo cliente. Verifique a tela para recalcular a rota no GPS."
            )
        } catch (_: Exception) {}
    }

    /**
     * Confirmação manual pelo motoboy para atualizar o GPS para a entrega ativa remanescente.
     */
    fun confirmRecalculation(context: Context) {
        val incident = _engineState.value.activeIncident
        val count = _engineState.value.totalConflictsResolved + 1
        val fee = incident?.estimatedDisplacementFeeBrl ?: 0.0
        val totalRecovered = _engineState.value.recoveredRevenueBrl + fee

        val message = if (incident != null) {
            "GPS recalculado exclusivamente para ${incident.remainingAppName} (${incident.recalculatedKm} km). Taxa de deslocamento de R$ 7,50 registrada para solicitação."
        } else {
            "Rota recalibrada com sucesso."
        }

        _engineState.value = _engineState.value.copy(
            isAwaitingDriverConfirmation = false,
            activeIncident = null,
            lastResolutionMessage = message,
            totalConflictsResolved = count,
            recoveredRevenueBrl = totalRecovered
        )

        HapticFeedbackHelper.vibrateSuccess(context)
        try {
            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Destino atualizado. Seguindo para entrega do ${incident?.remainingAppName ?: "pedido ativo"}."
            )
        } catch (_: Exception) {}
    }

    /**
     * O motoboy optou por manter a rota atual sem trocar o destino no GPS.
     */
    fun dismissWithoutRecalculation() {
        _engineState.value = _engineState.value.copy(
            isAwaitingDriverConfirmation = false,
            activeIncident = null,
            lastResolutionMessage = "Rota atual mantida pelo entregador."
        )
    }
}
