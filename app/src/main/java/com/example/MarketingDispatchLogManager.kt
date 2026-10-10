package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Modelo de dados de um registro histórico de disparo de marketing.
 */
data class MarketingDispatchLog(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val timestampFormatted: String,
    val scenarioName: String,
    val profitBrl: Double,
    val distanceKm: Double,
    val ratePerKm: Double = if (distanceKm > 0) profitBrl / distanceKm else 0.0,
    val fuelSavedBrl: Double,
    val notificationTitle: String,
    val notificationBody: String,
    val deliveryStatus: String,
    val isSuccess: Boolean = true,
    val channelId: String = "jarvis_simulated_marketing_channel"
)

/**
 * Gerenciador de persistência para o histórico de disparos do MarketingDashboard.
 * Utiliza SharedPreferences para armazenamento local durável e lista reativa para Jetpack Compose.
 */
object MarketingDispatchLogManager {

    private const val PREFS_NAME = "jarvis_marketing_dispatch_logs_prefs"
    private const val KEY_LOGS_JSON = "key_marketing_dispatch_logs_json"
    private const val MAX_LOGS = 60

    // Lista observável pelo Jetpack Compose para atualizações instantâneas de UI
    val logs = mutableStateListOf<MarketingDispatchLog>()

    private var isInitialized = false

    /**
     * Inicializa o gerenciador e carrega os registros do SharedPreferences.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_LOGS_JSON, null)

        logs.clear()
        if (!jsonString.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    logs.add(parseLogFromJson(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Se for a primeira execução e não houver logs salvos, preenche com histórico demonstrativo realista
        if (logs.isEmpty()) {
            loadInitialDemoLogs(context)
        }

        isInitialized = true
    }

    private fun loadInitialDemoLogs(context: Context) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

        val demo1 = MarketingDispatchLog(
            id = UUID.randomUUID().toString(),
            timestampMillis = now - 3600000L * 2, // 2 horas atrás
            timestampFormatted = sdf.format(Date(now - 3600000L * 2)),
            scenarioName = "Super Sexta",
            profitBrl = 310.0,
            distanceKm = 48.0,
            ratePerKm = 310.0 / 48.0,
            fuelSavedBrl = 45.60,
            notificationTitle = "⛽ Jarvis: R$ 6,46/km batido hoje! (Lucro: R$ 310,00)",
            notificationBody = "🔥 Telemetria de Hoje: Você rodou 48.0 km com lucro líquido de R$ 310,00 (R$ 6,46/km)! Economia estimada em combustível: R$ 45,60 descartando corridas sem margem.",
            deliveryStatus = "Entregue (Heads-Up)",
            isSuccess = true
        )

        val demo2 = MarketingDispatchLog(
            id = UUID.randomUUID().toString(),
            timestampMillis = now - 3600000L * 25, // Ontem
            timestampFormatted = sdf.format(Date(now - 3600000L * 25)),
            scenarioName = "Padrão",
            profitBrl = 180.0,
            distanceKm = 38.0,
            ratePerKm = 180.0 / 38.0,
            fuelSavedBrl = 36.10,
            notificationTitle = "⛽ Jarvis: R$ 4,74/km batido hoje! (Lucro: R$ 180,00)",
            notificationBody = "🔥 Telemetria de Hoje: Você rodou 38.0 km com lucro líquido de R$ 180,00 (R$ 4,74/km)! Economia estimada em combustível: R$ 36,10.",
            deliveryStatus = "Entregue (Heads-Up)",
            isSuccess = true
        )

        val demo3 = MarketingDispatchLog(
            id = UUID.randomUUID().toString(),
            timestampMillis = now - 3600000L * 50, // 2 dias atrás
            timestampFormatted = sdf.format(Date(now - 3600000L * 50)),
            scenarioName = "Chuva Alta Demanda",
            profitBrl = 440.0,
            distanceKm = 58.0,
            ratePerKm = 440.0 / 58.0,
            fuelSavedBrl = 55.10,
            notificationTitle = "⛽ Jarvis: R$ 7,59/km batido hoje! (Lucro: R$ 440,00)",
            notificationBody = "🔥 Telemetria de Hoje: Você rodou 58.0 km com lucro líquido de R$ 440,00 (R$ 7,59/km)! Economia estimada em combustível: R$ 55,10.",
            deliveryStatus = "Entregue (Push FCM)",
            isSuccess = true
        )

        logs.addAll(listOf(demo1, demo2, demo3))
        saveLogsToPrefs(context)
    }

    /**
     * Registra um novo disparo de marketing no log persistente.
     */
    fun recordDispatch(
        context: Context,
        scenarioName: String,
        profitBrl: Double,
        distanceKm: Double,
        ratePerKm: Double,
        fuelSavedBrl: Double,
        notificationTitle: String,
        notificationBody: String,
        deliveryStatus: String = "Entregue (Heads-Up)",
        isSuccess: Boolean = true
    ): MarketingDispatchLog {
        if (!isInitialized) initialize(context)

        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val log = MarketingDispatchLog(
            id = UUID.randomUUID().toString(),
            timestampMillis = now,
            timestampFormatted = sdf.format(Date(now)),
            scenarioName = scenarioName,
            profitBrl = profitBrl,
            distanceKm = distanceKm,
            ratePerKm = ratePerKm,
            fuelSavedBrl = fuelSavedBrl,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody,
            deliveryStatus = deliveryStatus,
            isSuccess = isSuccess
        )

        // Adiciona no topo da lista (ordem cronológica inversa)
        logs.add(0, log)
        if (logs.size > MAX_LOGS) {
            logs.removeRange(MAX_LOGS, logs.size)
        }

        saveLogsToPrefs(context)
        return log
    }

    /**
     * Limpa todo o histórico de logs persistido.
     */
    fun clearLogs(context: Context) {
        logs.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGS_JSON).apply()
    }

    private fun saveLogsToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (log in logs) {
            array.put(formatLogToJson(log))
        }
        prefs.edit().putString(KEY_LOGS_JSON, array.toString()).apply()
    }

    private fun formatLogToJson(log: MarketingDispatchLog): JSONObject {
        return JSONObject().apply {
            put("id", log.id)
            put("timestampMillis", log.timestampMillis)
            put("timestampFormatted", log.timestampFormatted)
            put("scenarioName", log.scenarioName)
            put("profitBrl", log.profitBrl)
            put("distanceKm", log.distanceKm)
            put("ratePerKm", log.ratePerKm)
            put("fuelSavedBrl", log.fuelSavedBrl)
            put("notificationTitle", log.notificationTitle)
            put("notificationBody", log.notificationBody)
            put("deliveryStatus", log.deliveryStatus)
            put("isSuccess", log.isSuccess)
            put("channelId", log.channelId)
        }
    }

    private fun parseLogFromJson(obj: JSONObject): MarketingDispatchLog {
        return MarketingDispatchLog(
            id = obj.optString("id", UUID.randomUUID().toString()),
            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
            timestampFormatted = obj.optString("timestampFormatted", ""),
            scenarioName = obj.optString("scenarioName", "Personalizado"),
            profitBrl = obj.optDouble("profitBrl", 0.0),
            distanceKm = obj.optDouble("distanceKm", 0.0),
            ratePerKm = obj.optDouble("ratePerKm", 0.0),
            fuelSavedBrl = obj.optDouble("fuelSavedBrl", 0.0),
            notificationTitle = obj.optString("notificationTitle", ""),
            notificationBody = obj.optString("notificationBody", ""),
            deliveryStatus = obj.optString("deliveryStatus", "Entregue (Heads-Up)"),
            isSuccess = obj.optBoolean("isSuccess", true),
            channelId = obj.optString("channelId", "jarvis_simulated_marketing_channel")
        )
    }
}
