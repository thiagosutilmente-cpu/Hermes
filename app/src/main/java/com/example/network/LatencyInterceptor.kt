package com.example.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Metadados detalhados da requisição que excedeu o limiar de latência.
 */
data class LatencyFallbackMetadata(
    val endpoint: String,
    val httpMethod: String,
    val latencyMs: Long,
    val thresholdMs: Long = LatencyInterceptor.DEFAULT_THRESHOLD_MS,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Interface dedicada de callback para notificar o sistema sobre o fallback imediato.
 */
fun interface ImmediateFallbackListener {
    fun onImmediateFallbackTriggered(metadata: LatencyFallbackMetadata)
}

/**
 * LatencyInterceptor
 *
 * Interceptor para OkHttp que:
 * 1. Mede a latência da requisição HTTP (RTT) com alta precisão via [System.nanoTime].
 * 2. Dispara um [LiveData] global de alerta se o tempo exceder 500ms.
 * 3. Invoca a interface dedicada [ImmediateFallbackListener] para notificar o sistema de fallback imediato.
 * 4. Aciona silenciosamente o motor local on-device do JARVIS para evitar qualquer atraso em decisões de entrega.
 */
open class LatencyInterceptor(
    val thresholdMs: Long = DEFAULT_THRESHOLD_MS,
    private val fallbackListener: ImmediateFallbackListener? = null
) : Interceptor {

    /**
     * Construtor secundário para aceitar expressões lambda diretamente.
     */
    constructor(
        thresholdMs: Long = DEFAULT_THRESHOLD_MS,
        onFallbackLambda: ((metadata: LatencyFallbackMetadata) -> Unit)?
    ) : this(
        thresholdMs = thresholdMs,
        fallbackListener = onFallbackLambda?.let { lambda ->
            ImmediateFallbackListener { meta -> lambda(meta) }
        }
    )

    companion object {
        const val TAG = "LatencyInterceptor"
        const val DEFAULT_THRESHOLD_MS = 500L

        // LiveData global de alerta com metadados completos
        private val _latencyAlertLiveData = MutableLiveData<LatencyFallbackMetadata?>()
        val latencyAlertLiveData: LiveData<LatencyFallbackMetadata?> = _latencyAlertLiveData

        // LiveData legado com dados simplificados
        val latencyExceededLiveData: LiveData<LatencyFallbackMetadata?> = _latencyAlertLiveData

        // Listener global estático opcional
        @Volatile
        var globalFallbackListener: ImmediateFallbackListener? = null

        /**
         * Limpa o estado atual do LiveData de alerta.
         */
        fun clearAlert() {
            _latencyAlertLiveData.postValue(null)
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = request.url.encodedPath
        val httpMethod = request.method
        val startNs = System.nanoTime()

        var response: Response? = null
        var isSuccess = false

        try {
            response = chain.proceed(request)
            isSuccess = response.isSuccessful
            return response
        } catch (e: Exception) {
            isSuccess = false
            throw e
        } finally {
            val elapsedNs = System.nanoTime() - startNs
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNs).coerceAtLeast(1L)

            // Registra a medição no monitor central
            HostingerLatencyMonitor.recordLatency(
                endpoint = endpoint,
                latencyMs = elapsedMs,
                isSuccess = isSuccess
            )

            // Se o tempo da resposta exceder 500ms
            if (elapsedMs > thresholdMs) {
                Log.w(
                    TAG,
                    "🚨 [LATÊNCIA EXCEDIDA > ${thresholdMs}ms] $httpMethod '$endpoint' levou ${elapsedMs}ms! Acionando fallback local imediato."
                )

                val metadata = LatencyFallbackMetadata(
                    endpoint = endpoint,
                    httpMethod = httpMethod,
                    latencyMs = elapsedMs,
                    thresholdMs = thresholdMs,
                    isSuccess = isSuccess
                )

                // 1. Dispara o LiveData global de alerta
                _latencyAlertLiveData.postValue(metadata)

                // 2. Chaveia o motor JARVIS para fallback local on-device (< 5ms)
                HostingerLatencyMonitor.setFallbackActive(true)

                // 3. Invoca a interface dedicada de callback
                try {
                    fallbackListener?.onImmediateFallbackTriggered(metadata)
                    globalFallbackListener?.onImmediateFallbackTriggered(metadata)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao invocar ImmediateFallbackListener: ${e.message}")
                }
            } else {
                Log.i(TAG, "✓ $httpMethod '$endpoint' respondido em ${elapsedMs}ms.")
            }
        }
    }
}
