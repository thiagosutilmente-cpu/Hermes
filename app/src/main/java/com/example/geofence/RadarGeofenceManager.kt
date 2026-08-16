package com.example.geofence

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.BuildConfig
import com.example.coordinator.RadarCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gerenciador central de Geofencing utilizando motor autônomo de cálculo de proximidade.
 * Monitora hot-spots de alta demanda, calcula proximidade em tempo real e dispara alertas por voz
 * integrados com o limite de velocidade configurado no .env (BuildConfig.MAX_SPEED_LIMIT_KMH).
 */
object RadarGeofenceManager {
    private const val TAG = "RadarGeofenceManager"

    private val _currentActiveZone = MutableStateFlow<DemandZone?>(null)
    val currentActiveZone: StateFlow<DemandZone?> = _currentActiveZone.asStateFlow()

    private val zoneNotificationCooldowns = mutableMapOf<String, Long>()
    private const val COOLDOWN_MILLIS = 3 * 60 * 1000L // 3 minutos entre avisos da mesma zona

    /**
     * Inicializa o motor de Geofencing e zonas de alta demanda
     */
    fun initialize(context: Context) {
        try {
            Log.i(TAG, "RadarGeofenceManager inicializado. Motor nativo de proximidade e zonas ativado com ${DemandZonesCatalog.DEFAULT_ZONES.size} polos mapeados.")
        } catch (e: Throwable) {
            Log.w(TAG, "Aviso ao inicializar RadarGeofenceManager: ${e.message}")
        }
    }

    /**
     * Zonas de alta demanda cadastradas e monitoradas
     */
    fun registerDemandZones(
        context: Context,
        zones: List<DemandZone> = DemandZonesCatalog.DEFAULT_ZONES
    ) {
        Log.i(TAG, "${zones.size} zonas de alta demanda ativas no motor contínuo de coordenadas.")
    }

    /**
     * Avaliador de proximidade em tempo real (Proximity & Speed Evaluator).
     * Garante detecção mesmo em modos de simulação, mudanças rápidas de GPS e sincroniza com o .env.
     */
    fun evaluateLocationProximity(
        latitude: Double,
        longitude: Double,
        speedKmh: Float = RadarCoordinator.currentSpeedKmh.value
    ) {
        val now = System.currentTimeMillis()
        val speedLimitEnv = resolveMaxSpeedLimitEnv()

        var matchedZone: DemandZone? = null

        for (zone in DemandZonesCatalog.DEFAULT_ZONES) {
            val distMeters = calculateDistanceMeters(latitude, longitude, zone.latitude, zone.longitude)
            if (distMeters <= zone.radiusMeters) {
                matchedZone = zone
                val lastNotified = zoneNotificationCooldowns[zone.id] ?: 0L
                if (now - lastNotified > COOLDOWN_MILLIS) {
                    zoneNotificationCooldowns[zone.id] = now
                    triggerProactiveVoiceAlert(zone, speedKmh, speedLimitEnv)
                }
                break
            }
        }

        _currentActiveZone.value = matchedZone
    }

    /**
     * Resolve o limite de velocidade configurado no .env via BuildConfig
     */
    fun resolveMaxSpeedLimitEnv(): Float {
        return try {
            BuildConfig.MAX_SPEED_LIMIT_KMH.toFloatOrNull() ?: 40.0f
        } catch (e: Throwable) {
            40.0f
        }
    }

    private fun triggerProactiveVoiceAlert(
        zone: DemandZone,
        speedKmh: Float,
        speedLimitKmh: Float
    ) {
        val isSpeeding = speedKmh > speedLimitKmh
        val apps = zone.primaryApps.joinToString(" e ")
        val mult = String.format("%.2f", zone.surgeMultiplier).replace(".", ",")

        val speech = if (isSpeeding) {
            "Atenção Thiago! Você entrou na área de alta demanda do ${zone.name}, dinâmica de ${mult}x no $apps. Alerta de velocidade: você está a ${speedKmh.toInt()} km por hora. O limite de segurança do sistema é ${speedLimitKmh.toInt()} km por hora. Reduza a velocidade."
        } else {
            "Radar Jarvis: Você está na zona de alta demanda ${zone.name}. Multiplicador dinâmico de ${mult}x no $apps. Jarvis ativo para capturar as melhores corridas."
        }

        try {
            RadarCoordinator.voiceManager?.speak(speech)
        } catch (e: Throwable) {
            Log.w(TAG, "Erro ao disparar voz de geofence: ${e.message}")
        }
    }

    /**
     * Remove todos os geofences cadastrados
     */
    fun removeGeofences(context: Context) {
        zoneNotificationCooldowns.clear()
        _currentActiveZone.value = null
        Log.i(TAG, "Zonas de geofencing limpas.")
    }

    private fun calculateDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
}
