package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * SpeedSafetyLockUtil
 *
 * Utilitário especializado em monitoramento da velocidade real do dispositivo através do GPS
 * via [FusedLocationProviderClient] do Google Play Services.
 *
 * Responsabilidades:
 * 1. Conectar ao GPS em alta precisão (`Priority.PRIORITY_HIGH_ACCURACY`) com amostragem a cada 1000ms.
 * 2. Calcular a velocidade real do deslocamento em km/h (`location.speed * 3.6` ou cálculo Haversine entre waypoints).
 * 3. Ativar a Trava de Segurança que bloqueia a interface do usuário quando a velocidade for superior a 10 km/h.
 * 4. Desbloquear a interface automaticamente quando o veículo parar ou reduzir abaixo do limiar (com histerese anti-oscilação).
 * 5. Notificar ouvintes (TTS, UI de Bloqueio, HUD Flutuante) das mudanças de estado da trava.
 */
class SpeedSafetyLockUtil private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SpeedSafetyLockUtil"

        // Limiar obrigatório de segurança: 10.0 km/h
        const val LOCK_SPEED_THRESHOLD_KMH = 10.0

        // Limiar inferior de histerese para desbloquear (evita oscilação na borda de 10 km/h)
        const val UNLOCK_HYSTERESIS_THRESHOLD_KMH = 8.5

        @Volatile
        private var instance: SpeedSafetyLockUtil? = null

        fun getInstance(context: Context): SpeedSafetyLockUtil {
            return instance ?: synchronized(this) {
                instance ?: SpeedSafetyLockUtil(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Modelo de estado do utilitário de segurança
     */
    data class State(
        val currentSpeedKmh: Double = 0.0,
        val isLocked: Boolean = false,
        val thresholdKmh: Double = LOCK_SPEED_THRESHOLD_KMH,
        val latitude: Double = -23.561684,
        val longitude: Double = -46.655981,
        val accuracyMeters: Float = 0f,
        val isTrackingActive: Boolean = false,
        val isSimulationMode: Boolean = false,
        val statusMessage: String = "Monitoramento inativo"
    )

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var lastLocation: Location? = null
    private var lastLocationTimestamp: Long = 0L

    var onLockStateChanged: ((isLocked: Boolean, speedKmh: Double) -> Unit)? = null

    /**
     * Inicia o monitoramento de velocidade em tempo real via FusedLocationProviderClient
     */
    @SuppressLint("MissingPermission")
    fun startTracking(): Boolean {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Permissão de localização não concedida para o SpeedSafetyLockUtil.")
            _state.value = _state.value.copy(
                isTrackingActive = false,
                statusMessage = "Aguardando permissão de GPS"
            )
            return false
        }

        if (_state.value.isTrackingActive) {
            return true
        }

        stopTracking()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
            setMinUpdateIntervalMillis(500L)
            setMinUpdateDistanceMeters(0.5f)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                processNewLocation(loc)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _state.value = _state.value.copy(
                isTrackingActive = true,
                statusMessage = "GPS Fused Location ativo (Limite: 10 km/h)"
            )
            Log.i(TAG, "Monitoramento FusedLocationProviderClient iniciado com sucesso.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar atualizações de localização: ${e.message}", e)
            _state.value = _state.value.copy(
                isTrackingActive = false,
                statusMessage = "Falha ao iniciar GPS: ${e.message}"
            )
            return false
        }
    }

    /**
     * Interrompe o monitoramento de localização do GPS
     */
    fun stopTracking() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao remover atualizações de localização: ${e.message}")
            }
        }
        locationCallback = null
        _state.value = _state.value.copy(isTrackingActive = false)
    }

    /**
     * Processa a leitura do GPS, computa a velocidade real e aplica a trava de segurança
     */
    private fun processNewLocation(location: Location) {
        if (_state.value.isSimulationMode) return

        var calculatedSpeedKmh = 0.0

        // 1. Tenta usar o hardware speed do GPS (m/s -> km/h)
        if (location.hasSpeed() && location.speed >= 0f) {
            calculatedSpeedKmh = location.speed.toDouble() * 3.6
        } else {
            // 2. Fallback de cálculo geodésico (delta distância / delta tempo)
            val prev = lastLocation
            val now = System.currentTimeMillis()
            if (prev != null && lastLocationTimestamp > 0L) {
                val dtSeconds = (now - lastLocationTimestamp) / 1000.0
                if (dtSeconds in 0.5..10.0) {
                    val distMeters = prev.distanceTo(location)
                    val speedMps = distMeters / dtSeconds
                    calculatedSpeedKmh = (speedMps * 3.6).coerceIn(0.0, 180.0)
                }
            }
        }

        lastLocation = location
        lastLocationTimestamp = System.currentTimeMillis()

        updateSpeedAndEvaluateLock(
            speedKmh = calculatedSpeedKmh,
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy
        )
    }

    /**
     * Aplica a lógica da trava com histerese (trava > 10 km/h e destrava < 8.5 km/h)
     */
    private fun updateSpeedAndEvaluateLock(
        speedKmh: Double,
        lat: Double,
        lng: Double,
        accuracy: Float
    ) {
        val currentLocked = _state.value.isLocked
        val newLocked = when {
            speedKmh >= LOCK_SPEED_THRESHOLD_KMH -> true
            speedKmh <= UNLOCK_HYSTERESIS_THRESHOLD_KMH -> false
            else -> currentLocked // Faixa de histerese para evitar oscilação
        }

        val wasLocked = currentLocked
        val speedRounded = Math.round(speedKmh * 10.0) / 10.0

        _state.value = _state.value.copy(
            currentSpeedKmh = speedRounded,
            isLocked = newLocked,
            latitude = lat,
            longitude = lng,
            accuracyMeters = accuracy,
            statusMessage = if (newLocked) {
                "🚨 TRAVA ATIVA (> 10 km/h) • Interface Bloqueada para Segurança"
            } else {
                "✅ Interface Liberada (Velocidade Segura)"
            }
        )

        // Se o estado mudou, notifica os ouvintes
        if (wasLocked != newLocked) {
            Log.w(TAG, "Estado da trava de segurança alterado: $wasLocked -> $newLocked (Velocidade: $speedRounded km/h)")
            onLockStateChanged?.invoke(newLocked, speedRounded)

            // Feedback sonoro e tático
            if (newLocked) {
                try {
                    TextToSpeechManager.getInstance(context).speak(
                        "Atenção: velocidade acima de 10 por hora detectada por GPS. Trava de segurança ativada. Não use a tela em movimento."
                    )
                } catch (_: Exception) {}
            } else {
                try {
                    TextToSpeechManager.getInstance(context).speak(
                        "Veículo parado ou abaixo do limite. Interface liberada."
                    )
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Simula velocidade para testes rápidos em bancada sem necessidade de moto em movimento
     */
    fun simulateSpeed(simSpeedKmh: Double) {
        _state.value = _state.value.copy(isSimulationMode = true)
        updateSpeedAndEvaluateLock(
            speedKmh = simSpeedKmh,
            lat = _state.value.latitude,
            lng = _state.value.longitude,
            accuracy = 2.5f
        )
    }

    /**
     * Desativa o modo de simulação e retorna ao GPS real do FusedLocationProviderClient
     */
    fun resetSimulation() {
        _state.value = _state.value.copy(isSimulationMode = false)
        startTracking()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
