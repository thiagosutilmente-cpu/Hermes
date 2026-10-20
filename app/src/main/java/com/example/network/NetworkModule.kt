package com.example.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * NetworkModule
 *
 * Módulo de configuração de rede para o ecossistema JARVIS NEURAL COCKPIT.
 * Fornece instâncias gerenciadas do OkHttpClient, Retrofit e dos serviços [ApiService] e [HostingerVpsApiService].
 *
 * Inclui:
 * - Tolerância a falhas e reconexão automática resiliente (RetryConnectionInterceptor)
 * - Monitoramento de latência em tempo real com threshold de 500ms (LatencyInterceptor)
 * - Interceptor de logs detalhados para auditoria de requisições
 * - Configuração dinâmica de URL da VPS Hostinger
 * - Timeouts calibrados para operação veicular crítica
 */
object NetworkModule {

    private const val TAG = "NetworkModule"

    // URL Padrão da VPS Hostinger onde roda o FastAPI (porta 8000)
    const val DEFAULT_BASE_URL = "http://187.77.248.73:8000/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var apiServiceInstance: ApiService? = null

    @Volatile
    private var vpsApiServiceInstance: HostingerVpsApiService? = null

    @Volatile
    private var retrofitInstance: Retrofit? = null

    // Logging Interceptor com nível BODY para debug ou BASIC para economia
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            Log.d("OkHttp_Jarvis", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    // Interceptador de latência integrado ao monitor do Cockpit
    private val latencyInterceptor: LatencyInterceptor by lazy {
        LatencyInterceptor(thresholdMs = 500L)
    }

    // Interceptador de reconexão resiliente com backoff exponencial
    private val retryInterceptor: RetryConnectionInterceptor by lazy {
        RetryConnectionInterceptor(maxRetries = 3)
    }

    /**
     * Instância singleton do OkHttpClient configurada para ambiente de tráfego intenso e baixa latência.
     */
    val okHttpClient: OkHttpClient by lazy {
        provideOkHttpClient()
    }

    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(latencyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Retorna ou cria a instância do Retrofit com a URL base especificada ou padrão.
     */
    @Synchronized
    fun provideRetrofit(baseUrl: String? = null): Retrofit {
        val targetUrl = sanitizeBaseUrl(baseUrl ?: currentBaseUrl)

        if (retrofitInstance != null && targetUrl == currentBaseUrl) {
            return retrofitInstance!!
        }

        currentBaseUrl = targetUrl
        Log.i(TAG, "Configurando Retrofit com Base URL: $currentBaseUrl")

        val newRetrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofitInstance = newRetrofit
        // Invalida instâncias de serviços cacheados para regenerar com a nova URL
        apiServiceInstance = null
        vpsApiServiceInstance = null

        return newRetrofit
    }

    /**
     * Retorna a implementação de [ApiService] para gerenciar assinaturas, split e status de licença.
     */
    @Synchronized
    fun provideApiService(baseUrl: String? = null): ApiService {
        if (baseUrl != null && baseUrl != currentBaseUrl) {
            provideRetrofit(baseUrl)
        }
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: provideRetrofit().create(ApiService::class.java).also {
                apiServiceInstance = it
            }
        }
    }

    /**
     * Retorna a implementação de [HostingerVpsApiService] para telemetria de ofertas e comandos.
     */
    @Synchronized
    fun provideHostingerVpsApiService(baseUrl: String? = null): HostingerVpsApiService {
        if (baseUrl != null && baseUrl != currentBaseUrl) {
            provideRetrofit(baseUrl)
        }
        return vpsApiServiceInstance ?: synchronized(this) {
            vpsApiServiceInstance ?: provideRetrofit().create(HostingerVpsApiService::class.java).also {
                vpsApiServiceInstance = it
            }
        }
    }

    /**
     * Atualiza a URL base da VPS dinamicamente (ex: se o motoboy configurar IP próprio nas opções).
     */
    @Synchronized
    fun updateBaseUrl(newUrl: String) {
        val sanitized = sanitizeBaseUrl(newUrl)
        if (sanitized != currentBaseUrl) {
            currentBaseUrl = sanitized
            provideRetrofit(currentBaseUrl)
        }
    }

    /**
     * Retorna a URL base em vigor.
     */
    fun getCurrentBaseUrl(): String = currentBaseUrl

    /**
     * Garante que a URL termine sempre com barra "/" para compatibilidade com o Retrofit.
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
