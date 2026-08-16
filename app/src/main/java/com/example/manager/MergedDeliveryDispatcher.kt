package com.example.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.LogType
import com.example.model.ActiveMergedRouteState
import com.example.model.MergedDeliveryStop
import com.example.model.StopActionType
import com.example.model.StopExecutionStatus
import com.example.data.GhostSequenceBatchResult
import com.example.data.GhostRouteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * Gerenciador Central de Rotas Mescladas & Itinerário Tático Passo a Passo (Merged Delivery Dispatcher).
 *
 * Responsável por:
 * 1. Transformar ofertas simultâneas/lotes Ghost em um plano de ação unificado.
 * 2. Guiar o motoboy passo a passo: O que coletar primeiro, qual app, código do pedido, dicas de entrega.
 * 3. Integrar com Waze e Google Maps em 1 toque para o endereço exato da etapa atual.
 * 4. Controlar o avanço de etapas (Confirmar Coleta -> Partir para Entrega -> Finalizar).
 * 5. Persistência e cache local 100% offline via Room Database (SQLite) para garantir acesso a detalhes mesmo sem sinal.
 */
object MergedDeliveryDispatcher {
    private const val TAG = "MergedDeliveryDispatcher"

    private val dispatcherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeRoute = MutableStateFlow<ActiveMergedRouteState?>(null)
    val activeRoute: StateFlow<ActiveMergedRouteState?> = _activeRoute.asStateFlow()

    private val _guidanceAudioTip = MutableStateFlow<String?>(null)
    val guidanceAudioTip: StateFlow<String?> = _guidanceAudioTip.asStateFlow()

    private val _isOfflineCached = MutableStateFlow(true)
    val isOfflineCached: StateFlow<Boolean> = _isOfflineCached.asStateFlow()

    init {
        // Inicializar com rota mesclada demonstrativa inteligente
        createDemoMergedRoute()
    }

    /**
     * Inicializa o motor de persistência Room e recupera rota ativa salva em caso de reinício ou perda de sinal
     */
    fun initializeWithCache(context: Context) {
        dispatcherScope.launch {
            try {
                val repository = GhostRouteRepository.getInstance(context)
                val cachedActiveRoute = repository.getActiveRouteDirect()
                if (cachedActiveRoute != null && cachedActiveRoute.stops.isNotEmpty()) {
                    _activeRoute.value = cachedActiveRoute
                    _isOfflineCached.value = true
                    Log.i(TAG, "Rota Ghost recuperada com sucesso do cache Room SQLite offline: ${cachedActiveRoute.batchId}")
                } else {
                    // Garante que a rota padrão fique salva em cache no Room
                    _activeRoute.value?.let { defaultRoute ->
                        repository.saveActiveRoute(defaultRoute)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso ao carregar cache Room de rotas Ghost: ${e.message}")
            }
        }
    }

    /**
     * Cria uma rota mesclada tática de demonstração para motoboys
     */
    fun createDemoMergedRoute() {
        val stops = listOf(
            MergedDeliveryStop(
                id = UUID.randomUUID().toString(),
                appName = "iFood",
                actionType = StopActionType.PICKUP,
                status = StopExecutionStatus.NEXT,
                establishmentOrCustomer = "Burger King (Shopping Paulista)",
                fullAddress = "Av. Paulista, 1230 - Loja 402, Bela Vista",
                latitude = -23.5645,
                longitude = -46.6521,
                orderCode = "#4920",
                customerNotes = "Retirar no balcão de delivery com código",
                itemsSummary = "2x Whopper + 1x Batata Média + Refri",
                fareShare = 14.50,
                estimatedWaitOrPrepMinutes = 2,
                sequenceNumber = 1,
                tacticalGuidance = "Pegue primeiro aqui! O pedido já está na esteira de embalagem."
            ),
            MergedDeliveryStop(
                id = UUID.randomUUID().toString(),
                appName = "Rappi",
                actionType = StopActionType.PICKUP,
                status = StopExecutionStatus.PENDING,
                establishmentOrCustomer = "Pizza Hut (Brigadeiro)",
                fullAddress = "Av. Brigadeiro Luís Antônio, 2200, Bela Vista",
                latitude = -23.5670,
                longitude = -46.6490,
                orderCode = "RAP-8812",
                customerNotes = "Entrar pela lateral de motos",
                itemsSummary = "1x Pizza Pan Grande Pepperoni",
                fareShare = 18.50,
                estimatedWaitOrPrepMinutes = 4,
                sequenceNumber = 2,
                tacticalGuidance = "Fica a apenas 400m do BK. Passe logo em seguida enquanto a pizza sai do forno."
            ),
            MergedDeliveryStop(
                id = UUID.randomUUID().toString(),
                appName = "iFood",
                actionType = StopActionType.DELIVERY,
                status = StopExecutionStatus.PENDING,
                establishmentOrCustomer = "Cliente Mariana Santos (Apto 84)",
                fullAddress = "Rua Cincinato Braga, 450, Bela Vista",
                latitude = -23.5685,
                longitude = -46.6470,
                orderCode = "#4920",
                customerNotes = "Portaria 24h libera subida ou deixa com porteiro Valdir",
                itemsSummary = "Entrega do BK",
                fareShare = 0.0,
                estimatedWaitOrPrepMinutes = 0,
                sequenceNumber = 3,
                tacticalGuidance = "Primeira entrega da rota! Fica no caminho da segunda entrega."
            ),
            MergedDeliveryStop(
                id = UUID.randomUUID().toString(),
                appName = "Rappi",
                actionType = StopActionType.DELIVERY,
                status = StopExecutionStatus.PENDING,
                establishmentOrCustomer = "Cliente Roberto Rocha (Conj. 1201)",
                fullAddress = "Alameda Santos, 1800, Cerqueira César",
                latitude = -23.5590,
                longitude = -46.6580,
                orderCode = "RAP-8812",
                customerNotes = "Interfone 1201 ou WhatsApp ao chegar",
                itemsSummary = "Entrega da Pizza Hut",
                fareShare = 0.0,
                estimatedWaitOrPrepMinutes = 0,
                sequenceNumber = 4,
                tacticalGuidance = "Finalização do lote! Rota limpa pela Alameda Santos."
            )
        )

        val demoRoute = ActiveMergedRouteState(
            batchId = "BATCH-GHOST-" + System.currentTimeMillis() % 10000,
            title = "Combo Inteligente iFood + Rappi (Bela Vista)",
            appsInvolved = listOf("iFood", "Rappi"),
            totalEarnings = 33.00,
            totalDistanceKm = 4.2,
            totalEstimatedMinutes = 24,
            waitTimeSavedMinutes = 14,
            stops = stops,
            currentStopIndex = 0,
            isRouteActive = false,
            startedTimestamp = 0L,
            completedStopsCount = 0
        )
        _activeRoute.value = demoRoute
    }

    /**
     * Carrega e inicia um lote calculado pela Ghost Sequence salvando instantaneamente no Room
     */
    fun startGhostBatchRoute(context: Context, batch: GhostSequenceBatchResult) {
        val stopsList = mutableListOf<MergedDeliveryStop>()

        // Constrói paradas baseadas nas coletas e entregas do lote
        batch.stopsInOptimizedOrder.forEachIndexed { index, stop ->
            val isPickup = stop.priority <= 2
            val actionType = if (isPickup) StopActionType.PICKUP else StopActionType.DELIVERY
            val app = if (index % 2 == 0) "iFood" else "Rappi"

            stopsList.add(
                MergedDeliveryStop(
                    id = stop.id,
                    offerBatchId = batch.batchId,
                    appName = app,
                    actionType = actionType,
                    status = if (index == 0) StopExecutionStatus.NEXT else StopExecutionStatus.PENDING,
                    establishmentOrCustomer = if (isPickup) "Coleta ${stop.address.split(",").firstOrNull() ?: "Restaurante"}" else "Entrega Cliente",
                    fullAddress = stop.address,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    orderCode = "#${1000 + index * 37}",
                    customerNotes = if (isPickup) "Retirar na esteira delivery" else "Tocar campainha / Portaria",
                    itemsSummary = if (isPickup) "Pacote selado" else "Entrega",
                    fareShare = if (isPickup) (batch.totalEarnings / 2.0) else 0.0,
                    estimatedWaitOrPrepMinutes = if (isPickup) 3 else 0,
                    sequenceNumber = index + 1,
                    tacticalGuidance = if (isPickup) "Coleta sincronizada no corredor" else "Destino otimizado sem desvio de rota"
                )
            )
        }

        val newRoute = ActiveMergedRouteState(
            batchId = batch.batchId,
            title = "Lote Mesclado: ${batch.appNamesFormatted}",
            appsInvolved = listOf("iFood", "Rappi"),
            totalEarnings = batch.totalEarnings,
            totalDistanceKm = batch.gainPerKm,
            totalEstimatedMinutes = batch.estimatedTimeMinutes,
            waitTimeSavedMinutes = batch.waitTimeSavedMinutes,
            stops = stopsList,
            currentStopIndex = 0,
            isRouteActive = true,
            startedTimestamp = System.currentTimeMillis(),
            completedStopsCount = 0
        )

        _activeRoute.value = newRoute
        _isOfflineCached.value = true
        RadarCoordinator.addLog("🎯 ROTA MESCLADA INICIADA & SALVA OFFLINE: ${newRoute.title} (R$ ${String.format(Locale.US, "%.2f", newRoute.totalEarnings)})", LogType.SUCCESS)

        // Persistência imediata no Room SQLite
        dispatcherScope.launch {
            GhostRouteRepository.getInstance(context).saveActiveRoute(newRoute)
        }

        // Aciona Waze / Maps na primeira parada automaticamente
        newRoute.currentStop?.let { firstStop ->
            launchNavigation(context, firstStop.fullAddress, firstStop.latitude, firstStop.longitude)
        }
    }

    /**
     * Inicia a execução da rota mesclada atual e persiste o estado ativo
     */
    fun activateCurrentRoute(context: Context) {
        val current = _activeRoute.value ?: return
        val updated = current.copy(
            isRouteActive = true,
            startedTimestamp = System.currentTimeMillis()
        )
        _activeRoute.value = updated
        _isOfflineCached.value = true
        RadarCoordinator.addLog("🚀 Cockpit: Rota Mesclada colocada em execução ativa (Cache Room Sincronizado).", LogType.SUCCESS)

        dispatcherScope.launch {
            GhostRouteRepository.getInstance(context).saveActiveRoute(updated)
        }

        updated.currentStop?.let { stop ->
            launchNavigation(context, stop.fullAddress, stop.latitude, stop.longitude)
        }
    }

    /**
     * Avança para a próxima etapa/parada da rota mesclada e sincroniza com o Room SQLite
     */
    fun completeCurrentStopAndAdvance(context: Context) {
        val current = _activeRoute.value ?: return
        val currentIndex = current.currentStopIndex
        val currentStops = current.stops.toMutableList()

        if (currentIndex < currentStops.size) {
            // Marca a atual como concluída
            val finishedStop = currentStops[currentIndex].copy(status = StopExecutionStatus.COMPLETED)
            currentStops[currentIndex] = finishedStop

            val nextIndex = currentIndex + 1
            if (nextIndex < currentStops.size) {
                // Marca a próxima como NEXT
                currentStops[nextIndex] = currentStops[nextIndex].copy(status = StopExecutionStatus.NEXT)
                val updated = current.copy(
                    stops = currentStops,
                    currentStopIndex = nextIndex,
                    completedStopsCount = current.completedStopsCount + 1
                )
                _activeRoute.value = updated

                val nextStop = currentStops[nextIndex]
                val actionVerb = if (nextStop.actionType == StopActionType.PICKUP) "Coletar no" else "Entregar para"
                val guidance = "✅ Etapa concluída! Próximo passo: $actionVerb ${nextStop.establishmentOrCustomer} (${nextStop.appName})."

                RadarCoordinator.addLog(guidance, LogType.SUCCESS)
                _guidanceAudioTip.value = guidance

                // Sincroniza progresso com o Room
                dispatcherScope.launch {
                    val repo = GhostRouteRepository.getInstance(context)
                    repo.updateRouteProgress(
                        batchId = current.batchId,
                        currentStopIndex = nextIndex,
                        completedStopsCount = updated.completedStopsCount,
                        isRouteActive = true,
                        updatedStop = finishedStop
                    )
                }

                // Dispara Waze/GPS automaticamente para o próximo endereço
                launchNavigation(context, nextStop.fullAddress, nextStop.latitude, nextStop.longitude)
            } else {
                // Todas as paradas finalizadas!
                val updated = current.copy(
                    stops = currentStops,
                    isRouteActive = false,
                    completedStopsCount = currentStops.size
                )
                _activeRoute.value = updated

                RadarCoordinator.addLog("🏆 LOTE MESCLADO 100% CONCLUÍDO! Faturamento de R$ ${String.format(Locale.US, "%.2f", current.totalEarnings)} creditado!", LogType.SUCCESS)
                RadarCoordinator.completeActiveDelivery()

                // Atualiza finalização no Room
                dispatcherScope.launch {
                    val repo = GhostRouteRepository.getInstance(context)
                    repo.updateRouteProgress(
                        batchId = current.batchId,
                        currentStopIndex = currentIndex,
                        completedStopsCount = currentStops.size,
                        isRouteActive = false,
                        updatedStop = finishedStop
                    )
                }
            }
        }
    }

    /**
     * Abre Waze ou Google Maps diretamente com o endereço/coordenadas da parada
     */
    fun launchNavigation(context: Context, address: String, lat: Double, lng: Double) {
        val settings = RadarCoordinator.settings.value
        val defaultNav = settings.defaultNavigationApp.lowercase()

        try {
            if (defaultNav == "waze") {
                val wazeUri = if (lat != 0.0 && lng != 0.0) {
                    Uri.parse("waze://?ll=$lat,$lng&navigate=yes")
                } else {
                    Uri.parse("waze://?q=" + Uri.encode(address))
                }
                val mapIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                    return
                }
            }

            // Fallback para Google Maps
            val gmmIntentUri = if (lat != 0.0 && lng != 0.0) {
                Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            } else {
                Uri.parse("google.navigation:q=" + Uri.encode(address) + "&mode=d")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback web browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(address))).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir navegação para $address: ${e.message}")
        }
    }
}
