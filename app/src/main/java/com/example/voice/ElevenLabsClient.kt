package com.example.voice

import com.example.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ElevenLabsClient {
    private const val BASE_URL = "https://api.elevenlabs.io/"
    
    val service: ElevenLabsService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ElevenLabsService::class.java)
    }

    val apiKey: String
        get() {
            val customKey = try {
                com.example.coordinator.RadarCoordinator.settings.value.elevenLabsApiKey
            } catch (e: Exception) {
                ""
            }
            return if (customKey.isNotEmpty()) customKey else BuildConfig.ELEVENLABS_API_KEY
        }
    
    // Voice ID para o Jarvis (Marcus - British Male, Professional)
    const val JARVIS_VOICE_ID = "VR6AewrXP67pIn9N9rU2" 
    
    // Voice ID para o Antoni (Amigável/Conversacional - PT-BR)
    const val ANTONI_VOICE_ID = "ErXwobaY60C9iAWzCgEh" 
}
