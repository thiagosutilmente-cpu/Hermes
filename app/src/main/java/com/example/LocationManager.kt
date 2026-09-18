package com.example

import android.Manifest
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Modelo de dados com as coordenadas GPS e telemetria capturadas em tempo real.
 */
data class DriverGpsLocation(
    val latitude: Double = -23.561684,
    val longitude: Double = -46.655981,
    val accuracy: Float = 4.2f,
    val speedKmh: Double = 0.0,
    val altitude: Double = 780.0,
    val bearing: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isMock: Boolean = false,
    val provider: String = "fused"
)

/**
 * Classe [LocationManager] baseada no [FusedLocationProviderClient] da Google Play Services.
 *
 * Responsabilidades:
 * 1. Verificar e garantir permissões de acesso ao local (FINE e COARSE LOCATION).
 * 2. Capturar coordenadas GPS em tempo real de alta precisão (Priority.PRIORITY_HIGH_ACCURACY).
 * 3. Expor atualizações contínuas via [StateFlow] e [Flow] reativos.
 * 4. Obter a última localização conhecida instantaneamente (getLastLocation).
 * 5. Calcular distância geodésica e tempo de aproximação até pontos de coleta/restaurantes.
 */
class LocationManager private constructor(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private val _currentLocation = MutableStateFlow(DriverGpsLocation())
    val currentLocation: StateFlow<DriverGpsLocation> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var activeLocationCallback: LocationCallback? = null

    companion object {
        private const val TAG = "RadarLocationManager"

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        @Volatile
        private var instance: LocationManager? = null

        fun getInstance(context: Context): LocationManager {
            return instance ?: synchronized(this) {
                instance ?: LocationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Verifica se as permissões de localização necessárias foram concedidas pelo usuário.
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Verifica se a permissão de alta precisão (GPS de hardware) foi concedida.
     */
    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Cria uma requisição padronizada de localização de alta precisão para entregadores.
     *
     * @param intervalMillis Intervalo padrão de atualização em milissegundos (default 2000ms = 2s).
     * @param minUpdateIntervalMillis Intervalo mínimo em milissegundos (default 1000ms = 1s).
     * @param minDistanceMeters Distância mínima de deslocamento para disparo (default 1.0 metro).
     */
    fun createLocationRequest(
        intervalMillis: Long = 2000L,
        minUpdateIntervalMillis: Long = 1000L,
        minDistanceMeters: Float = 1.0f
    ): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(minUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()
    }

    /**
     * Obtém a última localização conhecida pelo Fused Location Provider de forma assíncrona.
     */
    fun getLastLocation(onSuccess: (DriverGpsLocation) -> Unit, onFailure: ((Exception) -> Unit)? = null) {
        if (!hasLocationPermission()) {
            val err = SecurityException("Permissões de localização não concedidas.")
            Log.w(TAG, err.message ?: "")
            onFailure?.invoke(err)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        val gps = loc.toDriverGpsLocation()
                        _currentLocation.value = gps
                        onSuccess(gps)
                    } else {
                        onSuccess(_currentLocation.value)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Falha ao obter última localização do Fused Location", e)
                    onFailure?.invoke(e)
                }
        } catch (se: SecurityException) {
            Log.e(TAG, "Exceção de segurança em getLastLocation", se)
            onFailure?.invoke(se)
        }
    }

    /**
     * Inicia a captura contínua de coordenadas GPS em tempo real.
     * Atualiza automaticamente o [currentLocation] StateFlow.
     */
    fun startRealtimeLocationUpdates(
        intervalMillis: Long = 2000L,
        onLocationReceived: ((DriverGpsLocation) -> Unit)? = null
    ): Boolean {
        if (!hasLocationPermission()) {
            Log.w(TAG, "startRealtimeLocationUpdates: Permissão de localização negada.")
            return false
        }

        stopRealtimeLocationUpdates()

        val request = createLocationRequest(intervalMillis)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val gps = loc.toDriverGpsLocation()
                _currentLocation.value = gps
                onLocationReceived?.invoke(gps)
            }
        }

        activeLocationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
            _isTracking.value = true
            Log.i(TAG, "FusedLocationProvider: Captura contínua de GPS iniciada.")
            return true
        } catch (se: SecurityException) {
            Log.e(TAG, "Erro de segurança ao solicitar atualizações de GPS", se)
            _isTracking.value = false
            return false
        }
    }

    /**
     * Interrompe as atualizações contínuas de coordenadas GPS.
     */
    fun stopRealtimeLocationUpdates() {
        activeLocationCallback?.let { callback ->
            try {
                fusedLocationClient.removeLocationUpdates(callback)
                Log.i(TAG, "FusedLocationProvider: Atualizações de GPS interrompidas.")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao remover callback de localização", e)
            }
            activeLocationCallback = null
        }
        _isTracking.value = false
    }

    /**
     * Retorna um [Flow] de [DriverGpsLocation] para coleta em Coroutines / Jetpack Compose.
     */
    fun getLocationFlow(intervalMillis: Long = 2000L): Flow<DriverGpsLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Permissão de localização não concedida para o Flow"))
            return@callbackFlow
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val gps = loc.toDriverGpsLocation()
                trySend(gps)
            }
        }

        val request = createLocationRequest(intervalMillis)

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (se: SecurityException) {
            close(se)
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(callback)
            } catch (_: Exception) {}
        }
    }

    /**
     * Calcula a distância geodésica em quilômetros até um determinado ponto (restaurante ou entrega)
     * usando a fórmula de Haversine.
     */
    fun calculateDistanceKmTo(targetLat: Double, targetLng: Double): Double {
        val current = _currentLocation.value
        return calculateHaversineKm(current.latitude, current.longitude, targetLat, targetLng)
    }

    /**
     * Converte um objeto [Location] do Android para [DriverGpsLocation].
     */
    private fun Location.toDriverGpsLocation(): DriverGpsLocation {
        val speedKmh = if (hasSpeed() && speed >= 0) {
            (speed * 3.6).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val isMockLocation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            isMock
        } else {
            @Suppress("DEPRECATION")
            isFromMockProvider
        }

        return DriverGpsLocation(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            speedKmh = Math.round(speedKmh * 10.0) / 10.0,
            altitude = altitude,
            bearing = bearing,
            timestamp = time,
            isMock = isMockLocation,
            provider = provider ?: "fused"
        )
    }

    /**
     * Fórmula de Haversine para cálculo de distância entre duas coordenadas WGS84 em km.
     */
    private fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Raio da Terra em km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return Math.round(r * c * 100.0) / 100.0
    }
}
