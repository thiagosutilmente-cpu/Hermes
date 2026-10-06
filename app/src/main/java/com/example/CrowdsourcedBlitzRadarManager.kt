package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Categorias de Alertas Comunitários
 */
enum class HazardType(val label: String, val icon: String, val ttsWord: String) {
    POLICE_BLITZ("Blitz Policial", "🚔", "Blitz policial de fiscalização"),
    SPEED_RADAR("Radar Móvel", "📸", "Radar móvel de velocidade"),
    HEAVY_RAIN("Chuva / Alagamento", "🌧️", "Ponto de alagamento e chuva torrencial"),
    DANGER_ZONE("Trecho Perigoso", "⚠️", "Área de risco e roubos frequentes")
}

/**
 * Registro de alerta comunitário reportado por entregadores
 */
data class HazardReport(
    val id: String,
    val type: HazardType,
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val confirmationsCount: Int = 1,
    val distanceMeters: Int = 0
)

/**
 * CrowdsourcedBlitzRadarManager
 *
 * Gerenciador comunitário de alertas em tempo real no estilo "Waze para Motoboys".
 * Permite marcar e monitorar blitzes policiais, viaturas de trânsito, chuvas e áreas de risco.
 * Quando o piloto se aproxima de um ponto reportado recentemente, dispara alarme no fone (TTS).
 */
class CrowdsourcedBlitzRadarManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BlitzRadarManager"

        @Volatile
        private var INSTANCE: CrowdsourcedBlitzRadarManager? = null

        fun getInstance(context: Context): CrowdsourcedBlitzRadarManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrowdsourcedBlitzRadarManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _activeHazards = MutableStateFlow<List<HazardReport>>(emptyList())
    val activeHazards: StateFlow<List<HazardReport>> = _activeHazards.asStateFlow()

    private val _closestHazard = MutableStateFlow<HazardReport?>(null)
    val closestHazard: StateFlow<HazardReport?> = _closestHazard.asStateFlow()

    private val alertedHazardIds = mutableSetOf<String>()

    init {
        loadDefaultSampleHazards()
    }

    private fun loadDefaultSampleHazards() {
        val now = System.currentTimeMillis()
        val defaultList = listOf(
            HazardReport(
                id = "blitz_01",
                type = HazardType.POLICE_BLITZ,
                title = "Blitz Lei Seca / PM",
                description = "Comando fiscalizando escapamento e baú",
                address = "Av. 23 de Maio, próximo ao Viaduto Santa Generosa",
                latitude = -23.5781,
                longitude = -46.6432,
                timestamp = now - 12 * 60 * 1000,
                confirmationsCount = 8
            ),
            HazardReport(
                id = "blitz_02",
                type = HazardType.POLICE_BLITZ,
                title = "Fiscalização CET / PM",
                description = "Parando motos sentido centro",
                address = "Radial Leste • Altura Metrô Tatuapé",
                latitude = -23.5398,
                longitude = -46.5765,
                timestamp = now - 24 * 60 * 1000,
                confirmationsCount = 14
            ),
            HazardReport(
                id = "rain_01",
                type = HazardType.HEAVY_RAIN,
                title = "Pista Escorregadia / Poça",
                description = "Faixa da direita com lâmina d'água perigosa",
                address = "Marginal Pinheiros • Próx. Ponte Eusébio Matoso",
                latitude = -23.5684,
                longitude = -46.7011,
                timestamp = now - 35 * 60 * 1000,
                confirmationsCount = 5
            ),
            HazardReport(
                id = "danger_01",
                type = HazardType.DANGER_ZONE,
                title = "Atenção: Garupa suspeita",
                description = "Tentativa de furto de celular na calçada",
                address = "Rua Bela Cintra cruzamento com Santos",
                latitude = -23.5572,
                longitude = -46.6625,
                timestamp = now - 18 * 60 * 1000,
                confirmationsCount = 11
            )
        )
        _activeHazards.value = defaultList
    }

    /**
     * Reporta um novo ponto de blitz ou perigo na coordenada atual
     */
    fun reportHazard(
        type: HazardType,
        latitude: Double,
        longitude: Double,
        address: String,
        description: String = ""
    ): HazardReport {
        val newReport = HazardReport(
            id = "hazard_${System.currentTimeMillis()}",
            type = type,
            title = type.label,
            description = description.ifBlank { "Reportado por entregador na rota" },
            address = address.ifBlank { "Coordenada Atual" },
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            confirmationsCount = 1
        )

        val updated = listOf(newReport) + _activeHazards.value
        _activeHazards.value = updated

        try {
            HapticFeedbackHelper.vibrateSuccess(context)
            val tts = OfferTextToSpeechEngine.getInstance(context)
            tts.speak("Alerta de ${type.label} registrado e compartilhado no radar comunitário com sucesso!")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar reporte: ${e.message}")
        }

        return newReport
    }

    /**
     * Confirma um alerta existente (aumenta relevância comunitária)
     */
    fun confirmHazard(hazardId: String) {
        val updated = _activeHazards.value.map {
            if (it.id == hazardId) it.copy(confirmationsCount = it.confirmationsCount + 1) else it
        }
        _activeHazards.value = updated
        try {
            HapticFeedbackHelper.vibrateTap(context)
        } catch (_: Exception) {}
    }

    /**
     * Atualiza a posição do entregador e verifica proximidade de alertas.
     * Se estiver a menos de 800m de uma blitz ativa, dispara aviso sonoro/TTS.
     */
    fun updateLocationAndCheckProximity(driverLat: Double, driverLng: Double) {
        val hazards = _activeHazards.value
        if (hazards.isEmpty()) return

        val withDistances = hazards.map { hazard ->
            val distMeters = calculateDistanceMeters(driverLat, driverLng, hazard.latitude, hazard.longitude)
            hazard.copy(distanceMeters = distMeters)
        }.sortedBy { it.distanceMeters }

        _activeHazards.value = withDistances
        val nearest = withDistances.firstOrNull()
        _closestHazard.value = nearest

        // Alerta de proximidade (raio de 800 metros)
        if (nearest != null && nearest.distanceMeters in 50..800) {
            if (!alertedHazardIds.contains(nearest.id)) {
                alertedHazardIds.add(nearest.id)
                triggerProximityAudioWarning(nearest)
            }
        }
    }

    private fun triggerProximityAudioWarning(hazard: HazardReport) {
        try {
            HapticFeedbackHelper.vibrateWarning(context)
            val dist = hazard.distanceMeters
            val tts = OfferTextToSpeechEngine.getInstance(context)
            val speech = "Atenção piloto! ${hazard.type.ttsWord} a aproximadamente $dist metros na ${hazard.address}. Reduza a velocidade e redobre a atenção!"
            tts.speak(speech)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao vocalizar alerta de blitz: ${e.message}")
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371000.0 // Raio da Terra em metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toInt()
    }
}
