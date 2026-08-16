package com.example.service.gemini

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface Retrofit para a API do Gemini
 */
interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}
