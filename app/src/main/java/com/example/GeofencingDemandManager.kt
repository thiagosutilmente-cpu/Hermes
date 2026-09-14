package com.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modelo de dados representando uma Zona de Alta Demanda (Hotspot de pedidos).
 */
data class HighDemandZone(
    val id: String,
    val name: String,
    val category: String, // "Fast-Food", "Pizzaria / Noturno", "Shopping / Centro Comercial", "Dark Kitchen"
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 600f, // Raio padrão de 600 metros de geofence
    val averageOrdersPerHour: Int = 48,
    val estimatedBonusMultiplier: Double = 1.35, // Multiplicador de tarifa dinâmica estimada
    val primaryPartnerApp: String = "iFood + Rappi",
    val description: String = "Alta concentração de pedidos expressos e rotas curtas",
    val surgeBonusPercent: Int = ((estimatedBonusMultiplier - 1.0) * 100).toInt()
)

/**
 * Evento de transição da Geofence disparado pela API de Localização.
 */
data class GeofenceTransitionEvent(
    val zone: HighDemandZone,
    val transitionType: Int, // Geofence.GEOFENCE_TRANSITION_ENTER, EXIT, DWELL
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Estado de proximidade do entregador em relação aos polos de alta demanda.
 */
data class ProximityHotspotState(
    val closestZone: HighDemandZone? = null,
    val distanceMeters: Float = 0f,
    val isInside: Boolean = false,
    val isApproaching: Boolean = false // < 350m
)

/**
 * Gerenciador de Geofencing para Zonas de Alta Demanda (Hotspots do Entregador).
 *
 * Utiliza o [GeofencingClient] da Google Play Services para registrar cercas geográficas virtuais
 * ao redor dos principais pólos gastronômicos e comerciais de alta rotação.
 *
 * Quando o entregador adentra o raio da zona (ENTER ou DWELL), o sistema notifica em tempo real,
 * reproduz aviso por voz (Jarvis / Mãos Livres), emite feedback tátil e destaca a zona no radar.
 */
class GeofencingDemandManager private constructor(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        private const val TAG = "GeofencingDemandManager"
        const val ACTION_GEOFENCE_EVENT = "com.example.action.GEOFENCE_EVENT"
        const val NOTIFICATION_CHANNEL_GEOFENCE = "channel_radar_demand_geofence"
        const val NOTIFICATION_ID_GEOFENCE = 5002

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: GeofencingDemandManager? = null

        fun getInstance(context: Context): GeofencingDemandManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GeofencingDemandManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Zonas de Alta Demanda pré-configuradas (Pólos gastronômicos chave em São Paulo)
        val DEFAULT_HOTSPOTS = listOf(
            HighDemandZone(
                id = "zone_paulista_bk",
                name = "Polo Paulista • Av. Paulista / Frei Caneca",
                category = "Fast-Food & Dark Kitchen",
                latitude = -23.561684,
                longitude = -46.655981,
                radiusMeters = 650f,
                averageOrdersPerHour = 62,
                estimatedBonusMultiplier = 1.45,
                primaryPartnerApp = "iFood + Rappi",
                description = "Burger King, Starbucks, Habib's e Shopping Center 3 com alta taxa de chamadas"
            ),
            HighDemandZone(
                id = "zone_jardins_alameda",
                name = "Gastronomia Jardins • Al. Santos / Augusta",
                category = "Pizzarias & Gourmet",
                latitude = -23.564210,
                longitude = -46.652150,
                radiusMeters = 550f,
                averageOrdersPerHour = 45,
                estimatedBonusMultiplier = 1.30,
                primaryPartnerApp = "Rappi + iFood",
                description = "Pizza Hut, Bullguer e restaurantes noturnos com ticket médio elevado"
            ),
            HighDemandZone(
                id = "zone_pinheiros_teodoro",
                name = "Hub Pinheiros • Teodoro / Fradique",
                category = "Bares & Lanches Rápidos",
                latitude = -23.567890,
                longitude = -46.684120,
                radiusMeters = 700f,
                averageOrdersPerHour = 54,
                estimatedBonusMultiplier = 1.40,
                primaryPartnerApp = "Uber Direct + iFood",
                description = "Fluxo contínuo de entregas locais para Pinheiros e Vila Madalena"
            ),
            HighDemandZone(
                id = "zone_faria_lima_prime",
                name = "Faria Lima Corporativo • Itaim / JK",
                category = "Almoço Executivo & Cafeterias",
                latitude = -23.585120,
                longitude = -46.681530,
                radiusMeters = 800f,
                averageOrdersPerHour = 78,
                estimatedBonusMultiplier = 1.50,
                primaryPartnerApp = "iFood + Rappi + 99",
                description = "Maior densidade de pedidos corporativos durante turnos de pico"
            ),
            HighDemandZone(
                id = "zone_morumbi_shopping",
                name = "Complexo Morumbi • Chácara Sto Antônio",
                category = "Shoppings & Grandes Redes",
                latitude = -23.623100,
                longitude = -46.698900,
                radiusMeters = 900f,
                averageOrdersPerHour = 50,
                estimatedBonusMultiplier = 1.25,
                primaryPartnerApp = "iFood + Uber Direct",
                description = "Outback, Madero e praças de alimentação com pedidos acumulados"
            )
        )

        // Estado reativo da zona ativa no momento
        private val _currentActiveZone = MutableStateFlow<HighDemandZone?>(null)
        val currentActiveZone: StateFlow<HighDemandZone?> = _currentActiveZone.asStateFlow()

        // Histórico de transições ocorridas
        private val _zoneTransitions = MutableSharedFlow<GeofenceTransitionEvent>(extraBufferCapacity = 10)
        val zoneTransitions: SharedFlow<GeofenceTransitionEvent> = _zoneTransitions.asSharedFlow()

        // Lista de todas as zonas monitoradas
        private val _registeredZones = MutableStateFlow<List<HighDemandZone>>(DEFAULT_HOTSPOTS)
        val registeredZones: StateFlow<List<HighDemandZone>> = _registeredZones.asStateFlow()

        // Estado de proximidade e aproximação
        private val _proximityState = MutableStateFlow(ProximityHotspotState())
        val proximityState: StateFlow<ProximityHotspotState> = _proximityState.asStateFlow()

        // Estado de ativação do Geofencing
        private val _isGeofencingActive = MutableStateFlow(false)
        val isGeofencingActive: StateFlow<Boolean> = _isGeofencingActive.asStateFlow()

        /**
         * Disparado internamente pelo BroadcastReceiver ou pelo simulador de geofence.
         */
        fun onTransitionDetected(zoneId: String, transitionType: Int) {
            val zone = _registeredZones.value.find { it.id == zoneId } ?: return
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    _currentActiveZone.value = zone
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    if (_currentActiveZone.value?.id == zoneId) {
                        _currentActiveZone.value = null
                    }
                }
            }
            _zoneTransitions.tryEmit(
                GeofenceTransitionEvent(
                    zone = zone,
                    transitionType = transitionType
                )
            )
        }

        /**
         * Identifica se um endereço de coleta faz parte de algum polo de alta demanda monitorado.
         */
        fun matchZoneByAddress(address: String): HighDemandZone? {
            val clean = address.lowercase()
            return _registeredZones.value.firstOrNull { zone ->
                when (zone.id) {
                    "zone_paulista_bk" -> clean.contains("paulista") || clean.contains("frei caneca") || clean.contains("center 3")
                    "zone_jardins_alameda" -> clean.contains("jardins") || clean.contains("santos") || clean.contains("augusta") || clean.contains("oscar freire")
                    "zone_pinheiros_teodoro" -> clean.contains("pinheiros") || clean.contains("teodoro") || clean.contains("fradique")
                    "zone_faria_lima_prime" -> clean.contains("faria lima") || clean.contains("itaim") || clean.contains("jk")
                    "zone_morumbi_shopping" -> clean.contains("morumbi") || clean.contains("chácara") || clean.contains("chucri")
                    else -> false
                }
            }
        }

        /**
         * Retorna o multiplicador de ganho dinâmico aplicado ao pedido caso esteja em uma zona quente.
         */
        fun getEffectiveSurgeMultiplier(pickupAddress: String): Double {
            val active = _currentActiveZone.value
            if (active != null) return active.estimatedBonusMultiplier
            val matched = matchZoneByAddress(pickupAddress)
            return matched?.estimatedBonusMultiplier ?: 1.0
        }
    }

    init {
        createGeofenceNotificationChannel(context)
    }

    private fun createGeofenceNotificationChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_GEOFENCE,
                "Radar Zonas de Alta Demanda (Geofencing)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações instantâneas ao adentrar ou aproximar-se de polos com tarifa dinâmica elevada."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 300)
            }
            val notificationManager = ctx.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Registra as cercas virtuais das zonas de alta demanda na API de Geofencing do Android.
     */
    @SuppressLint("MissingPermission")
    fun registerHotspotGeofences(zones: List<HighDemandZone> = DEFAULT_HOTSPOTS) {
        try {
            _registeredZones.value = zones
            val geofenceList = zones.map { zone ->
                Geofence.Builder()
                    .setRequestId(zone.id)
                    .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT or
                        Geofence.GEOFENCE_TRANSITION_DWELL
                    )
                    .setLoiteringDelay(12000) // 12 segundos dentro da zona para confirmar permanência (DWELL)
                    .setNotificationResponsiveness(3000)
                    .build()
            }

            val request = GeofencingRequest.Builder().apply {
                setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
                addGeofences(geofenceList)
            }.build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    _isGeofencingActive.value = true
                    Log.d(TAG, "Geofences de alta demanda registradas com sucesso: ${zones.size} zonas ativas.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Falha ao registrar Geofences na Google Play Services API", e)
                    // Ativa modo local/passivo mesmo se o serviço de nuvem estiver temporariamente offline
                    _isGeofencingActive.value = true
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissão de localização ausente ao adicionar Geofences", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado ao registrar geofences", e)
        }
    }

    /**
     * Remove o monitoramento das cercas geográficas virtuais.
     */
    fun unregisterHotspotGeofences() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    _isGeofencingActive.value = false
                    _currentActiveZone.value = null
                    Log.d(TAG, "Geofences removidas com sucesso.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Falha ao remover Geofences", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao remover Geofences", e)
        }
    }

    /**
     * Avalia a distância do usuário para todas as zonas monitoradas com base na posição GPS atual
     * retornada pelo FusedLocationProviderClient.
     */
    fun evaluateCurrentLocation(latitude: Double, longitude: Double) {
        var foundInsideZone: HighDemandZone? = null
        var closestZone: HighDemandZone? = null
        var minDistance = Float.MAX_VALUE

        for (zone in _registeredZones.value) {
            val dist = FloatArray(1)
            Location.distanceBetween(latitude, longitude, zone.latitude, zone.longitude, dist)
            val d = dist[0]
            if (d < minDistance) {
                minDistance = d
                closestZone = zone
            }
            if (d <= zone.radiusMeters) {
                foundInsideZone = zone
            }
        }

        // Atualiza estado de proximidade
        val isInside = foundInsideZone != null
        val isApproaching = !isInside && minDistance <= (closestZone?.radiusMeters ?: 600f) + 350f
        _proximityState.value = ProximityHotspotState(
            closestZone = closestZone,
            distanceMeters = minDistance,
            isInside = isInside,
            isApproaching = isApproaching
        )

        val previous = _currentActiveZone.value
        if (foundInsideZone != null && previous?.id != foundInsideZone.id) {
            _currentActiveZone.value = foundInsideZone
            _zoneTransitions.tryEmit(
                GeofenceTransitionEvent(
                    zone = foundInsideZone,
                    transitionType = Geofence.GEOFENCE_TRANSITION_ENTER
                )
            )
            Log.d(TAG, "Entregador adentrou a zona de alta demanda: ${foundInsideZone.name}")
        } else if (foundInsideZone == null && previous != null) {
            val oldZone = previous
            _currentActiveZone.value = null
            _zoneTransitions.tryEmit(
                GeofenceTransitionEvent(
                    zone = oldZone,
                    transitionType = Geofence.GEOFENCE_TRANSITION_EXIT
                )
            )
            Log.d(TAG, "Entregador saiu da zona de alta demanda: ${oldZone.name}")
        }
    }

    /**
     * Permite simular a entrada do entregador em uma zona para testes em ambiente de desenvolvimento / emulador.
     */
    fun simulateEnterZone(zoneId: String) {
        val zone = _registeredZones.value.find { it.id == zoneId } ?: return
        _currentActiveZone.value = zone
        _proximityState.value = ProximityHotspotState(
            closestZone = zone,
            distanceMeters = 50f,
            isInside = true,
            isApproaching = false
        )
        _zoneTransitions.tryEmit(
            GeofenceTransitionEvent(
                zone = zone,
                transitionType = Geofence.GEOFENCE_TRANSITION_ENTER
            )
        )
    }

    /**
     * Simula a saída de qualquer zona ativa.
     */
    fun simulateExitZone() {
        val zone = _currentActiveZone.value ?: return
        _currentActiveZone.value = null
        _proximityState.value = ProximityHotspotState(
            closestZone = zone,
            distanceMeters = 1200f,
            isInside = false,
            isApproaching = false
        )
        _zoneTransitions.tryEmit(
            GeofenceTransitionEvent(
                zone = zone,
                transitionType = Geofence.GEOFENCE_TRANSITION_EXIT
            )
        )
    }
}

/**
 * [BroadcastReceiver] que recebe os eventos disparados em background pela Google Play Services Geofencing API.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent nulo recebido.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Erro na Geofencing API: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()

        for (geofence in triggeringGeofences) {
            val requestId = geofence.requestId
            Log.d(TAG, "Geofence disparada: requestId=$requestId, transition=$geofenceTransition")
            GeofencingDemandManager.onTransitionDetected(requestId, geofenceTransition)

            val zone = GeofencingDemandManager.DEFAULT_HOTSPOTS.find { it.id == requestId }
            if (zone != null) {
                postNotification(context, zone, geofenceTransition)
                triggerHapticFeedback(context, geofenceTransition)
            }
        }
    }

    private fun postNotification(context: Context, zone: HighDemandZone, transition: Int) {
        val isEnter = transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL
        val title = if (isEnter) {
            "🔥 Alta Demanda: ${zone.name}"
        } else {
            "📡 Saiu do Polo: ${zone.name}"
        }

        val text = if (isEnter) {
            "Tarifa dinâmica +${zone.surgeBonusPercent}% ativa • ~${zone.averageOrdersPerHour} ped/h (${zone.primaryPartnerApp})"
        } else {
            "Retornando ao regime normal de tarifas. Monitore novos polos no Radar."
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GeofencingDemandManager.NOTIFICATION_CHANNEL_GEOFENCE)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(GeofencingDemandManager.NOTIFICATION_ID_GEOFENCE, notification)
    }

    private fun triggerHapticFeedback(context: Context, transition: Int) {
        try {
            val isEnter = transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (isEnter) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 180), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 180, 100, 180), -1)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (_: Exception) {}
    }
}

