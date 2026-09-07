package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Planos de Acesso do Radar Coordinator
 */
enum class SubscriptionTier(
    val title: String,
    val priceFormatted: String,
    val billingCycle: String,
    val isPaid: Boolean
) {
    FREE("Plano Free", "R$ 0,00", "Gratuito com limites", false),
    PRO_MONTHLY("Plano Pro Mensal", "R$ 29,90", "/mês", true),
    PRO_ANNUAL("Plano Pro Anual", "R$ 239,90", "/ano (R$ 19,99/mês)", true)
}

/**
 * Estado atual da assinatura do condutor
 */
data class SubscriptionState(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val isActive: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialDaysRemaining: Int = 0,
    val expiryDateFormatted: String = "Expirado / Sem plano",
    val dailyScansUsed: Int = 0,
    val dailyScansLimit: Int = 3, // Limite para Free
    val isVoiceEnabled: Boolean = false,
    val isAutoFilterBadRunsEnabled: Boolean = false
) {
    val canScanNewOffers: Boolean
        get() = isActive || isTrialActive || (dailyScansUsed < dailyScansLimit)
}

/**
 * Gerenciador de Assinaturas e Paywall do Radar Coordinator
 * Permite persistência local, simulação de pagamento PIX e ativação de teste de 7 dias.
 */
object SubscriptionManager {

    private const val PREFS_NAME = "radar_subscription_prefs"
    private const val KEY_TIER = "sub_tier"
    private const val KEY_EXPIRY_MS = "sub_expiry_ms"
    private const val KEY_TRIAL_START_MS = "sub_trial_start_ms"
    private const val KEY_SCANS_USED = "sub_scans_used"
    private const val KEY_LAST_SCAN_DATE = "sub_last_scan_date"

    private lateinit var prefs: SharedPreferences

    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedState()
    }

    private fun loadSavedState() {
        if (!::prefs.isInitialized) return

        val tierName = prefs.getString(KEY_TIER, SubscriptionTier.FREE.name) ?: SubscriptionTier.FREE.name
        val tier = try { SubscriptionTier.valueOf(tierName) } catch (e: Exception) { SubscriptionTier.FREE }
        val expiryMs = prefs.getLong(KEY_EXPIRY_MS, 0L)
        val trialStartMs = prefs.getLong(KEY_TRIAL_START_MS, 0L)
        val now = System.currentTimeMillis()

        // Verifica trial de 7 dias
        val isTrial = trialStartMs > 0L && (now - trialStartMs) < (7L * 24 * 60 * 60 * 1000)
        val trialRemaining = if (isTrial) {
            val remainingMs = (7L * 24 * 60 * 60 * 1000) - (now - trialStartMs)
            (remainingMs / (24 * 60 * 60 * 1000)).toInt() + 1
        } else 0

        // Verifica plano pago ativo
        val isPaidActive = tier.isPaid && expiryMs > now
        val isActive = isPaidActive || isTrial

        // Contador de scans diários
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val lastDate = prefs.getString(KEY_LAST_SCAN_DATE, "") ?: ""
        val scans = if (lastDate == todayStr) prefs.getInt(KEY_SCANS_USED, 0) else 0

        val expiryStr = when {
            isTrial -> "Trial Grátis ($trialRemaining dias restantes)"
            isPaidActive -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expiryMs))
            else -> "Inativo (Plano Gratuito)"
        }

        val newState = SubscriptionState(
            tier = if (isActive && !isTrial) tier else if (isTrial) SubscriptionTier.PRO_MONTHLY else SubscriptionTier.FREE,
            isActive = isActive,
            isTrialActive = isTrial,
            trialDaysRemaining = trialRemaining,
            expiryDateFormatted = expiryStr,
            dailyScansUsed = scans,
            dailyScansLimit = 3,
            isVoiceEnabled = isActive,
            isAutoFilterBadRunsEnabled = isActive
        )
        _subscriptionState.value = newState

        // Sincroniza User Properties no Firebase Analytics
        FirebaseAnalyticsManager.updateSubscriptionUserProperties(
            tierName = newState.tier.name,
            isActive = newState.isActive,
            isTrial = newState.isTrialActive
        )
    }

    /**
     * Ativa o teste grátis de 7 dias do Plano Pro
     */
    fun startSevenDayTrial() {
        if (!::prefs.isInitialized) return
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_TRIAL_START_MS, now)
            .putString(KEY_TIER, SubscriptionTier.PRO_MONTHLY.name)
            .apply()
        loadSavedState()

        FirebaseAnalyticsManager.logTrialStarted(days = 7, source = "subscription_manager")
    }

    /**
     * Ativa a assinatura Pro (Simulação de confirmação de PIX / In-App Purchase)
     */
    fun activateProSubscription(annual: Boolean = false) {
        if (!::prefs.isInitialized) return
        val durationMs = if (annual) 365L * 24 * 60 * 60 * 1000 else 30L * 24 * 60 * 60 * 1000
        val expiryMs = System.currentTimeMillis() + durationMs
        val tier = if (annual) SubscriptionTier.PRO_ANNUAL else SubscriptionTier.PRO_MONTHLY

        prefs.edit()
            .putString(KEY_TIER, tier.name)
            .putLong(KEY_EXPIRY_MS, expiryMs)
            .apply()

        loadSavedState()
    }

    /**
     * Incrementa varredura para controle do plano Free
     */
    fun recordOfferScan() {
        if (!::prefs.isInitialized) return
        val now = System.currentTimeMillis()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val currentScans = _subscriptionState.value.dailyScansUsed + 1
        prefs.edit()
            .putString(KEY_LAST_SCAN_DATE, todayStr)
            .putInt(KEY_SCANS_USED, currentScans)
            .apply()
        loadSavedState()
    }

    /**
     * Reseta a assinatura para Free (modo de teste)
     */
    fun resetToFree() {
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
        loadSavedState()
    }
}
