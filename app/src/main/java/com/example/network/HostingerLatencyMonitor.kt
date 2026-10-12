package com.example.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Estado da telemetria de latência e saúde da conexão com a VPS Hostinger.
 */
data class HostingerLatencyState(
    val lastLatencyMs: Long = 0L,
    val averageLatencyMs: Long = 0L,
    val isBottleneckActive: Boolean = false,
    val bottleneckCount: Int = 0,
    val totalCallsCount: Int = 0,
    val lastEndpoint: String = "",
    val isFallbackToLocalEngineActive: Boolean = false,
    val statusDescription: String = "Aguardando primeira chamada",
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val latencyHistory: List<Long> = emptyList()
)

/**
 * HostingerLatencyMonitor
 *
 * Gerenciador singleton reativo de latência da rede e governança de fallback.
 * Quando as chamadas para a VPS Hostinger ultrapassam o limiar crítico de 500ms,
 * aciona automaticamente o fallback para o motor on-device do JARVIS (< 5ms)
 * para que o motoboy nunca perca uma corrida por lentidão de servidor.
 */
object HostingerLatencyMonitor {

    private const val TAG = "HostingerLatencyMonitor"
    const val CRITICAL_LATENCY_THRESHOLD_MS = 500L

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _latencyState = MutableStateFlow(HostingerLatencyState())
    val latencyState: StateFlow<HostingerLatencyState> = _latencyState.asStateFlow()

    private val recentLatencies = mutableListOf<Long>()

    /**
     * Registra uma medição de latência capturada pelo [HostingerLatencyInterceptor].
     */
    fun recordLatency(endpoint: String, latencyMs: Long, isSuccess: Boolean) {
        synchronized(recentLatencies) {
            recentLatencies.add(latencyMs)
            if (recentLatencies.size > 10) {
                recentLatencies.removeAt(0)
            }
        }

        val current = _latencyState.value
        val isBottleneck = latencyMs > CRITICAL_LATENCY_THRESHOLD_MS
        val newBottleneckCount = if (isBottleneck) current.bottleneckCount + 1 else current.bottleneckCount
        val newTotalCalls = current.totalCallsCount + 1

        val avg = synchronized(recentLatencies) {
            if (recentLatencies.isNotEmpty()) recentLatencies.average().toLong() else latencyMs
        }

        val history = synchronized(recentLatencies) {
            recentLatencies.toList()
        }

        val status = when {
            !isSuccess -> "Falha de Conexão com VPS"
            latencyMs <= 150L -> "Conexão Ultra-rápida (<150ms)"
            latencyMs <= CRITICAL_LATENCY_THRESHOLD_MS -> "Latência Aceitável (${latencyMs}ms)"
            else -> "Gargalo Crítico (${latencyMs}ms) > 500ms"
        }

        val fallbackActive = isBottleneck || current.isFallbackToLocalEngineActive

        _latencyState.value = current.copy(
            lastLatencyMs = latencyMs,
            averageLatencyMs = avg,
            isBottleneckActive = isBottleneck,
            bottleneckCount = newBottleneckCount,
            totalCallsCount = newTotalCalls,
            lastEndpoint = endpoint,
            isFallbackToLocalEngineActive = fallbackActive,
            statusDescription = status,
            lastUpdateTime = System.currentTimeMillis(),
            latencyHistory = history
        )

        if (isBottleneck) {
            Log.w(TAG, "🚨 GARGALO DETECTADO: $endpoint demorou ${latencyMs}ms (> 500ms). Chaveando JARVIS para motor local.")
        } else {
            Log.d(TAG, "Latência Hostinger: ${latencyMs}ms em $endpoint (Média: ${avg}ms)")
        }
    }

    /**
     * Alterna manualmente o estado de fallback para o motor local.
     */
    fun setFallbackActive(active: Boolean) {
        _latencyState.value = _latencyState.value.copy(
            isFallbackToLocalEngineActive = active,
            isBottleneckActive = active
        )
    }

    /**
     * Realiza um teste de ping ativo e não bloqueante para a VPS da Hostinger.
     */
    fun testPing(context: Context, onComplete: ((Long, Boolean) -> Unit)? = null) {
        scope.launch {
            val startNs = System.nanoTime()
            var success = false
            try {
                val service = RetrofitClient.getApiService()
                val response = service.checkHealth()
                success = response.isSuccessful
            } catch (e: Exception) {
                Log.w(TAG, "Erro no teste de ping: ${e.message}")
                success = false
            }
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs).coerceAtLeast(1L)
            recordLatency(endpoint = "/api/ping", latencyMs = elapsedMs, isSuccess = success)

            withContext(Dispatchers.Main) {
                onComplete?.invoke(elapsedMs, success)
            }
        }
    }

    /**
     * Reseta as estatísticas da sessão.
     */
    fun resetStats() {
        synchronized(recentLatencies) {
            recentLatencies.clear()
        }
        _latencyState.value = HostingerLatencyState(
            statusDescription = "Estatísticas reiniciadas"
        )
    }
}
