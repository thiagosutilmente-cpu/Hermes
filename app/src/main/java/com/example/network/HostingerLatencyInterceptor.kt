package com.example.network

/**
 * HostingerLatencyInterceptor
 *
 * Especialização de [LatencyInterceptor] configurada especificamente
 * para as chamadas à VPS Hostinger (187.77.248.73:8000).
 */
class HostingerLatencyInterceptor(
    thresholdMs: Long = DEFAULT_THRESHOLD_MS,
    listener: ImmediateFallbackListener? = null
) : LatencyInterceptor(thresholdMs, listener)
