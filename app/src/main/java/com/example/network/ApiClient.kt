package com.example.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ApiClient
 *
 * Cliente Singleton do Retrofit configurado para a VPS Hostinger (http://187.77.248.73:8000/).
 * Inclui HttpLoggingInterceptor no nível BODY para auditoria completa e monitoramento do tráfego.
 */
object ApiClient {

    private const val TAG = "ApiClient"
    const val BASE_URL = "http://187.77.248.73:8000/"

    private var currentBaseUrl: String = BASE_URL

    @Volatile
    private var cachedApiService: HostingerVpsApiService? = null

    @Volatile
    private var cachedRetrofit: Retrofit? = null

    /**
     * Interceptor de log OkHttp configurado no nível BODY para depuração e monitoramento da VPS.
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            Log.d("OkHttp_Jarvis", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * OkHttpClient configurado com timeouts calibrados e o Interceptor de log.
     */
    val okHttpClient: OkHttpClient by lazy {
        provideOkHttpClient()
    }

    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Retorna a instância do Retrofit com a URL base configurada.
     */
    @Synchronized
    fun getRetrofit(baseUrl: String? = null): Retrofit {
        val targetUrl = sanitizeBaseUrl(baseUrl ?: currentBaseUrl)

        if (cachedRetrofit != null && targetUrl == currentBaseUrl) {
            return cachedRetrofit!!
        }

        currentBaseUrl = targetUrl
        Log.i(TAG, "Configurando Retrofit no ApiClient para: $currentBaseUrl")

        val newRetrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        cachedRetrofit = newRetrofit
        cachedApiService = null

        return newRetrofit
    }

    /**
     * Retorna o serviço HostingerVpsApiService pronto para chamadas de API.
     */
    @Synchronized
    fun getApiService(baseUrl: String? = null): HostingerVpsApiService {
        val targetUrl = sanitizeBaseUrl(baseUrl ?: currentBaseUrl)

        if (cachedApiService != null && targetUrl == currentBaseUrl) {
            return cachedApiService!!
        }

        val retrofit = getRetrofit(targetUrl)
        val service = retrofit.create(HostingerVpsApiService::class.java)
        cachedApiService = service
        return service
    }

    /**
     * Atualiza a URL base da VPS dinamicamente se necessário.
     */
    @Synchronized
    fun updateBaseUrl(newUrl: String) {
        val sanitized = sanitizeBaseUrl(newUrl)
        if (sanitized != currentBaseUrl) {
            currentBaseUrl = sanitized
            getRetrofit(currentBaseUrl)
        }
    }

    /**
     * Retorna a URL base ativa.
     */
    fun getCurrentBaseUrl(): String = currentBaseUrl

    /**
     * Garante o prefixo http:// e o sufixo / na URL.
     */
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
