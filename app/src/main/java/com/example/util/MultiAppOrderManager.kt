package com.example.util

import android.util.Log
import com.example.coordinator.RadarCoordinator
import com.example.data.FirestoreManager
import com.example.api.DailyReportItem
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import com.example.data.OfferEntity

data class ActiveOrder(
    val id: String,
    val appName: String,
    val fare: Double,
    val pickupAddress: String,
    val deliveryAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryLat: Double,
    val deliveryLng: Double,
    var status: OrderStatus = OrderStatus.PICKING_UP,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OrderStatus {
    PICKING_UP,
    PICKED_UP,
    DELIVERING,
    COMPLETED,
    CANCELLED
}

object MultiAppOrderManager {
    private const val TAG = "MultiAppOrderManager"
    
    private val _activeOrders = MutableStateFlow<List<ActiveOrder>>(emptyList())
    val activeOrders = _activeOrders.asStateFlow()

    private val _pendingOffers = MutableStateFlow<List<OfferEntity>>(emptyList())
    val pendingOffers = _pendingOffers.asStateFlow()

    private val _optimizedRoute = MutableStateFlow<List<StopPoint>>(emptyList())
    val optimizedRoute = _optimizedRoute.asStateFlow()

    private val _currentNavigationAddress = MutableStateFlow<String>("")
    val currentNavigationAddress = _currentNavigationAddress.asStateFlow()

    private var activeOrdersListener: ListenerRegistration? = null
    private var pedidosListener: ListenerRegistration? = null

    init {
        startFirestoreSync()
    }

    fun startFirestoreSync() {
        activeOrdersListener?.remove()
        activeOrdersListener = FirestoreManager.listenToActiveOrders { remoteOrders ->
            _activeOrders.value = remoteOrders
            recalculateRoute()
        }

        pedidosListener?.remove()
        pedidosListener = FirestoreManager.listenToPedidosCollection { pendingList ->
            Log.d(TAG, "Syncing real-time offers from 'pedidos' collection: count=${pendingList.size}")
            _pendingOffers.value = pendingList
        }

        FirestoreManager.seedPedidosIfEmpty()
    }

    fun setNavigationAddress(address: String) {
        _currentNavigationAddress.value = address
    }

    fun addOrder(order: ActiveOrder) {
        _activeOrders.update { current ->
            if (current.none { it.id == order.id }) {
                current + order
            } else {
                current
            }
        }
        // Sync to Firebase Firestore in real-time
        FirestoreManager.saveActiveOrder(order)
        recalculateRoute()
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        val targetOrder = _activeOrders.value.find { it.id == orderId }
        _activeOrders.update { current ->
            current.map {
                if (it.id == orderId) it.copy(status = newStatus) else it
            }
        }
        
        // Sync order status change to Firestore in real-time
        FirestoreManager.updateActiveOrderStatus(orderId, newStatus)

        if (newStatus == OrderStatus.COMPLETED) {
            targetOrder?.let { completedOrder ->
                recordCompletedEarnings(completedOrder)
            }
            removeOrder(orderId)
        } else if (newStatus == OrderStatus.CANCELLED) {
            removeOrder(orderId)
        } else {
            recalculateRoute()
        }
    }

    fun removeOrder(orderId: String) {
        _activeOrders.update { current ->
            current.filterNot { it.id == orderId }
        }
        FirestoreManager.removeActiveOrder(orderId)
        recalculateRoute()
    }

    private fun recordCompletedEarnings(order: ActiveOrder) {
        try {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val currentReport = DailyReportItem(
                date = dateStr,
                totalOffersEvaluated = 1,
                totalOffersAccepted = 1,
                totalOffersRejected = 0,
                totalOffersConsidered = 1,
                estimatedEarnings = order.fare,
                totalDistanceKm = 3.5, // Estimated average distance
                totalTimeMin = 15.0,
                averageFareValue = order.fare,
                earningsPerKm = if (3.5 > 0) order.fare / 3.5 else order.fare,
                appBreakdown = null
            )
            FirestoreManager.saveDailyReport(currentReport)

            val currentCompletedCount = RadarCoordinator.todaySummary.value.totalAccepted + 1
            val currentTotalEarnings = RadarCoordinator.todaySummary.value.totalEarnings + order.fare
            FirestoreManager.saveActiveSessionStats(
                completedCount = currentCompletedCount,
                totalEarnings = currentTotalEarnings,
                totalDistanceKm = RadarCoordinator.todaySummary.value.totalDistanceKm + 3.5,
                totalTimeMinutes = RadarCoordinator.todaySummary.value.totalTimeMinutes + 15.0
            )
            Log.d(TAG, "Recorded completed earnings for order ${order.id}: R$ ${order.fare} to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording completed earnings", e)
        }
    }

    fun recalculateRoute() {
        val currentOrders = _activeOrders.value
        if (currentOrders.isEmpty()) {
            _optimizedRoute.value = emptyList()
            return
        }

        val location = RadarCoordinator.currentLocation.value
        val lat = location?.latitude ?: -23.5505
        val lng = location?.longitude ?: -46.6333

        val stops = mutableListOf<StopPoint>()
        currentOrders.forEach { order ->
            val isUrgentApp = order.appName.lowercase().contains("ifood") || order.appName.lowercase().contains("rappi")
            val urgency = if (isUrgentApp) 1.8 else 1.2
            if (order.status == OrderStatus.PICKING_UP) {
                stops.add(
                    StopPoint(
                        id = "${order.id}_pickup",
                        address = order.pickupAddress,
                        latitude = order.pickupLat,
                        longitude = order.pickupLng,
                        type = StopType.PICKUP,
                        orderId = order.id,
                        urgencyScore = urgency,
                        baseValue = order.fare
                    )
                )
            }
            if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                stops.add(
                    StopPoint(
                        id = "${order.id}_delivery",
                        address = order.deliveryAddress,
                        latitude = order.deliveryLat,
                        longitude = order.deliveryLng,
                        type = StopType.DELIVERY,
                        orderId = order.id,
                        urgencyScore = urgency,
                        baseValue = order.fare
                    )
                )
            }
        }

        val isSmartEnabled = com.example.coordinator.RadarCoordinator.settings.value.smartSequenceEnabled
        val optimized = if (isSmartEnabled) {
            RouteOptimizer.optimizeRoute(lat, lng, stops)
        } else {
            stops
        }
        _optimizedRoute.value = optimized
        
        if (optimized.isNotEmpty()) {
            val nextStop = optimized.first()
            val summary = optimized.joinToString(" -> ") { "${it.type} (${it.address.split(",").first()})" }
            Log.d(TAG, "Roteiro Multi-App Otimizado: $summary")
            RadarCoordinator.addLog("Logística: Roteiro Multi-App recalculado. Próximo: ${nextStop.type} em ${nextStop.address.split(",").first()}", com.example.coordinator.LogType.INFO)

            // Seamlessly update the coordinator's active destination so GPS and TTS auto-align to the optimized multi-stop sequence
            val currentSettings = RadarCoordinator.settings.value
            if (currentSettings.activeDeliveryDestination != nextStop.address) {
                val updatedSettings = currentSettings.copy(
                    isActiveDeliveryEnabled = true,
                    activeDeliveryDestination = nextStop.address
                )
                RadarCoordinator.updateSettings(updatedSettings)
            }
        }
    }
}
