package com.example

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Critérios pré-definidos de lucratividade para a Resposta Automática (Auto-Aceite).
 */
data class AutoAcceptCriteria(
    val isEnabled: Boolean = true,
    val minGainPerKm: Double = 4.50, // Padrão de R$ 4,50/km (acima da média das plataformas)
    val minValueBrl: Double = 16.00, // Valor mínimo da corrida em R$
    val maxDistanceKm: Double = 5.5, // Distância máxima aceitável em km
    val minHourlyRateBrl: Double = 45.00, // Rendimento horário estimado mínimo (R$/h)
    val allowWhileMoving: Boolean = true, // Permite aceitar em movimento sem que o piloto toque na tela
    val reactionDelayMs: Long = 1200L, // Delay de segurança antes do aceite automático (permite cancelar se desejar)
    val enabledPlatforms: Set<String> = setOf("iFood", "Uber", "99 Moto", "Rappi"),
    val requireHighDemandZone: Boolean = false // Se true, apenas ofertas originadas em zonas quentes
)

/**
 * Registro de corrida aceita automaticamente pela IA.
 */
data class AutoAcceptedRideRecord(
    val id: String = UUID.randomUUID().toString(),
    val offerId: String,
    val appName: String,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val gainPerKm: Double,
    val estimatedHourlyRate: Double,
    val reason: String,
    val timestampFormatted: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Resultado da avaliação de lucratividade.
 */
data class AutoAcceptEvaluation(
    val isApproved: Boolean,
    val reason: String,
    val gainPerKm: Double,
    val estimatedHourlyRate: Double,
    val isUltraLucrative: Boolean
)

/**
 * UltraProfitAutoAcceptManager
 *
 * Gerenciador autônomo responsável pela resposta automática para aceitação de corridas
 * baseada em critérios pré-definidos de lucratividade.
 *
 * Funcionalidades:
 * 1. Avalia ofertas recebidas (notificações, OCR de tela, WebSocket da VPS, FCM).
 * 2. Verifica se a corrida é classificada como 'ultra-lucrativa' (R$/km elevado, rota curta e alta rentabilidade horária).
 * 3. Se aprovada e com auto-aceite ativado:
 *    - Inicia contagem regressiva de segurança tática (1 a 2 segundos).
 *    - Executa o clique de aceitação no aplicativo nativo via [JarvisAccessibilityService.performAcceptClick].
 *    - Dispara notificação por síntese de voz (TTS) para o fone do capacete.
 *    - Emite assinatura física vibratória no guidão ([HapticFeedbackHelper.vibrateAccept]).
 *    - Registra no histórico auditável e sincroniza com o backend.
 * 4. Permite operação segura em trânsito (acima de 10 km/h) sem necessitar que o condutor tire as mãos do guidão.
 */
class UltraProfitAutoAcceptManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AutoAcceptManager"
        private const val PREFS_NAME = "ultra_profit_auto_accept_prefs"

        private const val KEY_ENABLED = "pref_auto_accept_enabled"
        private const val KEY_MIN_GAIN_KM = "pref_min_gain_per_km"
        private const val KEY_MIN_VALUE = "pref_min_value_brl"
        private const val KEY_MAX_DISTANCE = "pref_max_distance_km"
        private const val KEY_MIN_HOURLY = "pref_min_hourly_rate"
        private const val KEY_ALLOW_MOVING = "pref_allow_while_moving"
        private const val KEY_REACTION_DELAY = "pref_reaction_delay_ms"
        private const val KEY_PLATFORMS = "pref_enabled_platforms"
        private const val KEY_STATS_COUNT = "pref_stats_accepted_count"
        private const val KEY_STATS_PROFIT = "pref_stats_total_profit"
        private const val KEY_HISTORY_JSON = "pref_recent_history_json"

        @Volatile
        private var instance: UltraProfitAutoAcceptManager? = null

        fun getInstance(context: Context): UltraProfitAutoAcceptManager {
            return instance ?: synchronized(this) {
                instance ?: UltraProfitAutoAcceptManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Critérios ativos
    private val _criteria = MutableStateFlow(loadCriteriaFromPrefs())
    val criteria: StateFlow<AutoAcceptCriteria> = _criteria.asStateFlow()

    // Estatísticas ao vivo
    private val _totalAcceptedCount = MutableStateFlow(prefs.getInt(KEY_STATS_COUNT, 0))
    val totalAcceptedCount: StateFlow<Int> = _totalAcceptedCount.asStateFlow()

    private val _totalProfitGeneratedBrl = MutableStateFlow(prefs.getFloat(KEY_STATS_PROFIT, 0f).toDouble())
    val totalProfitGeneratedBrl: StateFlow<Double> = _totalProfitGeneratedBrl.asStateFlow()

    // Histórico recente das últimas corridas aceitas pelo robô
    private val _recentAutoAcceptedRides = MutableStateFlow<List<AutoAcceptedRideRecord>>(loadHistoryFromPrefs())
    val recentAutoAcceptedRides: StateFlow<List<AutoAcceptedRideRecord>> = _recentAutoAcceptedRides.asStateFlow()

    // Estado da oferta atualmente em contagem regressiva de auto-aceite
    private val _pendingOffer = MutableStateFlow<RadarOffer?>(null)
    val pendingOffer: StateFlow<RadarOffer?> = _pendingOffer.asStateFlow()

    private val _countdownRemainingMs = MutableStateFlow<Long>(0L)
    val countdownRemainingMs: StateFlow<Long> = _countdownRemainingMs.asStateFlow()

    private var activeCountdownRunnable: Runnable? = null
    private var pendingCallback: ((RadarOffer, String) -> Unit)? = null

    /**
     * Avalia uma oferta contra os critérios de ultra-lucratividade.
     */
    fun evaluateOffer(offer: RadarOffer): AutoAcceptEvaluation {
        val currentCriteria = _criteria.value
        val gainPerKm = if (offer.distanceKm > 0) offer.value / offer.distanceKm else offer.value
        val timeMin = if (offer.timeMinutes > 0) offer.timeMinutes else 15
        val hourlyRate = (offer.value / timeMin) * 60.0

        val passesGain = gainPerKm >= currentCriteria.minGainPerKm
        val passesValue = offer.value >= currentCriteria.minValueBrl
        val passesDistance = offer.distanceKm <= currentCriteria.maxDistanceKm
        val passesHourly = hourlyRate >= currentCriteria.minHourlyRateBrl
        val passesPlatform = currentCriteria.enabledPlatforms.contains(offer.appName) ||
                currentCriteria.enabledPlatforms.any { offer.appName.contains(it, ignoreCase = true) }

        val isApproved = currentCriteria.isEnabled && passesGain && passesValue && passesDistance && passesHourly && passesPlatform

        val reason = when {
            !currentCriteria.isEnabled -> "Auto-Aceite desativado nas preferências"
            !passesPlatform -> "Plataforma ${offer.appName} desmarcada nos filtros"
            !passesGain -> "Ganho de ${formatBrl(gainPerKm)}/km abaixo do mínimo de ${formatBrl(currentCriteria.minGainPerKm)}/km"
            !passesValue -> "Valor de ${formatBrl(offer.value)} abaixo do mínimo de ${formatBrl(currentCriteria.minValueBrl)}"
            !passesDistance -> "Distância de ${String.format(Locale.ROOT, "%.1f", offer.distanceKm)}km excede o limite de ${currentCriteria.maxDistanceKm}km"
            !passesHourly -> "Rendimento estimado de ${formatBrl(hourlyRate)}/h abaixo do limiar de ${formatBrl(currentCriteria.minHourlyRateBrl)}/h"
            else -> "Super-Lucrativa: ${formatBrl(gainPerKm)}/km • ${formatBrl(hourlyRate)}/h estimada!"
        }

        val isUltra = gainPerKm >= (currentCriteria.minGainPerKm * 1.15) || gainPerKm >= 5.50

        return AutoAcceptEvaluation(
            isApproved = isApproved,
            reason = reason,
            gainPerKm = gainPerKm,
            estimatedHourlyRate = hourlyRate,
            isUltraLucrative = isUltra
        )
    }

    /**
     * Submete uma oferta para processamento automático.
     * Retorna true se a oferta foi aceita ou agendada para aceite imediato.
     */
    fun processIncomingOffer(
        offer: RadarOffer,
        isCurrentlyMoving: Boolean,
        onAcceptedCallback: (RadarOffer, String) -> Unit
    ): Boolean {
        val currentCriteria = _criteria.value
        if (!currentCriteria.isEnabled) return false

        // Se o condutor estiver em movimento e a flag de segurança proibir aceite em deslocamento
        if (isCurrentlyMoving && !currentCriteria.allowWhileMoving) {
            Log.d(TAG, "Oferta rejeitada para auto-aceite: moto em movimento e permissão desativada")
            return false
        }

        val eval = evaluateOffer(offer)
        if (!eval.isApproved) {
            Log.d(TAG, "Oferta ${offer.id} não cumpre critérios: ${eval.reason}")
            return false
        }

        Log.i(TAG, "🚀 OFERTA ULTRA-LUCRATIVA IDENTIFICADA: ${offer.restaurant} (${formatBrl(offer.value)} - ${eval.gainPerKm} R$/km)")

        // Se houver delay configurado, inicia contagem regressiva tática
        if (currentCriteria.reactionDelayMs > 0) {
            startCountdown(offer, currentCriteria.reactionDelayMs, onAcceptedCallback)
        } else {
            executeAcceptanceNow(offer, onAcceptedCallback)
        }
        return true
    }

    /**
     * Inicia a contagem regressiva de segurança (permitindo cancelamento manual se o motoboy desejar).
     */
    private fun startCountdown(
        offer: RadarOffer,
        delayMs: Long,
        onAcceptedCallback: (RadarOffer, String) -> Unit
    ) {
        cancelPendingCountdown()

        _pendingOffer.value = offer
        _countdownRemainingMs.value = delayMs
        pendingCallback = onAcceptedCallback

        val startTime = System.currentTimeMillis()
        val intervalMs = 100L

        activeCountdownRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = (delayMs - elapsed).coerceAtLeast(0L)
                _countdownRemainingMs.value = remaining

                if (remaining <= 0L) {
                    _pendingOffer.value = null
                    _countdownRemainingMs.value = 0L
                    executeAcceptanceNow(offer, onAcceptedCallback)
                } else {
                    mainHandler.postDelayed(this, intervalMs)
                }
            }
        }
        mainHandler.post(activeCountdownRunnable!!)
    }

    /**
     * Cancela qualquer contagem regressiva de auto-aceite em andamento.
     */
    fun cancelPendingCountdown() {
        activeCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        activeCountdownRunnable = null
        _pendingOffer.value = null
        _countdownRemainingMs.value = 0L
        pendingCallback = null
    }

    /**
     * Executa o aceite imediato da oferta com clique nativo por acessibilidade, TTS, vibração e log.
     */
    fun executeAcceptanceNow(
        offer: RadarOffer,
        onAcceptedCallback: (RadarOffer, String) -> Unit
    ) {
        cancelPendingCountdown()

        val eval = evaluateOffer(offer)
        val sourceLabel = "Auto-Aceite Piloto Automático (${formatBrl(eval.gainPerKm)}/km)"

        // 1. Tenta acionar clique nativo no botão da tela através do Serviço de Acessibilidade
        val clickedNative = JarvisAccessibilityService.performAcceptClick()
        Log.i(TAG, "Clique de acessibilidade nativo executado: $clickedNative")

        // 2. Dispara feedback háptico duplo confirmativo
        HapticFeedbackHelper.vibrateAccept(context)

        // 3. Sintetiza áudio via TTS para o capacete
        val ttsMsg = "Corrida ultra lucrativa aceita no piloto automático! ${offer.restaurant}, ${formatBrl(offer.value)} por ${String.format(Locale.ROOT, "%.1f", offer.distanceKm)} quilômetros no ${offer.appName}."
        try {
            TextToSpeechManager.getInstance(context).speak(ttsMsg, priority = true)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao emitir aviso de voz TTS: ${e.message}")
        }

        // 4. Invoca o callback principal do app para atualizar ganhos, mapa, Room e VPS
        onAcceptedCallback(offer, sourceLabel)

        // 5. Atualiza métricas estatísticas
        val newCount = _totalAcceptedCount.value + 1
        val newProfit = _totalProfitGeneratedBrl.value + offer.netProfit
        _totalAcceptedCount.value = newCount
        _totalProfitGeneratedBrl.value = newProfit

        prefs.edit()
            .putInt(KEY_STATS_COUNT, newCount)
            .putFloat(KEY_STATS_PROFIT, newProfit.toFloat())
            .apply()

        // 6. Registra no histórico recente do robô
        val record = AutoAcceptedRideRecord(
            offerId = offer.id,
            appName = offer.appName,
            restaurant = offer.restaurant,
            value = offer.value,
            distanceKm = offer.distanceKm,
            gainPerKm = eval.gainPerKm,
            estimatedHourlyRate = eval.estimatedHourlyRate,
            reason = eval.reason,
            timestampFormatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        )

        val updatedHistory = listOf(record) + _recentAutoAcceptedRides.value.take(19)
        _recentAutoAcceptedRides.value = updatedHistory
        saveHistoryToPrefs(updatedHistory)
    }

    /**
     * Atualiza e persiste os critérios configurados pelo usuário.
     */
    fun updateCriteria(newCriteria: AutoAcceptCriteria) {
        _criteria.value = newCriteria
        prefs.edit()
            .putBoolean(KEY_ENABLED, newCriteria.isEnabled)
            .putFloat(KEY_MIN_GAIN_KM, newCriteria.minGainPerKm.toFloat())
            .putFloat(KEY_MIN_VALUE, newCriteria.minValueBrl.toFloat())
            .putFloat(KEY_MAX_DISTANCE, newCriteria.maxDistanceKm.toFloat())
            .putFloat(KEY_MIN_HOURLY, newCriteria.minHourlyRateBrl.toFloat())
            .putBoolean(KEY_ALLOW_MOVING, newCriteria.allowWhileMoving)
            .putLong(KEY_REACTION_DELAY, newCriteria.reactionDelayMs)
            .putStringSet(KEY_PLATFORMS, newCriteria.enabledPlatforms)
            .apply()
    }

    /**
     * Limpa o histórico e reinicia estatísticas
     */
    fun resetStatistics() {
        _totalAcceptedCount.value = 0
        _totalProfitGeneratedBrl.value = 0.0
        _recentAutoAcceptedRides.value = emptyList()
        prefs.edit()
            .putInt(KEY_STATS_COUNT, 0)
            .putFloat(KEY_STATS_PROFIT, 0f)
            .remove(KEY_HISTORY_JSON)
            .apply()
    }

    /**
     * Gera uma oferta simulada ultra-lucrativa para testes imediatos pelo usuário.
     */
    fun generateUltraProfitableTestOffer(): RadarOffer {
        val platforms = listOf("iFood", "Uber", "99 Moto", "Rappi")
        val app = platforms.random()
        val distance = (2.4 + (Math.random() * 2.2)) // 2.4 a 4.6 km
        val gainPerKm = (5.20 + (Math.random() * 2.10)) // R$ 5.20 a 7.30 / km
        val value = (distance * gainPerKm).coerceAtLeast(18.50)

        val restaurants = listOf(
            "Outback Steakhouse • Frei Caneca",
            "Madero Prime Burger • Jardins",
            "Pecorino Cucina • Al. Santos",
            "Z Deli Sandwiches • Pinheiros",
            "Bullguer Artesanal • Vila Madalena",
            "Paris 6 Bistrô • Haddock Lobo"
        )

        return RadarOffer(
            id = "auto_${System.currentTimeMillis() % 100000}",
            appName = app,
            restaurant = restaurants.random(),
            value = Math.round(value * 100.0) / 100.0,
            distanceKm = Math.round(distance * 10.0) / 10.0,
            timeMinutes = (distance * 3.5).toInt().coerceAtLeast(10),
            pickupAddress = "Balcão Rápido / Retirada Imediata",
            destinationAddress = "Rota Curta Residencial VIP",
            isMultiStack = false,
            neuralDecision = NeuralDecision(
                decision = "accept",
                confidence = 0.98,
                reason = "Gatilho de Ultra-Lucratividade atingido: R$ ${String.format(Locale("pt", "BR"), "%.2f", gainPerKm)}/km"
            )
        )
    }

    private fun loadCriteriaFromPrefs(): AutoAcceptCriteria {
        return AutoAcceptCriteria(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            minGainPerKm = prefs.getFloat(KEY_MIN_GAIN_KM, 4.50f).toDouble(),
            minValueBrl = prefs.getFloat(KEY_MIN_VALUE, 16.00f).toDouble(),
            maxDistanceKm = prefs.getFloat(KEY_MAX_DISTANCE, 5.5f).toDouble(),
            minHourlyRateBrl = prefs.getFloat(KEY_MIN_HOURLY, 45.00f).toDouble(),
            allowWhileMoving = prefs.getBoolean(KEY_ALLOW_MOVING, true),
            reactionDelayMs = prefs.getLong(KEY_REACTION_DELAY, 1200L),
            enabledPlatforms = prefs.getStringSet(KEY_PLATFORMS, setOf("iFood", "Uber", "99 Moto", "Rappi")) ?: setOf("iFood", "Uber", "99 Moto", "Rappi")
        )
    }

    private fun loadHistoryFromPrefs(): List<AutoAcceptedRideRecord> {
        val json = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val list = mutableListOf<AutoAcceptedRideRecord>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AutoAcceptedRideRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        offerId = obj.optString("offerId", ""),
                        appName = obj.optString("appName", "iFood"),
                        restaurant = obj.optString("restaurant", ""),
                        value = obj.optDouble("value", 0.0),
                        distanceKm = obj.optDouble("distanceKm", 0.0),
                        gainPerKm = obj.optDouble("gainPerKm", 0.0),
                        estimatedHourlyRate = obj.optDouble("estimatedHourlyRate", 0.0),
                        reason = obj.optString("reason", ""),
                        timestampFormatted = obj.optString("timestampFormatted", ""),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistoryToPrefs(list: List<AutoAcceptedRideRecord>) {
        try {
            val array = JSONArray()
            for (item in list.take(20)) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("offerId", item.offerId)
                    put("appName", item.appName)
                    put("restaurant", item.restaurant)
                    put("value", item.value)
                    put("distanceKm", item.distanceKm)
                    put("gainPerKm", item.gainPerKm)
                    put("estimatedHourlyRate", item.estimatedHourlyRate)
                    put("reason", item.reason)
                    put("timestampFormatted", item.timestampFormatted)
                    put("timestampMillis", item.timestampMillis)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_HISTORY_JSON, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar histórico de auto-aceite: ${e.message}")
        }
    }

    private fun formatBrl(value: Double): String {
        return String.format(Locale("pt", "BR"), "R$ %.2f", value)
    }
}
