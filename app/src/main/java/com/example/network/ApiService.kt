package com.example.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ApiService
 *
 * Interface Retrofit que conecta o JARVIS NEURAL COCKPIT aos endpoints do servidor
 * Python/FastAPI hospedado na VPS Hostinger para governança da Assinatura Semanal (R$ 25,00),
 * validação de status de licença, split Asaas (40% Líder / 60% PJ Master) e webhooks.
 */
interface ApiService {

    /**
     * Cria ou gera o payload da Assinatura Semanal via PIX Recorrente com split Asaas de 40%.
     * Endpoint: POST /api/v1/subscription/create
     */
    @Headers("Content-Type: application/json")
    @POST("/api/v1/subscription/create")
    suspend fun createSubscription(
        @Body request: CreateSubscriptionRequest
    ): Response<CreateSubscriptionResponse>

    /**
     * Consulta em tempo real o status da licença do entregador (ativação de 7 dias, vencimento, chave).
     * Endpoint: GET /api/v1/subscription/status/{customerId}
     */
    @GET("/api/v1/subscription/status/{customerId}")
    suspend fun getSubscriptionStatus(
        @Path("customerId") customerId: String
    ): Response<SubscriptionStatusResponse>

    /**
     * Executa auditoria formal do Motor Financeiro Asaas (Passo 1: Split 40%/60% e Webhooks).
     * Endpoint: GET /api/v1/financial/split/audit
     */
    @GET("/api/v1/financial/split/audit")
    suspend fun auditFinancialSplit(): Response<FinancialSplitAuditResponse>

    /**
     * Endpoint para simulação ou encaminhamento de webhook de pagamento do Asaas.
     * Endpoint: POST /api/v1/webhook/asaas
     */
    @Headers("Content-Type: application/json")
    @POST("/api/v1/webhook/asaas")
    suspend fun sendAsaasWebhook(
        @Body webhookPayload: AsaasWebhookPayload
    ): Response<Map<String, Any>>

    /**
     * Ping de verificação de integridade e conectividade com o FastAPI.
     * Endpoint: GET /api/ping ou GET /
     */
    @GET("/api/ping")
    suspend fun pingServer(): Response<Map<String, Any>>
}

// ============================================================================
// DTOs / Modelos de Dados da Assinatura e Licença
// ============================================================================

/**
 * Requisição de Criação de Assinatura Semanal Asaas (PIX Recorrente)
 */
data class CreateSubscriptionRequest(
    @SerializedName("customer_id")
    val customerId: String,

    @SerializedName("leader_wallet_id")
    val leaderWalletId: String = "wal_general_praca_sp_01",

    @SerializedName("next_due_date")
    val nextDueDate: String, // Formato YYYY-MM-DD

    @SerializedName("value")
    val value: Double = 25.00,

    @SerializedName("cycle")
    val cycle: String = "WEEKLY",

    @SerializedName("billing_type")
    val billingType: String = "PIX",

    @SerializedName("customer_phone")
    val customerPhone: String? = null
)

/**
 * Resposta da Criação de Assinatura Semanal
 */
data class CreateSubscriptionResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("plan")
    val plan: String,

    @SerializedName("total_value")
    val totalValue: Double,

    @SerializedName("split_summary")
    val splitSummary: SplitSummaryResponse?,

    @SerializedName("asaas_payload")
    val asaasPayload: Map<String, Any>? = null,

    @SerializedName("message")
    val message: String? = null
)

data class SplitSummaryResponse(
    @SerializedName("leader_praca_wallet")
    val leaderPracaWallet: String,

    @SerializedName("leader_praca_value")
    val leaderPracaValue: Double,

    @SerializedName("leader_praca_percentage")
    val leaderPracaPercentage: String,

    @SerializedName("master_pj_value")
    val masterPjValue: Double,

    @SerializedName("master_pj_percentage")
    val masterPjPercentage: String
)

/**
 * Resposta da Consulta de Status da Licença de 7 dias
 */
data class SubscriptionStatusResponse(
    @SerializedName("customer_id")
    val customerId: String,

    @SerializedName("status")
    val status: String, // "ACTIVE", "OVERDUE", "NOT_FOUND"

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("plan_type")
    val planType: String? = null,

    @SerializedName("activation_key")
    val activationKey: String? = null,

    @SerializedName("expires_at")
    val expiresAt: String? = null
)

/**
 * Resposta de Auditoria Formal do Passo 1
 */
data class FinancialSplitAuditResponse(
    @SerializedName("passo_1_status")
    val passo1Status: String,

    @SerializedName("validacao_tecnica")
    val validacaoTecnica: TechnicalValidationResponse?,

    @SerializedName("resumo_fluxo_financeiro")
    val resumoFluxoFinanceiro: String,

    @SerializedName("proxima_acao_recomendada")
    val proximaAcaoRecomendada: String
)

data class TechnicalValidationResponse(
    @SerializedName("calculo_split_correto")
    val calculoSplitCorreto: Boolean,

    @SerializedName("payload_asaas_valido")
    val payloadAsaasValido: Boolean,

    @SerializedName("cobertura_webhooks")
    val coberturaWebhooks: Boolean
)

/**
 * Payload simulado ou real de Webhook do Asaas
 */
data class AsaasWebhookPayload(
    @SerializedName("event")
    val event: String, // "PAYMENT_RECEIVED", "PAYMENT_OVERDUE", etc.

    @SerializedName("payment")
    val payment: AsaasPaymentItem? = null
)

data class AsaasPaymentItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("customer")
    val customer: String,

    @SerializedName("value")
    val value: Double = 25.00,

    @SerializedName("billingType")
    val billingType: String = "PIX",

    @SerializedName("status")
    val status: String? = null
)
