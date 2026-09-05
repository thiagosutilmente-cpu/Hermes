package com.example

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Evento de cruzamento do limiar de segurança de velocidade (10.0 km/h).
 */
sealed class SafetyThresholdEvent {
    data class ThresholdExceeded(val speedKmh: Double) : SafetyThresholdEvent()
    data class ThresholdCleared(val speedKmh: Double) : SafetyThresholdEvent()
}

/**
 * Estado agregado de telemetria de velocidade e segurança pronto para consumo na UI.
 */
data class SpeedSafetyUiState(
    val currentSpeedKmh: Double = 0.0,
    val isSafetyLockActive: Boolean = false,
    val speedSource: String = "GPS Fused",
    val accuracyMeters: Float = 0f,
    val isServiceConnected: Boolean = false,
    val isTracking: Boolean = false,
    val lockThresholdKmh: Double = LocationService.SAFETY_SPEED_LOCK_THRESHOLD_KMH,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
)

/**
 * [SpeedSafetyViewModel]
 *
 * Conecta-se ao [LocationService] para fornecer um stream reativo e em tempo real da velocidade
 * do usuário para a interface gráfica (Jetpack Compose).
 *
 * Principais responsabilidades:
 * 1. Observa o fluxo contínuo de [LocationSpeedState] emitido pelo serviço de localização.
 * 2. Garante atualização reativa imediata quando os limiares de segurança são ultrapassados (> 10 km/h).
 * 3. Dispara eventos de alerta ([safetyThresholdEvents]) ao cruzar o limiar de bloqueio/desbloqueio.
 * 4. Gerencia a inicialização, bind/unbind e simulação controlada para testes.
 */
class SpeedSafetyViewModel(application: Application) : AndroidViewModel(application) {

    private var locationService: LocationService? = null
    private val _isServiceBound = MutableStateFlow(false)

    // Estado da UI expondo a telemetria em tempo real
    private val _uiState = MutableStateFlow(SpeedSafetyUiState())
    val uiState: StateFlow<SpeedSafetyUiState> = _uiState.asStateFlow()

    // Stream de eventos acionados especificamente quando o limiar de 10 km/h é cruzado
    private val _safetyThresholdEvents = MutableSharedFlow<SafetyThresholdEvent>(extraBufferCapacity = 16)
    val safetyThresholdEvents: SharedFlow<SafetyThresholdEvent> = _safetyThresholdEvents.asSharedFlow()

    // Conexão do serviço Bound
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LocationService.LocalBinder
            locationService = binder?.getService()
            _isServiceBound.value = true
            _uiState.value = _uiState.value.copy(isServiceConnected = true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            _isServiceBound.value = false
            _uiState.value = _uiState.value.copy(isServiceConnected = false)
        }
    }

    init {
        // Observa o stream de localização do LocationService
        viewModelScope.launch {
            LocationService.globalLocationState.collect { locationState ->
                val previousLockState = _uiState.value.isSafetyLockActive
                val newLockState = locationState.isSafetyLockActive

                // Atualiza o estado da UI
                _uiState.value = _uiState.value.copy(
                    currentSpeedKmh = locationState.currentSpeedKmh,
                    isSafetyLockActive = newLockState,
                    speedSource = locationState.speedSource,
                    accuracyMeters = locationState.accuracyMeters,
                    isTracking = locationState.isTracking,
                    lastUpdateTimestamp = locationState.lastUpdateTimeMillis
                )

                // Verifica se o limiar de segurança foi cruzado
                if (!previousLockState && newLockState) {
                    _safetyThresholdEvents.emit(
                        SafetyThresholdEvent.ThresholdExceeded(locationState.currentSpeedKmh)
                    )
                } else if (previousLockState && !newLockState) {
                    _safetyThresholdEvents.emit(
                        SafetyThresholdEvent.ThresholdCleared(locationState.currentSpeedKmh)
                    )
                }
            }
        }

        // Tenta iniciar e vincular ao LocationService
        bindLocationService()
    }

    /**
     * Inicia e vincula o serviço em primeiro plano para rastreamento contínuo.
     */
    fun bindLocationService() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START_LOCATION_TRACKING
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            // Em ambientes sem serviço de background permitido imediatamente, o stream global ainda opera
        }
    }

    /**
     * Permite simulação manual de velocidade para testes rápidos da interface.
     */
    fun setSimulatedSpeed(speedKmh: Double) {
        LocationService.updateSimulatedSpeed(speedKmh)
    }

    /**
     * Alterna rapidamente entre parado (0 km/h) e em trânsito (32 km/h) para validação do limiar.
     */
    fun toggleTestSpeed() {
        val currentSpeed = _uiState.value.currentSpeedKmh
        val nextSpeed = if (currentSpeed > LocationService.SAFETY_SPEED_LOCK_THRESHOLD_KMH) 0.0 else 32.0
        setSimulatedSpeed(nextSpeed)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            val context = getApplication<Application>()
            if (_isServiceBound.value) {
                context.unbindService(serviceConnection)
                _isServiceBound.value = false
            }
        } catch (e: Exception) {
            // Unbind defensivo
        }
    }
}
