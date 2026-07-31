package com.example.voice

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody

interface ElevenLabsService {
    @POST("v1/text-to-speech/{voice_id}")
    suspend fun textToSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String,
        @Body requestBody: ElevenLabsRequestBody
    ): Response<ResponseBody>
}

@JsonClass(generateAdapter = true)
data class ElevenLabsRequestBody(
    val text: String,
    val model_id: String = "eleven_multilingual_v2",
    val voice_settings: VoiceSettings = VoiceSettings()
)

@JsonClass(generateAdapter = true)
data class VoiceSettings(
    val stability: Float = 0.5f,
    val similarity_boost: Float = 0.75f,
    val style: Float = 0.0f,
    val use_speaker_boost: Boolean = true
)
