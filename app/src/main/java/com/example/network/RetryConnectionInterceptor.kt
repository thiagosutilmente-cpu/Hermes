package com.example.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * RetryConnectionInterceptor
 *
 * Interceptor de resiliência de rede para o OkHttp.
 * Executa até 3 tentativas automáticas com backoff exponencial suave em caso de
 * instabilidades transitórias de rede celular (4G/5G) ou oscilações na VPS Hostinger.
 */
class RetryConnectionInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 200L
) : Interceptor {

    companion object {
        private const val TAG = "RetryConnection"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                // Códigos 502, 503, 504 da VPS podem tentar retry
                if (response.code in 502..504 && attempt < maxRetries - 1) {
                    response.close()
                    attempt++
                    val backoff = initialDelayMs * (1L shl (attempt - 1))
                    Log.w(TAG, "Tentativa $attempt falhou com HTTP ${response.code}. Aguardando ${backoff}ms...")
                    Thread.sleep(backoff)
                    continue
                }
                return response
            } catch (e: IOException) {
                lastException = e
                attempt++
                if (attempt >= maxRetries) {
                    Log.e(TAG, "Todas as $maxRetries tentativas falharam para '${request.url.encodedPath}': ${e.message}")
                    throw e
                }
                val backoff = initialDelayMs * (1L shl (attempt - 1))
                Log.w(TAG, "Falha de conexão na tentativa $attempt. Reconectando em ${backoff}ms... (${e.message})")
                try {
                    Thread.sleep(backoff)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
            }
        }

        throw lastException ?: IOException("Falha desconhecida no RetryConnectionInterceptor")
    }
}
