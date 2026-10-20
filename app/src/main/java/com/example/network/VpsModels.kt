package com.example.network

import com.google.gson.annotations.SerializedName

/**
 * Modelos de dados para envio e recebimento de informações com o servidor VPS na Hostinger.
 */

/**
 * Payload de envio de decisão de oferta (Aceita ou Recusada)
 */
data class OfferDecisionRequest(
    @SerializedName("eventType")
    val eventType: String = "OFFER_DECISION",

    @SerializedName("offerId")
    val offerId: String,

    @SerializedName("action")
    val action: String, // "ACCEPTED" ou "DECLINED"

    @SerializedName("appName")
    val appName: String,

    @SerializedName("restaurant")
    val restaurant: String,

    @SerializedName("value")
    val value: Double,

    @SerializedName("distanceKm")
    val distanceKm: Double,

    @SerializedName("gainPerKm")
    val gainPerKm: Double,

    @SerializedName("source")
    val source: String, // "Toque na Tela", "Comando de Voz", "HUD Flutuante", etc.

    @SerializedName("reason")
    val reason: String = "",

    @SerializedName("deliveryAddress")
    val deliveryAddress: String = "",

    @SerializedName("deviceId")
    val deviceId: String = android.os.Build.MODEL,

    @SerializedName("androidVersion")
    val androidVersion: String = android.os.Build.VERSION.RELEASE,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resposta padrão do servidor VPS para operações de decisão de oferta
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resposta detalhada do registro da oferta no banco de dados da VPS
 */
data class OfferDecisionResponse(
    @SerializedName("receiptId")
    val receiptId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("savedAt")
    val savedAt: Long,

    @SerializedName("voice_alert")
    val voiceAlert: String? = null,

    @SerializedName("decision")
    val decision: String? = null
)

/**
 * Payload para registro de comandos de voz (STT)
 */
data class VoiceCommandRequest(
    @SerializedName("eventType")
    val eventType: String = "VOICE_COMMAND",

    @SerializedName("command")
    val command: String,

    @SerializedName("spokenText")
    val spokenText: String,

    @SerializedName("actionTaken")
    val actionTaken: String,

    @SerializedName("confidence")
    val confidence: Float = 0.95f,

    @SerializedName("micSource")
    val micSource: String = "BLUETOOTH_HELMET_OR_PHONE",

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Resposta de status e teste de conexão da VPS
 */
data class PingResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("serverTime")
    val serverTime: Long,

    @SerializedName("version")
    val version: String? = null
)

/**
 * Payload para registro e atualização do Token FCM do dispositivo na VPS
 */
data class FcmTokenRegisterRequest(
    @SerializedName("eventType")
    val eventType: String = "FCM_TOKEN_REGISTER",

    @SerializedName("token")
    val token: String,

    @SerializedName("deviceId")
    val deviceId: String = android.os.Build.MODEL,

    @SerializedName("androidVersion")
    val androidVersion: String = android.os.Build.VERSION.RELEASE,

    @SerializedName("driverId")
    val driverId: String = "driver_${android.os.Build.ID}",

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Payload para solicitação de análise neural de corrida na VPS
 */
data class RideAnalysisApiRequest(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("notification_text")
    val notificationText: String
)

/**
 * Resposta da análise neural de corrida processada pelo Gemini na VPS
 */
data class RideAnalysisApiResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("fallback_mode")
    val fallbackMode: Boolean? = false,

    @SerializedName("analysis")
    val analysis: RideAnalysisDetail? = null
)

data class RideAnalysisDetail(
    @SerializedName("decision")
    val decision: String? = null,

    @SerializedName("voice_alert")
    val voiceAlert: String? = null,

    @SerializedName("reasoning")
    val reasoning: String? = null,

    @SerializedName("haptic_pattern")
    val hapticPattern: String? = null
)

// =========================================================
// MODELOS PARA AUTOMAÇÃO DE META ADS (VPS HOSTINGER)
// =========================================================

data class MetaToggleApiRequest(
    @SerializedName("active")
    val active: Boolean
)

data class MetaBudgetApiRequest(
    @SerializedName("budget_reais")
    val budgetReais: Double
)

data class MetaCampaignResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("campaign_id")
    val campaignId: String? = null,

    @SerializedName("campaign_name")
    val campaignName: String? = null,

    @SerializedName("daily_budget_cents")
    val dailyBudgetCents: Int? = null,

    @SerializedName("target_audience")
    val targetAudience: String? = null,

    @SerializedName("mode")
    val mode: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)

data class MetaInsightsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("campaign_id")
    val campaignId: String? = null,

    @SerializedName("campaign_name")
    val campaignName: String? = null,

    @SerializedName("status")
    val status: String? = "ACTIVE",

    @SerializedName("is_active")
    val isActive: Boolean = true,

    @SerializedName("daily_budget_reais")
    val dailyBudgetReais: Double = 20.0,

    @SerializedName("target_audience")
    val targetAudience: String? = null,

    @SerializedName("metrics")
    val metrics: MetaMetricsDetail? = null,

    @SerializedName("is_live_meta_api")
    val isLiveMetaApi: Boolean = false
)

data class MetaMetricsDetail(
    @SerializedName("spend_reais")
    val spendReais: Double = 0.0,

    @SerializedName("impressions")
    val impressions: Int = 0,

    @SerializedName("clicks")
    val clicks: Int = 0,

    @SerializedName("cpc_reais")
    val cpcReais: Double = 0.0,

    @SerializedName("ctr_percent")
    val ctrPercent: Double = 0.0,

    @SerializedName("downloads_started")
    val downloadsStarted: Int = 0
)

data class MetaBudgetResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("daily_budget_reais")
    val dailyBudgetReais: Double? = null,

    @SerializedName("message")
    val message: String? = null
)

// -------------------------------------------------------------
// MODELOS DE AUTOMAÇÃO DO WHATSAPP PESSOAL (VPS HOSTINGER)
// -------------------------------------------------------------

data class WhatsAppMarketingStatusResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("status")
    val status: String = "CONNECTED",

    @SerializedName("connected_phone")
    val connectedPhone: String = "(11) 98765-4321",

    @SerializedName("auto_reply_active")
    val autoReplyActive: Boolean = true,

    @SerializedName("engine")
    val engine: String = "VPS_HOSTINGER_EVOLUTION_API",

    @SerializedName("leads_today")
    val leadsToday: Int = 19,

    @SerializedName("auto_replies_sent")
    val autoRepliesSent: Int = 19,

    @SerializedName("apk_downloads_sent")
    val apkDownloadsSent: Int = 11,

    @SerializedName("pix_converted")
    val pixConverted: Int = 4,

    @SerializedName("average_response_seconds")
    val averageResponseSeconds: Double = 2.1,

    @SerializedName("message")
    val message: String? = null
)

data class WhatsAppTestSendRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,

    @SerializedName("custom_message")
    val customMessage: String? = null
)

data class WhatsAppTestSendResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("phone_sent")
    val phoneSent: String? = null,

    @SerializedName("message_preview")
    val messagePreview: String? = null,

    @SerializedName("delivery_status")
    val deliveryStatus: String? = "DELIVERED",

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("message")
    val message: String? = null
)

data class WhatsAppToggleAutoReplyRequest(
    @SerializedName("active")
    val active: Boolean
)

data class WhatsAppLeadModel(
    @SerializedName("id")
    val id: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("source")
    val source: String = "Meta Ads (Instagram/FB)",

    @SerializedName("status")
    val status: String = "APK_ENVIADO",

    @SerializedName("first_contact_time")
    val firstContactTime: String,

    @SerializedName("last_message")
    val lastMessage: String
)

data class WhatsAppLeadsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("total_leads")
    val totalLeads: Int,

    @SerializedName("leads")
    val leads: List<WhatsAppLeadModel>
)

// ==============================================================================
// MODELOS DA IA VENDEDORA 100% AUTÔNOMA NO WHATSAPP
// ==============================================================================

data class AiSalesMetricsData(
    @SerializedName("enabled")
    val enabled: Boolean = true,

    @SerializedName("total_conversations")
    val totalConversations: Int = 14,

    @SerializedName("objections_handled")
    val objectionsHandled: Int = 9,

    @SerializedName("pix_generated")
    val pixGenerated: Int = 8,

    @SerializedName("pix_paid_auto")
    val pixPaidAuto: Int = 6,

    @SerializedName("total_revenue_reais")
    val totalRevenueReais: Double = 119.40,

    @SerializedName("conversion_rate_percent")
    val conversionRatePercent: Double = 42.8
)

data class AiSalesStatusResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: AiSalesMetricsData? = null,

    @SerializedName("message")
    val message: String? = null
)

data class AiSalesToggleRequest(
    @SerializedName("enabled")
    val enabled: Boolean
)

data class AiSalesToggleResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("enabled")
    val enabled: Boolean
)

data class AiSimulateRequest(
    @SerializedName("phone")
    val phone: String = "5511999999999",

    @SerializedName("message")
    val message: String
)

data class AiSimulateResult(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("user_message")
    val userMessage: String,

    @SerializedName("ai_response")
    val aiResponse: String,

    @SerializedName("action_type")
    val actionType: String,

    @SerializedName("objection_handled")
    val objectionHandled: Boolean,

    @SerializedName("timestamp")
    val timestamp: String
)

data class AiSimulateResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("simulation")
    val simulation: AiSimulateResult? = null,

    @SerializedName("message")
    val message: String? = null
)

data class AiDealModel(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("value_reais")
    val valueReais: Double = 19.90,

    @SerializedName("closed_by")
    val closedBy: String = "IA Autônoma (Sem intervenção)",

    @SerializedName("timestamp")
    val timestamp: String
)

data class AiDealsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("total")
    val total: Int,

    @SerializedName("deals")
    val deals: List<AiDealModel>
)

// ==============================================================================
// MODELOS DA SUPER AUTOMAÇÃO 360° (RECUPERAÇÃO, VOZ, OTIMIZADOR DE ADS)
// ==============================================================================

data class SuperAutoStatusData(
    @SerializedName("pix_recovery_enabled")
    val pixRecoveryEnabled: Boolean = true,

    @SerializedName("voice_engine_enabled")
    val voiceEngineEnabled: Boolean = true,

    @SerializedName("meta_optimizer_enabled")
    val metaOptimizerEnabled: Boolean = true,

    @SerializedName("sound_alerts_enabled")
    val soundAlertsEnabled: Boolean = true,

    @SerializedName("total_pix_recovered")
    val totalPixRecovered: Int = 7,

    @SerializedName("revenue_recovered_reais")
    val revenueRecoveredReais: Double = 139.30,

    @SerializedName("voice_audios_sent")
    val voiceAudiosSent: Int = 28,

    @SerializedName("ads_budget_optimized_reais")
    val adsBudgetOptimizedReais: Double = 45.00
)

data class SuperAutoStatusResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: SuperAutoStatusData? = null
)

data class SuperAutoToggleRequest(
    @SerializedName("feature")
    val feature: String,

    @SerializedName("enabled")
    val enabled: Boolean
)

data class SuperAutoToggleResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("feature")
    val feature: String,

    @SerializedName("enabled")
    val enabled: Boolean
)

data class TriggerRecoveryRequest(
    @SerializedName("phone")
    val phone: String = "5511999999999"
)

data class TriggerRecoveryResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("target_phone")
    val targetPhone: String? = null,

    @SerializedName("step")
    val step: String? = null,

    @SerializedName("message_sent")
    val messageSent: String? = null,

    @SerializedName("recovery_status")
    val recoveryStatus: String? = null
)

data class TestVoiceRequest(
    @SerializedName("phone")
    val phone: String = "5511999999999",

    @SerializedName("script_key")
    val scriptKey: String = "apresentacao"
)

data class TestVoiceResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("target_phone")
    val targetPhone: String? = null,

    @SerializedName("presence_simulation")
    val presenceSimulation: String? = null,

    @SerializedName("script_title")
    val scriptTitle: String? = null,

    @SerializedName("duration")
    val duration: String? = null,

    @SerializedName("transcript")
    val transcript: String? = null,

    @SerializedName("status")
    val status: String? = null
)

data class OptimizeAdsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("campaign_name")
    val campaignName: String? = null,

    @SerializedName("previous_budget_reais")
    val previousBudgetReais: Double? = null,

    @SerializedName("new_budget_reais")
    val newBudgetReais: Double? = null,

    @SerializedName("cost_per_acquisition_reais")
    val costPerAcquisitionReais: Double? = null,

    @SerializedName("decision")
    val decision: String? = null,

    @SerializedName("optimization_reason")
    val optimizationReason: String? = null,

    @SerializedName("roas_multiplier")
    val roasMultiplier: String? = null
)

// ==============================================================================
// MODELOS DA OPERAÇÃO SINGULARIDADE - R$ 3 MILHÕES / 10 AGENTES AUTÔNOMOS
// ==============================================================================

data class SingularidadeDashboardData(
    @SerializedName("target_daily")
    val targetDaily: Double = 8333.33,

    @SerializedName("target_annual")
    val targetAnnual: Double = 3000000.00,

    @SerializedName("today_gross_revenue")
    val todayGrossRevenue: Double = 8840.00,

    @SerializedName("today_ad_spend")
    val todayAdSpend: Double = 2120.00,

    @SerializedName("today_net_profit")
    val todayNetProfit: Double = 6625.50,

    @SerializedName("today_roas")
    val todayRoas: Double = 4.17,

    @SerializedName("today_cpa")
    val todayCpa: Double = 9.14,

    @SerializedName("status_goal")
    val statusGoal: String = "META_DIARIA_BATIDA",

    @SerializedName("completion_pct")
    val completionPct: Double = 100.0,

    @SerializedName("active_agents_count")
    val activeAgentsCount: Int = 10
)

data class SingularidadeStatusResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: SingularidadeDashboardData? = null
)

data class SingularidadeCycleResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("result")
    val result: Map<String, Any>? = null
)



