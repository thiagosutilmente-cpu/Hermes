package com.example.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * HostingerVpsApiService
 *
 * Interface Retrofit definindo os endpoints de comunicação com o servidor VPS na Hostinger (187.77.248.73:8000).
 */
interface HostingerVpsApiService {

    /**
     * Envia o registro de uma oferta aceita ou recusada pelo entregador
     */
    @Headers("Content-Type: application/json")
    @POST("/api/offers/decision")
    suspend fun sendOfferDecision(
        @Body request: OfferDecisionRequest
    ): Response<ApiResponse<OfferDecisionResponse>>

    /**
     * Envia o registro de um comando de voz capturado pelo capacete
     */
    @Headers("Content-Type: application/json")
    @POST("/api/voice/command")
    suspend fun sendVoiceCommand(
        @Body request: VoiceCommandRequest
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * Valida a saúde e conectividade do servidor VPS via /api/ping
     */
    @GET("/api/ping")
    suspend fun checkHealth(): Response<PingResponse>

    /**
     * Valida a saúde e conectividade do servidor VPS via /health
     */
    @GET("/health")
    suspend fun getHealth(): Response<PingResponse>

    /**
     * Executa análise neural de oferta veicular com o Gemini hospedado na VPS
     */
    @Headers("Content-Type: application/json")
    @POST("/api/v1/analyze-ride")
    suspend fun analyzeRide(
        @Body request: RideAnalysisApiRequest
    ): Response<RideAnalysisApiResponse>

    /**
     * Consulta o status da assinatura de 7 dias ou plano Pro
     */
    @GET("/api/v1/subscription/status/{customerId}")
    suspend fun getSubscriptionStatus(
        @Path("customerId") customerId: String
    ): Response<SubscriptionStatusResponse>

    /**
     * Registra o token FCM do dispositivo para recepção de ofertas em tempo real
     */
    @Headers("Content-Type: application/json")
    @POST("/api/fcm/token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRegisterRequest
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * Lança a campanha de tráfego de motoboys no Meta Ads via VPS
     */
    @POST("/api/meta-ads/launch")
    suspend fun launchMetaCampaign(): Response<MetaCampaignResponse>

    /**
     * Alterna o status da campanha no Meta Ads entre ACTIVE e PAUSED
     */
    @Headers("Content-Type: application/json")
    @POST("/api/meta-ads/toggle-status")
    suspend fun toggleMetaCampaign(
        @Body request: MetaToggleApiRequest
    ): Response<MetaCampaignResponse>

    /**
     * Retorna métricas ao vivo da campanha de Meta Ads na VPS
     */
    @GET("/api/meta-ads/insights")
    suspend fun getMetaInsights(): Response<MetaInsightsResponse>

    /**
     * Atualiza o orçamento diário da campanha no Meta Ads
     */
    @Headers("Content-Type: application/json")
    @POST("/api/meta-ads/budget")
    suspend fun updateMetaBudget(
        @Body request: MetaBudgetApiRequest
    ): Response<MetaBudgetResponse>

    /**
     * Retorna o status da automação do WhatsApp Pessoal na VPS
     */
    @GET("/api/marketing/whatsapp/status")
    suspend fun getWhatsAppMarketingStatus(): Response<WhatsAppMarketingStatusResponse>

    /**
     * Dispara mensagem de teste para o WhatsApp configurado
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/whatsapp/send-test")
    suspend fun sendWhatsAppTest(
        @Body request: WhatsAppTestSendRequest
    ): Response<WhatsAppTestSendResponse>

    /**
     * Alterna a auto-resposta automática no WhatsApp
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/whatsapp/toggle-auto-reply")
    suspend fun toggleWhatsAppAutoReply(
        @Body request: WhatsAppToggleAutoReplyRequest
    ): Response<WhatsAppMarketingStatusResponse>

    /**
     * Retorna os leads recentes atendidos automaticamente no WhatsApp
     */
    @GET("/api/marketing/whatsapp/leads")
    suspend fun getWhatsAppLeads(): Response<WhatsAppLeadsResponse>

    /**
     * Retorna status e métricas da IA Vendedora 100% Autônoma na VPS
     */
    @GET("/api/marketing/ai-sales/status")
    suspend fun getAiSalesStatus(): Response<AiSalesStatusResponse>

    /**
     * Ativa ou pausa o piloto de vendas autônomo da IA no WhatsApp
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/ai-sales/toggle")
    suspend fun toggleAiSales(
        @Body request: AiSalesToggleRequest
    ): Response<AiSalesToggleResponse>

    /**
     * Simula a resposta da IA em tempo real com quebra de objeções
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/ai-sales/simulate")
    suspend fun simulateAiNegotiation(
        @Body request: AiSimulateRequest
    ): Response<AiSimulateResponse>

    /**
     * Retorna vendas fechadas e ativadas automaticamente pela IA
     */
    @GET("/api/marketing/ai-sales/deals")
    suspend fun getAiSalesDeals(): Response<AiDealsResponse>

    /**
     * Retorna status consolidado da suite de Super Automação 360°
     */
    @GET("/api/marketing/super-auto/status")
    suspend fun getSuperAutoStatus(): Response<SuperAutoStatusResponse>

    /**
     * Liga ou desliga módulos da super automação (recuperação, voz, otimizador)
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/super-auto/toggle")
    suspend fun toggleSuperAutoFeature(
        @Body request: SuperAutoToggleRequest
    ): Response<SuperAutoToggleResponse>

    /**
     * Dispara teste de mensagem de recuperação de Pix abandonado
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/super-auto/trigger-recovery")
    suspend fun triggerPixRecovery(
        @Body request: TriggerRecoveryRequest
    ): Response<TriggerRecoveryResponse>

    /**
     * Simula envio de áudio humanizado no WhatsApp
     */
    @Headers("Content-Type: application/json")
    @POST("/api/marketing/super-auto/test-voice")
    suspend fun testVoiceMessage(
        @Body request: TestVoiceRequest
    ): Response<TestVoiceResponse>

    /**
     * Executa ciclo de otimização autônoma de orçamento do Meta Ads
     */
    @POST("/api/marketing/super-auto/optimize-ads")
    suspend fun runAdsOptimization(): Response<OptimizeAdsResponse>

    /**
     * Retorna status consolidado da Operação Singularidade (Meta R$ 3 Milhões/Ano)
     */
    @GET("/api/singularidade/status")
    suspend fun getSingularidadeStatus(): Response<SingularidadeStatusResponse>

    /**
     * Dispara ciclo imediato do orquestrador MIND-0 com os 10 agentes
     */
    @POST("/api/singularidade/cycle/run")
    suspend fun triggerSingularidadeCycle(): Response<SingularidadeCycleResponse>
}



