package com.example.voice

import com.example.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object OpenAiClient {
    private const val BASE_URL = "https://api.openai.com/"
    
    val service: OpenAiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenAiService::class.java)
    }

    val apiKey: String
        get() {
            val customKey = try {
                com.example.coordinator.RadarCoordinator.settings.value.openAiApiKey
            } catch (e: Exception) {
                ""
            }
            return if (customKey.isNotEmpty()) customKey else try { BuildConfig.OPENAI_API_KEY } catch (e: Exception) { "" }
        }
}
