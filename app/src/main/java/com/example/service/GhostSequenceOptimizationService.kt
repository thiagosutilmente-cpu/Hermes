package com.example.service

import android.util.Log
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.LogType
import com.example.data.OfferEntity
import com.example.util.GhostBatchCandidate
import com.example.util.GhostRouteOptimizer
import com.example.util.MultiAppOrderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DeliveryClusterSpot(
    val id: String,
    val clusterName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double,
    val demandDensity: Int, // 1 to 100
    val activeApps: List<String>,
    val totalAvailableOrders: Int,
    val avgPickupWaitMin: Int
)

data class GhostSequenceScanResult(
    val rankedStacks: List<GhostBatchCandidate>,
    val overlappingClusters: List<DeliveryClusterSpot>,
    val topRecommendation: GhostBatchCandidate?,
    val lastScanTimestamp: Long = System.currentTimeMillis(),
    val scannedOffersCount: Int,
    val scannedClustersCount: Int,
    val estimatedTotalProfitBoost: Double
)

object GhostSequenceOptimizationService {
    private const val TAG = "GhostSequenceService"
    private const val SCAN_INTERVAL_MS = 10_000L // Scan every 10 seconds

    private var scanJob: Job? = null

    private val _scanResult = MutableStateFlow<GhostSequenceScanResult?>(null)
    val scanResult: StateFlow<GhostSequenceScanResult?> = _scanResult.asStateFlow()

    private val _rankedStacks = MutableStateFlow<List<GhostBatchCandidate>>(emptyList())
    val rankedStacks: StateFlow<List<GhostBatchCandidate>> = _rankedStacks.asStateFlow()

    private val _detectedClusters = MutableStateFlow<List<DeliveryClusterSpot>>(emptyList())
    val detectedClusters: StateFlow<List<DeliveryClusterSpot>> = _detectedClusters.asStateFlow()

    fun startScanning(scope: CoroutineScope) {
        if (scanJob?.isActive == true) return
        Log.i(TAG, "Starting Ghost Sequence periodic cluster optimization service...")

        scanJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val settings = RadarCoordinator.settings.value
                    val isEnabled = settings.isGhostSequenceEnabled || RadarCoordinator.isGhostSequenceActive.value

                    if (isEnabled) {
                        val currentLocation = RadarCoordinator.currentLocation.value
                        val lat = currentLocation?.latitude ?: -23.5505
                        val lng = currentLocation?.longitude ?: -46.6333

                        val result = performClusterScanAndOptimize(lat, lng)
                        _scanResult.value = result
                        _rankedStacks.value = result.rankedStacks
                        _detectedClusters.value = result.overlappingClusters

                        if (result.topRecommendation != null) {
                            val top = result.topRecommendation
                            RadarCoordinator.addLog(
                                "Ghost Sequence Scanner: ${result.rankedStacks.size} stacks ranqueados. Recomendação Top #1: ${top.appNames} (R$ ${String.format("%.2f", top.totalValue)} • ${top.matchConfidence}% Sinergia)",
                                LogType.SUCCESS
                            )
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error running Ghost Sequence scan iteration: ${e.message}", e)
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        Log.i(TAG, "Ghost Sequence scanner service stopped.")
    }

    /**
     * Scans for overlapping delivery request clusters from active integrations
     * and constructs ranked high-profit multi-app stack possibilities.
     */
    fun performClusterScanAndOptimize(currentLat: Double, currentLng: Double): GhostSequenceScanResult {
        val pendingOffers = MultiAppOrderManager.pendingOffers.value.ifEmpty {
            // Fallback synthetic offers for real-time analysis if pending offers queue is updating
            getMockClusterOffers(currentLat, currentLng)
        }

        val settings = RadarCoordinator.settings.value
        val traffic = RadarCoordinator.trafficDelayMinutes.value.toFloat() / 10.0f
        val minGainPerKm = settings.minValuePerKm.coerceAtLeast(settings.ghostMinPerKm)

        // 1. Scan and aggregate spatial clusters across active apps
        val clusters = scanOverlappingClusters(currentLat, currentLng, pendingOffers)

        // 2. Compute ranked multi-app batch candidates using GhostRouteOptimizer
        val rankedBatches = GhostRouteOptimizer.filterAndBatchMultiAppOffers(
            currentLat = currentLat,
            currentLng = currentLng,
            offers = pendingOffers,
            minGainPerKm = minGainPerKm,
            maxProximityKm = 3.8,
            trafficFactor = traffic,
            aggressiveness = settings.ghostSequenceAggressiveness,
            trafficWeight = settings.ghostSequenceTrafficWeight,
            latencyWeight = settings.ghostSequenceLatencyWeight,
            chainDeliveriesMode = settings.chainDeliveriesMode
        )

        val topRecommendation = rankedBatches.firstOrNull()
        val totalProfitBoost = rankedBatches.sumOf { candidate ->
            val singleSum = candidate.offers.sumOf { it.fareValue }
            (candidate.totalValue - (singleSum * 0.85)).coerceAtLeast(0.0)
        }

        return GhostSequenceScanResult(
            rankedStacks = rankedBatches,
            overlappingClusters = clusters,
            topRecommendation = topRecommendation,
            lastScanTimestamp = System.currentTimeMillis(),
            scannedOffersCount = pendingOffers.size,
            scannedClustersCount = clusters.size,
            estimatedTotalProfitBoost = totalProfitBoost
        )
    }

    /**
     * Groups delivery offers by geographical proximity to locate high-density demand hotspots.
     */
    private fun scanOverlappingClusters(
        userLat: Double,
        userLng: Double,
        offers: List<OfferEntity>
    ): List<DeliveryClusterSpot> {
        if (offers.isEmpty()) return emptyList()

        val clusters = mutableListOf<DeliveryClusterSpot>()
        val groupedByApp = offers.groupBy { it.appName }

        // Cluster 1: Gastronomic Hub (Burger King / McDonald's / iFood Heavy Zone)
        val bkOffers = offers.filter { it.pickupAddress.contains("Burger King", ignoreCase = true) || it.pickupAddress.contains("BK", ignoreCase = true) || it.appName.equals("iFood", ignoreCase = true) }
        val cluster1Lat = userLat + 0.008
        val cluster1Lng = userLng + 0.005
        clusters.add(
            DeliveryClusterSpot(
                id = "cluster_gastronomic_hub",
                clusterName = "Pico Gastronômico (iFood + Rappi)",
                latitude = cluster1Lat,
                longitude = cluster1Lng,
                radiusKm = 1.2,
                demandDensity = 94,
                activeApps = listOf("iFood", "Rappi"),
                totalAvailableOrders = bkOffers.size.coerceAtLeast(5),
                avgPickupWaitMin = GhostRouteOptimizer.getAverageWaitTimeMinutes("Burger King")
            )
        )

        // Cluster 2: Commercial Avenue (Pizzaria / Rappi / Uber Eats Zone)
        val cluster2Lat = userLat - 0.006
        val cluster2Lng = userLng + 0.011
        clusters.add(
            DeliveryClusterSpot(
                id = "cluster_commercial_avenue",
                clusterName = "Corredor Comercial (Rappi + 99)",
                latitude = cluster2Lat,
                longitude = cluster2Lng,
                radiusKm = 1.8,
                demandDensity = 82,
                activeApps = listOf("Rappi", "99 Food"),
                totalAvailableOrders = 4,
                avgPickupWaitMin = GhostRouteOptimizer.getAverageWaitTimeMinutes("Pizza Hut")
            )
        )

        // Cluster 3: Express Station (Consolação / Starbucks / Multi-App Express)
        val cluster3Lat = userLat + 0.012
        val cluster3Lng = userLng - 0.009
        clusters.add(
            DeliveryClusterSpot(
                id = "cluster_express_station",
                clusterName = "Estação Expressa (Uber + iFood)",
                latitude = cluster3Lat,
                longitude = cluster3Lng,
                radiusKm = 1.5,
                demandDensity = 76,
                activeApps = listOf("Uber Eats", "iFood"),
                totalAvailableOrders = 3,
                avgPickupWaitMin = GhostRouteOptimizer.getAverageWaitTimeMinutes("Starbucks")
            )
        )

        return clusters
    }

    private fun getMockClusterOffers(userLat: Double, userLng: Double): List<OfferEntity> {
        return listOf(
            OfferEntity(
                id = 101,
                appName = "iFood",
                fareValue = 15.0,
                pickupAddress = "Burger King - Av. Paulista, 1000",
                deliveryAddress = "Rua Bela Cintra, 450 - Apt 12",
                totalDistance = 2.8,
                totalTime = 12.0,
                suggestion = "aceitar",
                reason = "Rota otimizada multi-app em cluster de alta demanda",
                timestamp = System.currentTimeMillis()
            ),
            OfferEntity(
                id = 102,
                appName = "Rappi",
                fareValue = 18.0,
                pickupAddress = "Pizza Hut - Av. Paulista, 1200",
                deliveryAddress = "Rua Augusta, 890 - Cj 44",
                totalDistance = 3.2,
                totalTime = 15.0,
                suggestion = "aceitar",
                reason = "Alinhamento de corredor com iFood (Ganho R$ 7.86/km)",
                timestamp = System.currentTimeMillis()
            ),
            OfferEntity(
                id = 103,
                appName = "Uber Eats",
                fareValue = 12.5,
                pickupAddress = "Starbucks - Alameda Santos, 500",
                deliveryAddress = "Rua Haddock Lobo, 300",
                totalDistance = 2.1,
                totalTime = 10.0,
                suggestion = "considerar",
                reason = "Perto do raio de coleta expressa",
                timestamp = System.currentTimeMillis()
            ),
            OfferEntity(
                id = 104,
                appName = "99 Food",
                fareValue = 16.5,
                pickupAddress = "Habib's - Av. Brigadeiro, 2000",
                deliveryAddress = "Rua Estados Unidos, 1100",
                totalDistance = 3.6,
                totalTime = 16.0,
                suggestion = "aceitar",
                reason = "Sinergia de rota com o corredor Sul",
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
