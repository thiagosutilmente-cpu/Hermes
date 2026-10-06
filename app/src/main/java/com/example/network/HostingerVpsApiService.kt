package com.example.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * HostingerVpsApiService
 *
 * Interface Retrofit definindo os endpoints de comunicação com o servidor VPS na Hostinger (187.77.248.73:8080).
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
     * Valida a saúde e conectividade do servidor VPS
     */
    @GET("/api/ping")
    suspend fun checkHealth(): Response<PingResponse>
}
