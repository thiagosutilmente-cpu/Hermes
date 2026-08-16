package com.example.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.example.coordinator.RadarCoordinator
import com.example.data.FirebaseAuthManager
import com.example.data.FirestoreManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsável por capturar eventos de Geofencing da Location API do Google.
 * Processa a entrada, permanência e saída de zonas de alta demanda (Hotspots),
 * notificando o piloto via comandos de voz e integrando com o limite de velocidade configurado no .env.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.example.ACTION_GEOFENCE_EVENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "GeofencingEvent recebido é nulo.")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Erro no GeofencingEvent: código ${geofencingEvent.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

        if (triggeringGeofences.isEmpty()) {
            Log.d(TAG, "Nenhuma geofence disparada encontrada no evento.")
            return
        }

        val currentSpeed = RadarCoordinator.currentSpeedKmh.value
        val speedLimitEnv = resolveMaxSpeedLimitEnv()

        for (geofence in triggeringGeofences) {
            val zoneId = geofence.requestId
            val demandZone = DemandZonesCatalog.DEFAULT_ZONES.find { it.id == zoneId }
                ?: DemandZone(
                    id = zoneId,
                    name = "Polo de Alta Demanda",
                    latitude = 0.0,
                    longitude = 0.0,
                    surgeMultiplier = 1.50
                )

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    handleGeofenceEnter(context, demandZone, currentSpeed, speedLimitEnv)
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    handleGeofenceDwell(context, demandZone, currentSpeed, speedLimitEnv)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    handleGeofenceExit(context, demandZone)
                }
                else -> {
                    Log.d(TAG, "Transição de Geofence desconhecida: $transitionType")
                }
            }
        }
    }

    /**
     * Resolve o limite de velocidade máximo seguro configurado no .env via BuildConfig
     */
    private fun resolveMaxSpeedLimitEnv(): Float {
        return try {
            BuildConfig.MAX_SPEED_LIMIT_KMH.toFloatOrNull() ?: 40.0f
        } catch (e: Throwable) {
            40.0f
        }
    }

    private fun handleGeofenceEnter(
        context: Context,
        zone: DemandZone,
        currentSpeedKmh: Float,
        speedLimitKmh: Float
    ) {
        val isSpeeding = currentSpeedKmh > speedLimitKmh
        val appsList = zone.primaryApps.joinToString(" e ")
        val multiplierText = String.format("%.2f", zone.surgeMultiplier).replace(".", ",")

        val voiceMessage = if (isSpeeding) {
            "Atenção piloto! Você entrou no ${zone.name}, com tarifa dinâmica de ${multiplierText}x no $appsList. Atenção: sua velocidade de ${currentSpeedKmh.toInt()} km por hora ultrapassa o limite seguro de ${speedLimitKmh.toInt()} km por hora do sistema. Reduza para sua segurança!"
        } else {
            "Radar Jarvis: Entrando no ${zone.name}. Alta demanda de pedidos no $appsList com multiplicador de ${multiplierText}x. Jarvis pronto para capturar as melhores rotas encadeadas."
        }

        Log.i(TAG, "GEOFENCE ENTER: ${zone.name} | Vel: $currentSpeedKmh km/h | Limite: $speedLimitKmh km/h | Fala: $voiceMessage")
        dispatchVoiceAlert(voiceMessage)

        // Sincroniza telemetria da zona ativa com o Firestore
        syncGeofenceEventToFirestore(zone, "ENTER", currentSpeedKmh, isSpeeding)
    }

    private fun handleGeofenceDwell(
        context: Context,
        zone: DemandZone,
        currentSpeedKmh: Float,
        speedLimitKmh: Float
    ) {
        Log.d(TAG, "GEOFENCE DWELL: Permanecendo no ${zone.name}")
        val isSpeeding = currentSpeedKmh > speedLimitKmh
        if (isSpeeding) {
            dispatchVoiceAlert("Atenção! Você continua pilotando a ${currentSpeedKmh.toInt()} km por hora na zona ${zone.name}. Reduza para abaixo de ${speedLimitKmh.toInt()} km por hora.")
        }
    }

    private fun handleGeofenceExit(context: Context, zone: DemandZone) {
        val voiceMessage = "Você saiu do ${zone.name}. Jarvis continuando monitoramento de rotas nas áreas adjacentes."
        Log.i(TAG, "GEOFENCE EXIT: ${zone.name}")
        dispatchVoiceAlert(voiceMessage)
        syncGeofenceEventToFirestore(zone, "EXIT", RadarCoordinator.currentSpeedKmh.value, false)
    }

    private fun dispatchVoiceAlert(message: String) {
        try {
            RadarCoordinator.voiceManager?.speak(message)
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao sintetizar voz via RadarCoordinator: ${e.message}")
        }
    }

    private fun syncGeofenceEventToFirestore(
        zone: DemandZone,
        eventType: String,
        speedKmh: Float,
        speedViolation: Boolean
    ) {
        try {
            val speedStatus = if (speedViolation) "EXCESSO_VELOCIDADE" else "VELOCIDADE_SEGURA"
            val logMessage = "Geofence $eventType: ${zone.name} | Multiplicador: ${zone.surgeMultiplier}x | Vel: ${speedKmh.toInt()} km/h ($speedStatus)"
            FirestoreManager.addCloudLog(logMessage, if (speedViolation) "ALERT" else "INFO")
        } catch (e: Throwable) {
            Log.w(TAG, "Falha ao registrar evento de geofence no Firestore: ${e.message}")
        }
    }
}
