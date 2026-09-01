package com.example.radar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radar.data.ApiConnectionStatus
import com.example.radar.data.ConnectionState
import com.example.radar.data.DeliveryOffer
import com.example.radar.data.MonitoringState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RadarMonitorViewModel : ViewModel() {

    private val _connectionStatus = MutableStateFlow(
        ApiConnectionStatus(
            state = ConnectionState.CONNECTED,
            latencyMs = 14,
            gpsAccuracyMeters = 3.8f
        )
    )
    val connectionStatus: StateFlow<ApiConnectionStatus> = _connectionStatus.asStateFlow()

    private val sampleOffer = DeliveryOffer(
        id = "stk_8921",
        appName = "iFood + Rappi (Multi-Stack)",
        restaurant = "Burger King & Pizza Hut",
        value = 33.00,
        distanceKm = 4.2,
        timeMinutes = 18,
        pickupAddress = "Av. Paulista, 1578",
        deliveryAddress = "R. Bela Cintra, 904",
        neuralDecision = "ACCEPT",
        confidence = 0.96
    )

    private val _monitoringState = MutableStateFlow(
        MonitoringState(
            isMonitoringActive = true,
            offersScannedToday = 48,
            acceptedOffers = 19,
            totalEarningsToday = 284.50,
            lastDetectedOffer = sampleOffer
        )
    )
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    fun toggleMonitoring() {
        val newStatus = !_monitoringState.value.isMonitoringActive
        _monitoringState.update { it.copy(isMonitoringActive = newStatus) }
    }

    fun setMonitoringActive(active: Boolean) {
        _monitoringState.update { it.copy(isMonitoringActive = active) }
    }

    fun reconnectApi() {
        viewModelScope.launch {
            _connectionStatus.update { it.copy(state = ConnectionState.CONNECTING) }
            delay(1200)
            _connectionStatus.update {
                it.copy(
                    state = ConnectionState.CONNECTED,
                    latencyMs = (10..22).random()
                )
            }
        }
    }

    fun acceptCurrentOffer() {
        _monitoringState.update { current ->
            val offer = current.lastDetectedOffer
            val addValue = offer?.value ?: 0.0
            current.copy(
                acceptedOffers = current.acceptedOffers + 1,
                totalEarningsToday = current.totalEarningsToday + addValue,
                lastDetectedOffer = null
            )
        }
    }

    fun declineCurrentOffer() {
        _monitoringState.update { current ->
            current.copy(lastDetectedOffer = null)
        }
    }

    fun injectCustomOffer(value: Double, distance: Double, app: String) {
        val gainPerKm = if (distance > 0) value / distance else value
        val decision = if (gainPerKm >= 5.0 || (gainPerKm >= 3.5 && distance <= 4.0)) "ACCEPT" else "DECLINE"
        val customOffer = DeliveryOffer(
            id = "custom_${System.currentTimeMillis() % 10000}",
            appName = app,
            restaurant = "Outback Steakhouse & Starbucks",
            value = value,
            distanceKm = distance,
            timeMinutes = (distance * 4.2).toInt().coerceAtLeast(8),
            pickupAddress = "Shopping Morumbi",
            deliveryAddress = "Av. Chucri Zaidan, 1200",
            neuralDecision = decision,
            confidence = if (decision == "ACCEPT") 0.94 else 0.82
        )
        _monitoringState.update { current ->
            current.copy(
                isMonitoringActive = true,
                offersScannedToday = current.offersScannedToday + 1,
                lastDetectedOffer = customOffer
            )
        }
    }
}
