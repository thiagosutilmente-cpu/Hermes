package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Ação tomada pelo entregador (Aceite ou Rejeição).
 */
enum class DecisionAction(val label: String, val icon: String, val colorHex: Long) {
    ACCEPTED("Aceita", "✅", 0xFF00FF88),
    DECLINED("Rejeitada", "❌", 0xFFFF4757)
}

/**
 * Modelo de dados de um evento de decisão de oferta registrado no sistema interno de log.
 */
data class OfferDecisionLog(
    val id: String = UUID.randomUUID().toString(),
    val offerId: String,
    val appName: String,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val gainPerKm: Double,
    val action: DecisionAction,
    val reason: String,
    val source: String,
    val timestampFormatted: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Gerenciador interno de logs de ofertas (Aceitação e Rejeição).
 * Mantém o histórico reativo na UI e persistido no armazenamento local.
 */
object OfferDecisionLogManager {

    private const val PREFS_NAME = "radar_decision_logs_prefs"
    private const val KEY_LOGS_JSON = "key_decision_logs_json"

    // Lista observável pelo Jetpack Compose para recomposições imediatas
    val logs = mutableStateListOf<OfferDecisionLog>()

    private var isInitialized = false

    /**
     * Inicializa os logs a partir do SharedPreferences.
     * Caso esteja vazio, preenche com um conjunto inicial de eventos realistas.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_LOGS_JSON, null)

        logs.clear()
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    logs.add(
                        OfferDecisionLog(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            offerId = obj.optString("offerId", "stk_00"),
                            appName = obj.optString("appName", "iFood"),
                            restaurant = obj.optString("restaurant", "Restaurante"),
                            value = obj.optDouble("value", 0.0),
                            distanceKm = obj.optDouble("distanceKm", 0.0),
                            gainPerKm = obj.optDouble("gainPerKm", 0.0),
                            action = if (obj.optString("action") == "ACCEPTED") DecisionAction.ACCEPTED else DecisionAction.DECLINED,
                            reason = obj.optString("reason", "Decisão manual"),
                            source = obj.optString("source", "Toque Manual"),
                            timestampFormatted = obj.optString("timestampFormatted", "Hoje"),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadDefaultLogs()
            }
        } else {
            loadDefaultLogs()
            persistLogs(prefs)
        }
    }

    /**
     * Registra evento de aceitação de oferta.
     */
    fun logAccept(
        context: Context,
        offer: RadarOffer,
        reason: String = "Ganho/km vantajoso",
        source: String = "Toque na Tela"
    ): OfferDecisionLog {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val item = OfferDecisionLog(
            offerId = offer.id,
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            gainPerKm = offer.gainPerKm,
            action = DecisionAction.ACCEPTED,
            reason = reason,
            source = source,
            timestampFormatted = timeFormat.format(Date())
        )
        logs.add(0, item)
        saveToDisk(context)
        return item
    }

    /**
     * Registra evento de rejeição de oferta.
     */
    fun logDecline(
        context: Context,
        offer: RadarOffer,
        reason: String = "Distância/valor não compensatório",
        source: String = "Toque na Tela"
    ): OfferDecisionLog {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val item = OfferDecisionLog(
            offerId = offer.id,
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            gainPerKm = offer.gainPerKm,
            action = DecisionAction.DECLINED,
            reason = reason,
            source = source,
            timestampFormatted = timeFormat.format(Date())
        )
        logs.add(0, item)
        saveToDisk(context)
        return item
    }

    /**
     * Limpa todo o histórico de logs.
     */
    fun clearLogs(context: Context) {
        logs.clear()
        saveToDisk(context)
    }

    /**
     * Métricas agregadas simples do histórico.
     */
    fun getTotalAccepted(): Int = logs.count { it.action == DecisionAction.ACCEPTED }
    fun getTotalDeclined(): Int = logs.count { it.action == DecisionAction.DECLINED }
    fun getTotalDecisions(): Int = logs.size

    fun getAcceptanceRate(): Double {
        if (logs.isEmpty()) return 0.0
        return (getTotalAccepted().toDouble() / logs.size.toDouble()) * 100.0
    }

    fun getTotalAcceptedValue(): Double {
        return logs.filter { it.action == DecisionAction.ACCEPTED }.sumOf { it.value }
    }

    fun getAverageGainPerKmAccepted(): Double {
        val accepted = logs.filter { it.action == DecisionAction.ACCEPTED }
        if (accepted.isEmpty()) return 0.0
        return accepted.map { it.gainPerKm }.average()
    }

    private fun saveToDisk(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        persistLogs(prefs)
    }

    private fun persistLogs(prefs: SharedPreferences) {
        try {
            val array = JSONArray()
            // Limitar a no máximo 150 registros para alta performance
            val subset = logs.take(150)
            for (log in subset) {
                val obj = JSONObject()
                obj.put("id", log.id)
                obj.put("offerId", log.offerId)
                obj.put("appName", log.appName)
                obj.put("restaurant", log.restaurant)
                obj.put("value", log.value)
                obj.put("distanceKm", log.distanceKm)
                obj.put("gainPerKm", log.gainPerKm)
                obj.put("action", log.action.name)
                obj.put("reason", log.reason)
                obj.put("source", log.source)
                obj.put("timestampFormatted", log.timestampFormatted)
                obj.put("timestampMillis", log.timestampMillis)
                array.put(obj)
            }
            prefs.edit().putString(KEY_LOGS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadDefaultLogs() {
        val initialEvents = listOf(
            OfferDecisionLog(
                offerId = "stk_seed_01",
                appName = "iFood + Rappi",
                restaurant = "Burger King Faria Lima & Pizza Hut",
                value = 33.00,
                distanceKm = 4.2,
                gainPerKm = 7.86,
                action = DecisionAction.ACCEPTED,
                reason = "Ganho/km acima da média (R$ 7.86/km)",
                source = "Comando de Voz",
                timestampFormatted = "14:15:02"
            ),
            OfferDecisionLog(
                offerId = "stk_seed_02",
                appName = "Uber Eats",
                restaurant = "McDonald's Henrique Schaumann",
                value = 11.50,
                distanceKm = 6.8,
                gainPerKm = 1.69,
                action = DecisionAction.DECLINED,
                reason = "Distância excessiva (>6km) e R$/km baixo",
                source = "IA Jarvis Decision",
                timestampFormatted = "14:02:40"
            ),
            OfferDecisionLog(
                offerId = "stk_seed_03",
                appName = "Rappi + iFood",
                restaurant = "Madero Prime & Bacio di Latte",
                value = 36.00,
                distanceKm = 4.8,
                gainPerKm = 7.50,
                action = DecisionAction.ACCEPTED,
                reason = "Stack multi-app com alta rentabilidade",
                source = "Toque na Tela",
                timestampFormatted = "13:48:19"
            ),
            OfferDecisionLog(
                offerId = "stk_seed_04",
                appName = "99 Food",
                restaurant = "Habib's Rebouças",
                value = 13.00,
                distanceKm = 5.2,
                gainPerKm = 2.50,
                action = DecisionAction.DECLINED,
                reason = "Ganho/km abaixo da linha de corte",
                source = "Toque na Tela",
                timestampFormatted = "13:22:11"
            ),
            OfferDecisionLog(
                offerId = "stk_seed_05",
                appName = "iFood",
                restaurant = "Outback Center 3",
                value = 22.00,
                distanceKm = 3.2,
                gainPerKm = 6.88,
                action = DecisionAction.ACCEPTED,
                reason = "Distância curta compensa",
                source = "Navegação Maps",
                timestampFormatted = "12:55:44"
            )
        )
        logs.addAll(initialEvents)
    }
}
