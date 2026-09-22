package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Despachador de Ofertas em Segundo Plano.
 * Mantém o loop de interceptação de corridas ativo mesmo quando o aplicativo
 * estiver minimizado, na tela inicial ou em segundo plano (ex: piloto no Waze ou com tela apagada).
 * Dispara notificações push de alta prioridade com sons personalizados.
 */
object BackgroundOfferDispatcher {
    private const val TAG = "BackgroundOfferDispatcher"

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var dispatchJob: Job? = null

    @Volatile
    var isAppInBackground: Boolean = false

    @Volatile
    var isTrackingActive: Boolean = false

    @Volatile
    var isAutoDispatchActive: Boolean = true

    @Volatile
    var currentFilterCriteria: OfferFilterCriteria = OfferFilterCriteria()

    private val _latestOffers = MutableStateFlow<List<RadarOffer>>(emptyList())
    val latestOffers: StateFlow<List<RadarOffer>> = _latestOffers.asStateFlow()

    private var onNewOfferListener: ((RadarOffer) -> Unit)? = null

    fun setOnNewOfferListener(listener: ((RadarOffer) -> Unit)?) {
        onNewOfferListener = listener
    }

    /**
     * Inicia o loop de monitoramento de ofertas em segundo plano.
     */
    fun startMonitoring(context: Context, localNotificationManager: LocalNotificationManager) {
        if (dispatchJob?.isActive == true) return

        // 1. Escuta notificações reais interceptadas de apps de entrega (iFood, Uber, Rappi, 99)
        scope.launch {
            AppNotificationListenerService.lastInterceptedOffer.collect { interceptedOffer ->
                if (interceptedOffer != null) {
                    processIncomingOffer(context, localNotificationManager, interceptedOffer)
                }
            }
        }

        dispatchJob = scope.launch {
            Log.d(TAG, "Iniciando monitoramento contínuo de ofertas em segundo plano...")
            while (isActive) {
                delay(13000L) // Varredura periódica a cada 13 segundos

                if (!isTrackingActive || !isAutoDispatchActive) {
                    continue
                }

                val newOffer = LiveDispatchSimulator.generateNextOffer()
                processIncomingOffer(context, localNotificationManager, newOffer)
            }
        }
    }

    private fun processIncomingOffer(
        context: Context,
        localNotificationManager: LocalNotificationManager,
        offer: RadarOffer
    ) {
        val speed = LocationService.globalLocationState.value.currentSpeedKmh
        val isSafetyLocked = LocationService.globalLocationState.value.isSafetyLockActive || speed > 10.0

        val currentList = _latestOffers.value.toMutableList()
        if (currentList.none { it.id == offer.id }) {
            currentList.add(0, offer)
            if (currentList.size > 8) {
                currentList.removeAt(currentList.lastIndex)
            }
            _latestOffers.value = currentList
        }

        // Notifica listener da UI se registrado
        onNewOfferListener?.invoke(offer)

        // Se o app estiver em segundo plano e a velocidade estiver segura (<= 10 km/h):
        if (isAppInBackground && !isSafetyLocked) {
            val criteria = currentFilterCriteria
            // Verifica se a notificação passa pelo filtro configurado
            if (criteria.isNotificationFilterEnabled) {
                val passes = NotificationFilterManager.evaluateAndRecord(offer, criteria)
                if (passes) {
                    localNotificationManager.showHighPriorityOfferNotification(offer, criteria)
                    Log.d(TAG, "Notificação push de alta prioridade emitida em 2º plano: ${offer.restaurant} (${offer.gainPerKm} R$/km)")
                } else {
                    Log.d(TAG, "Oferta em 2º plano filtrada: ${offer.gainPerKm} R$/km < piso")
                }
            } else {
                localNotificationManager.showHighPriorityOfferNotification(offer, criteria)
            }
        }
    }

    fun stopMonitoring() {
        dispatchJob?.cancel()
        dispatchJob = null
        Log.d(TAG, "Monitoramento de segundo plano interrompido.")
    }
}
