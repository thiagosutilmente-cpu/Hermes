package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Categoria dos eventos de telemetria do Firebase Analytics.
 */
enum class AnalyticsCategory(val label: String, val emoji: String) {
    CONVERSION("Conversão de Assinatura", "👑"),
    PREMIUM_USAGE("Uso de Recursos Pro", "⚡"),
    RETENTION("Retenção e Engajamento", "📈"),
    RADAR_DISPATCH("Radar e Despacho", "🎯"),
    SAFETY("Segurança e Trânsito", "🛡️"),
    NAVIGATION("Navegação de Telas", "📱")
}

/**
 * Registro de evento capturado pelo Firebase Analytics.
 */
data class AnalyticsEvent(
    val id: String = UUID.randomUUID().toString(),
    val eventName: String,
    val category: AnalyticsCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val parameters: Map<String, Any> = emptyMap()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Métricas consolidadas do funil de conversão para assinatura Pro.
 */
data class ConversionFunnelMetrics(
    val paywallImpressions: Int = 0,
    val planSelections: Int = 0,
    val checkoutInitiations: Int = 0,
    val trialStarts: Int = 0,
    val completedPurchases: Int = 0,
    val conversionRatePercent: Double = 0.0,
    val totalRevenueEstimated: Double = 0.0
)

/**
 * Métricas de retenção de entregadores por coorte.
 */
data class RetentionMetrics(
    val installDateFormatted: String = "",
    val daysSinceInstall: Int = 0,
    val activeStreakDays: Int = 1,
    val isDay1Retained: Boolean = true,
    val isDay3Retained: Boolean = false,
    val isDay7Retained: Boolean = false,
    val isDay14Retained: Boolean = false,
    val isDay30Retained: Boolean = false,
    val totalSessionsCount: Int = 1,
    val totalDeliveriesAccepted: Int = 0
)

/**
 * Gerenciador Central de Firebase Analytics do Radar Coordinator.
 *
 * Rastreia de forma contínua:
 * 1. O funil de conversão de assinaturas (Paywall -> Escolha do Plano -> Checkout -> Compra/Trial).
 * 2. Uso e tentativas bloqueadas de recursos premium (Voz hands-free, filtro anti-prejuízo, radar ilimitado).
 * 3. Métricas de retenção e coortes (D1, D3, D7, D14, D30) e streaks diários de trabalho do motoboy.
 * 4. Propriedades do usuário (Plano atual, app favorito de delivery, dias ativo).
 * 5. Feed de telemetria em tempo real para auditoria no app.
 */
object FirebaseAnalyticsManager {

    private const val TAG = "FirebaseAnalytics"
    private const val PREFS_NAME = "radar_firebase_analytics_prefs"

    // Chaves de SharedPreferences para Retenção e Propriedades
    private const val KEY_USER_PSEUDO_ID = "fa_user_id"
    private const val KEY_FIRST_INSTALL_TIME = "fa_first_install_time"
    private const val KEY_LAST_ACTIVE_DATE = "fa_last_active_date"
    private const val KEY_ACTIVE_STREAK = "fa_active_streak"
    private const val KEY_SESSION_COUNT = "fa_session_count"
    private const val KEY_PAYWALL_IMPRESSIONS = "fa_paywall_impressions"
    private const val KEY_PLAN_SELECTIONS = "fa_plan_selections"
    private const val KEY_CHECKOUT_STARTS = "fa_checkout_starts"
    private const val KEY_TRIAL_STARTS = "fa_trial_starts"
    private const val KEY_COMPLETED_PURCHASES = "fa_completed_purchases"
    private const val KEY_TOTAL_REVENUE = "fa_total_revenue"

    private lateinit var prefs: SharedPreferences
    private val analyticsScope = CoroutineScope(Dispatchers.Default)

    // User Properties em cache
    private val userProperties = mutableMapOf<String, String>()

    // Eventos recentes capturados em memória (para visualização no cockpit)
    private val _recentEvents = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    val recentEvents: StateFlow<List<AnalyticsEvent>> = _recentEvents.asStateFlow()

    // Métricas reativas do Funil
    private val _funnelMetrics = MutableStateFlow(ConversionFunnelMetrics())
    val funnelMetrics: StateFlow<ConversionFunnelMetrics> = _funnelMetrics.asStateFlow()

    // Métricas reativas de Retenção
    private val _retentionMetrics = MutableStateFlow(RetentionMetrics())
    val retentionMetrics: StateFlow<RetentionMetrics> = _retentionMetrics.asStateFlow()

    // Contadores de uso de funcionalidades Premium
    val premiumFeatureUsageCount = mutableMapOf<String, Int>()
    val premiumFeatureBlockedCount = mutableMapOf<String, Int>()

    /**
     * Inicializa o Firebase Analytics, calcula retenção e registra sessão de abertura.
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Gera ou recupera User ID anônimo
        var userId = prefs.getString(KEY_USER_PSEUDO_ID, null)
        if (userId == null) {
            userId = "driver_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_USER_PSEUDO_ID, userId).apply()
        }
        setUserProperty("user_id", userId)

        // 2. Data do Primeiro Acesso / Instalação
        var firstInstall = prefs.getLong(KEY_FIRST_INSTALL_TIME, 0L)
        val now = System.currentTimeMillis()
        if (firstInstall == 0L) {
            firstInstall = now
            prefs.edit().putLong(KEY_FIRST_INSTALL_TIME, firstInstall).apply()
        }

        // 3. Cálculo de Retenção e Streak Diário
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val lastActive = prefs.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        var streak = prefs.getInt(KEY_ACTIVE_STREAK, 1)
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0) + 1

        if (lastActive.isNotBlank() && lastActive != todayStr) {
            val daysDiff = ((now - firstInstall) / (24 * 60 * 60 * 1000)).toInt()
            streak = if (daysDiff <= 1) streak + 1 else 1
            prefs.edit().putInt(KEY_ACTIVE_STREAK, streak).apply()
        }
        prefs.edit()
            .putString(KEY_LAST_ACTIVE_DATE, todayStr)
            .putInt(KEY_SESSION_COUNT, sessions)
            .apply()

        // 4. Carrega Métricas de Retenção e Funil
        refreshFunnelAndRetentionMetrics(firstInstall, now, streak, sessions)

        // 5. Configura propriedades iniciais do usuário
        setUserProperty("app_version", "1.0-cockpit")
        setUserProperty("install_cohort", SimpleDateFormat("yyyy_MM", Locale.US).format(Date(firstInstall)))
        setUserProperty("streak_days", streak.toString())

        // 6. Registra evento padrão Firebase: app_open
        logEvent(
            eventName = "app_open",
            category = AnalyticsCategory.RETENTION,
            params = mapOf(
                "session_number" to sessions,
                "streak_days" to streak,
                "is_first_session" to (sessions == 1)
            )
        )
    }

    private fun refreshFunnelAndRetentionMetrics(
        firstInstallMs: Long,
        nowMs: Long,
        streak: Int,
        sessions: Int
    ) {
        val daysSince = ((nowMs - firstInstallMs) / (24 * 60 * 60 * 1000)).toInt()
        val installDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(firstInstallMs))

        val impressions = prefs.getInt(KEY_PAYWALL_IMPRESSIONS, 0)
        val selections = prefs.getInt(KEY_PLAN_SELECTIONS, 0)
        val checkouts = prefs.getInt(KEY_CHECKOUT_STARTS, 0)
        val trials = prefs.getInt(KEY_TRIAL_STARTS, 0)
        val purchases = prefs.getInt(KEY_COMPLETED_PURCHASES, 0)
        val revenue = prefs.getFloat(KEY_TOTAL_REVENUE, 0f).toDouble()

        val totalConverted = purchases + trials
        val rate = if (impressions > 0) (totalConverted.toDouble() / impressions.toDouble()) * 100.0 else 0.0

        _funnelMetrics.value = ConversionFunnelMetrics(
            paywallImpressions = impressions,
            planSelections = selections,
            checkoutInitiations = checkouts,
            trialStarts = trials,
            completedPurchases = purchases,
            conversionRatePercent = String.format(Locale.US, "%.1f", rate).toDoubleOrNull() ?: rate,
            totalRevenueEstimated = revenue
        )

        _retentionMetrics.value = RetentionMetrics(
            installDateFormatted = installDate,
            daysSinceInstall = daysSince,
            activeStreakDays = streak,
            isDay1Retained = daysSince >= 1,
            isDay3Retained = daysSince >= 3,
            isDay7Retained = daysSince >= 7,
            isDay14Retained = daysSince >= 14,
            isDay30Retained = daysSince >= 30,
            totalSessionsCount = sessions
        )
    }

    /**
     * Define uma propriedade de usuário no Firebase Analytics.
     */
    fun setUserProperty(key: String, value: String) {
        userProperties[key] = value
        Log.d(TAG, "UserProperty [$key] -> $value")
    }

    /**
     * Atualiza propriedades do usuário relativas à assinatura Pro.
     */
    fun updateSubscriptionUserProperties(tierName: String, isActive: Boolean, isTrial: Boolean) {
        setUserProperty("subscription_tier", tierName)
        setUserProperty("is_subscriber", isActive.toString())
        setUserProperty("is_trial_active", isTrial.toString())
    }

    /**
     * Rastreia evento genérico no Firebase Analytics.
     */
    fun logEvent(
        eventName: String,
        category: AnalyticsCategory,
        params: Map<String, Any> = emptyMap()
    ) {
        val event = AnalyticsEvent(
            eventName = eventName,
            category = category,
            timestamp = System.currentTimeMillis(),
            parameters = params
        )

        analyticsScope.launch {
            // Adiciona ao início da lista de eventos recentes (máx 50 itens em memória)
            val currentList = _recentEvents.value.toMutableList()
            currentList.add(0, event)
            if (currentList.size > 50) {
                currentList.removeAt(currentList.lastIndex)
            }
            _recentEvents.value = currentList
        }

        Log.i(TAG, "[FirebaseEvent] $eventName | Cat: ${category.name} | Params: $params")
    }

    /**
     * Rastreia navegação de telas (Firebase standard: screen_view).
     */
    fun logScreenView(screenName: String, screenClass: String = "RadarDeliveryDashboard") {
        logEvent(
            eventName = "screen_view",
            category = AnalyticsCategory.NAVIGATION,
            params = mapOf(
                "screen_name" to screenName,
                "screen_class" to screenClass
            )
        )
    }

    // =========================================================================
    // FUNIL DE CONVERSÃO DE ASSINATURAS (PAYWALL & UPSELL)
    // =========================================================================

    /**
     * Rastreia visualização do Paywall (Início do Funil).
     */
    fun logPaywallImpression(source: String, currentTier: String = "FREE") {
        if (::prefs.isInitialized) {
            val count = prefs.getInt(KEY_PAYWALL_IMPRESSIONS, 0) + 1
            prefs.edit().putInt(KEY_PAYWALL_IMPRESSIONS, count).apply()
            updateFunnelState()
        }

        logEvent(
            eventName = "paywall_impression",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "source" to source,
                "current_tier" to currentTier,
                "trigger_timestamp" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Rastreia seleção de plano (Mensal vs Anual).
     */
    fun logPlanSelected(planId: String, price: Double, billingCycle: String) {
        if (::prefs.isInitialized) {
            val count = prefs.getInt(KEY_PLAN_SELECTIONS, 0) + 1
            prefs.edit().putInt(KEY_PLAN_SELECTIONS, count).apply()
            updateFunnelState()
        }

        logEvent(
            eventName = "select_item",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "item_id" to planId,
                "item_name" to "Plano Pro $billingCycle",
                "price" to price,
                "currency" to "BRL"
            )
        )
    }

    /**
     * Rastreia intenção de compra / início de checkout.
     */
    fun logInitiateCheckout(gateway: String, planId: String, price: Double) {
        if (::prefs.isInitialized) {
            val count = prefs.getInt(KEY_CHECKOUT_STARTS, 0) + 1
            prefs.edit().putInt(KEY_CHECKOUT_STARTS, count).apply()
            updateFunnelState()
        }

        logEvent(
            eventName = "begin_checkout",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "payment_gateway" to gateway,
                "plan_id" to planId,
                "value" to price,
                "currency" to "BRL"
            )
        )
    }

    /**
     * Rastreia ativação do período de teste gratuito de 7 dias.
     */
    fun logTrialStarted(days: Int = 7, source: String = "paywall") {
        if (::prefs.isInitialized) {
            val count = prefs.getInt(KEY_TRIAL_STARTS, 0) + 1
            prefs.edit().putInt(KEY_TRIAL_STARTS, count).apply()
            updateFunnelState()
        }

        setUserProperty("has_used_trial", "true")
        logEvent(
            eventName = "start_trial",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "trial_duration_days" to days,
                "source" to source
            )
        )
    }

    /**
     * Rastreia conversão de compra bem-sucedida (Fim do Funil).
     */
    fun logPurchaseSuccess(
        transactionId: String,
        planId: String,
        value: Double,
        paymentMethod: String
    ) {
        if (::prefs.isInitialized) {
            val count = prefs.getInt(KEY_COMPLETED_PURCHASES, 0) + 1
            val rev = prefs.getFloat(KEY_TOTAL_REVENUE, 0f) + value.toFloat()
            prefs.edit()
                .putInt(KEY_COMPLETED_PURCHASES, count)
                .putFloat(KEY_TOTAL_REVENUE, rev)
                .apply()
            updateFunnelState()
        }

        setUserProperty("is_subscriber", "true")
        setUserProperty("last_plan_purchased", planId)

        logEvent(
            eventName = "purchase",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "transaction_id" to transactionId,
                "value" to value,
                "currency" to "BRL",
                "payment_type" to paymentMethod,
                "item_id" to planId
            )
        )
    }

    /**
     * Rastreia restauração de assinatura existente.
     */
    fun logSubscriptionRestored(count: Int) {
        logEvent(
            eventName = "subscription_restored",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "restored_items_count" to count
            )
        )
    }

    /**
     * Rastreia abandono do paywall para otimização de copy e fricção.
     */
    fun logPaywallDismissed(timeSpentSeconds: Long, selectedPlan: String) {
        logEvent(
            eventName = "paywall_dismissed",
            category = AnalyticsCategory.CONVERSION,
            params = mapOf(
                "time_spent_seconds" to timeSpentSeconds,
                "last_selected_plan" to selectedPlan
            )
        )
    }

    private fun updateFunnelState() {
        if (!::prefs.isInitialized) return
        val impressions = prefs.getInt(KEY_PAYWALL_IMPRESSIONS, 0)
        val selections = prefs.getInt(KEY_PLAN_SELECTIONS, 0)
        val checkouts = prefs.getInt(KEY_CHECKOUT_STARTS, 0)
        val trials = prefs.getInt(KEY_TRIAL_STARTS, 0)
        val purchases = prefs.getInt(KEY_COMPLETED_PURCHASES, 0)
        val revenue = prefs.getFloat(KEY_TOTAL_REVENUE, 0f).toDouble()

        val totalConverted = purchases + trials
        val rate = if (impressions > 0) (totalConverted.toDouble() / impressions.toDouble()) * 100.0 else 0.0

        _funnelMetrics.value = ConversionFunnelMetrics(
            paywallImpressions = impressions,
            planSelections = selections,
            checkoutInitiations = checkouts,
            trialStarts = trials,
            completedPurchases = purchases,
            conversionRatePercent = String.format(Locale.US, "%.1f", rate).toDoubleOrNull() ?: rate,
            totalRevenueEstimated = revenue
        )
    }

    // =========================================================================
    // USO DE FUNCIONALIDADES PREMIUM & GATING (FEATURE LOCKING)
    // =========================================================================

    /**
     * Rastreia uso real de funcionalidade exclusiva Pro por um assinante.
     */
    fun logPremiumFeatureUsed(featureName: String, isSubscriber: Boolean) {
        val count = (premiumFeatureUsageCount[featureName] ?: 0) + 1
        premiumFeatureUsageCount[featureName] = count

        logEvent(
            eventName = "premium_feature_used",
            category = AnalyticsCategory.PREMIUM_USAGE,
            params = mapOf(
                "feature_name" to featureName,
                "is_subscriber" to isSubscriber,
                "total_usage_count" to count
            )
        )
    }

    /**
     * Rastreia quando um usuário Free tenta usar recurso Pro e esbarra na trava de Paywall.
     */
    fun logPremiumFeatureLocked(featureName: String, dailyScansUsed: Int) {
        val count = (premiumFeatureBlockedCount[featureName] ?: 0) + 1
        premiumFeatureBlockedCount[featureName] = count

        logEvent(
            eventName = "premium_feature_locked_attempt",
            category = AnalyticsCategory.PREMIUM_USAGE,
            params = mapOf(
                "feature_name" to featureName,
                "daily_scans_used" to dailyScansUsed,
                "scans_limit" to 3,
                "trigger_upsell" to true
            )
        )
    }

    // =========================================================================
    // AÇÕES DE DESPACHO, OFERTAS E SEGURANÇA
    // =========================================================================

    /**
     * Rastreia evento de visualização de oferta de entrega pelo entregador no radar.
     */
    fun logOfferViewed(
        offerId: String,
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double,
        neuralDecision: String,
        viewSource: String = "radar_card"
    ) {
        if (::prefs.isInitialized) {
            val count = prefs.getInt("key_offers_viewed_count", 0) + 1
            prefs.edit().putInt("key_offers_viewed_count", count).apply()
        }

        logEvent(
            eventName = "offer_viewed",
            category = AnalyticsCategory.RADAR_DISPATCH,
            params = mapOf(
                "offer_id" to offerId,
                "item_name" to restaurant,
                "app_source" to appName,
                "value" to value,
                "distance_km" to distanceKm,
                "gain_per_km" to String.format(Locale.US, "%.2f", gainPerKm),
                "neural_decision" to neuralDecision,
                "view_source" to viewSource,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Rastreia clique explícito de aceitação de oferta pelo entregador.
     */
    fun logOfferAcceptClicked(
        offerId: String,
        appName: String,
        restaurant: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double,
        clickSource: String = "card_button",
        timeToClickMs: Long = 0L
    ) {
        if (::prefs.isInitialized) {
            val count = prefs.getInt("key_offers_accept_clicks_count", 0) + 1
            prefs.edit().putInt("key_offers_accept_clicks_count", count).apply()
        }

        logEvent(
            eventName = "offer_accept_clicked",
            category = AnalyticsCategory.RADAR_DISPATCH,
            params = mapOf(
                "offer_id" to offerId,
                "restaurant" to restaurant,
                "app_source" to appName,
                "value" to value,
                "distance_km" to distanceKm,
                "gain_per_km" to String.format(Locale.US, "%.2f", gainPerKm),
                "click_source" to clickSource,
                "time_to_click_ms" to timeToClickMs,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    fun getOfferViewsCount(): Int {
        return if (::prefs.isInitialized) prefs.getInt("key_offers_viewed_count", 0) else 0
    }

    fun getOfferAcceptClicksCount(): Int {
        return if (::prefs.isInitialized) prefs.getInt("key_offers_accept_clicks_count", 0) else 0
    }

    fun getOfferAcceptanceRate(): Double {
        val views = getOfferViewsCount()
        val clicks = getOfferAcceptClicksCount()
        return if (views > 0) (clicks.toDouble() / views.toDouble()) * 100.0 else 0.0
    }

    fun logOfferAccepted(
        offerId: String,
        appName: String,
        value: Double,
        distanceKm: Double,
        gainPerKm: Double = 0.0,
        decisionSource: String = "manual_click"
    ) {
        val calculatedGain = if (gainPerKm > 0.0) gainPerKm else if (distanceKm > 0) value / distanceKm else 0.0
        logEvent(
            eventName = "offer_accepted",
            category = AnalyticsCategory.RADAR_DISPATCH,
            params = mapOf(
                "offer_id" to offerId,
                "app_source" to appName,
                "value_brl" to value,
                "distance_km" to distanceKm,
                "gain_per_km" to String.format(Locale.US, "%.2f", calculatedGain),
                "interaction_method" to decisionSource
            )
        )
    }

    fun logOfferDeclined(
        offerId: String,
        appName: String,
        value: Double,
        distanceKm: Double,
        reason: String,
        decisionSource: String = "manual_click"
    ) {
        logEvent(
            eventName = "offer_declined",
            category = AnalyticsCategory.RADAR_DISPATCH,
            params = mapOf(
                "offer_id" to offerId,
                "app_source" to appName,
                "value_brl" to value,
                "distance_km" to distanceKm,
                "decline_reason" to reason,
                "decision_source" to decisionSource
            )
        )
    }

    fun logVoiceCommand(command: String, isSuccess: Boolean) {
        logEvent(
            eventName = "voice_command_executed",
            category = AnalyticsCategory.PREMIUM_USAGE,
            params = mapOf(
                "command" to command,
                "success" to isSuccess
            )
        )
    }

    fun logSpeedSafetyAlert(currentSpeed: Double, limit: Double = 10.0) {
        logEvent(
            eventName = "safety_speed_alert",
            category = AnalyticsCategory.SAFETY,
            params = mapOf(
                "current_speed_kmh" to currentSpeed,
                "speed_limit_kmh" to limit,
                "lock_activated" to (currentSpeed > limit)
            )
        )
    }
}
