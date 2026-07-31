package com.example.voice

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.ResponseBody
import com.squareup.moshi.JsonClass

interface OpenAiService {
    @POST("v1/audio/speech")
    suspend fun textToSpeech(
        @Header("Authorization") authorization: String,
        @Body requestBody: OpenAiRequestBody
    ): Response<ResponseBody>
}

@JsonClass(generateAdapter = true)
data class OpenAiRequestBody(
    val model: String = "tts-1",
    val input: String,
    val voice: String = "alloy",
    val response_format: String = "mp3"
)
