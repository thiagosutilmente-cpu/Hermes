package com.example.radar.data

enum class ConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}

data class ApiConnectionStatus(
    val state: ConnectionState = ConnectionState.CONNECTED,
    val endpoint: String = "api.radar-coordinator.local",
    val latencyMs: Int = 18,
    val gpsAccuracyMeters: Float = 4.2f,
    val activeServices: List<String> = listOf("iFood", "Rappi", "Uber", "99")
)

data class DeliveryOffer(
    val id: String,
    val appName: String,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val timeMinutes: Int,
    val pickupAddress: String,
    val deliveryAddress: String,
    val neuralDecision: String = "ACCEPT",
    val confidence: Double = 0.95
) {
    val gainPerKm: Double
        get() = if (distanceKm > 0) value / distanceKm else value
}

data class MonitoringState(
    val isMonitoringActive: Boolean = true,
    val offersScannedToday: Int = 42,
    val acceptedOffers: Int = 18,
    val totalEarningsToday: Double = 284.50,
    val autoAcceptThresholdPerKm: Double = 5.0,
    val lastDetectedOffer: DeliveryOffer? = null
)
