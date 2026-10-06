package com.example.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RetrofitClient
 *
 * Cliente Singleton do Retrofit configurado para a VPS na Hostinger (187.77.248.73:8080).
 * Suporta atualização dinâmica de URL caso o IP ou domínio seja alterado nas configurações.
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"
    const val DEFAULT_BASE_URL = "http://187.77.248.73:8080/"

    private var currentBaseUrl = DEFAULT_BASE_URL
    private var cachedService: HostingerVpsApiService? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Retorna a instância do serviço Retrofit para chamadas de API
     */
    fun getApiService(baseUrl: String? = null): HostingerVpsApiService {
        val targetUrl = sanitizeBaseUrl(baseUrl ?: currentBaseUrl)

        if (cachedService != null && targetUrl == currentBaseUrl) {
            return cachedService!!
        }

        currentBaseUrl = targetUrl

        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(HostingerVpsApiService::class.java)
        cachedService = service
        Log.i(TAG, "Retrofit inicializado apontando para: $currentBaseUrl")
        return service
    }

    /**
     * Atualiza a URL base da VPS dinamicamente (ex: se o usuário trocar IP nas configurações)
     */
    fun updateBaseUrl(newUrl: String) {
        val sanitized = sanitizeBaseUrl(newUrl)
        if (sanitized != currentBaseUrl) {
            currentBaseUrl = sanitized
            cachedService = null
        }
    }

    private fun sanitizeBaseUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }
}
