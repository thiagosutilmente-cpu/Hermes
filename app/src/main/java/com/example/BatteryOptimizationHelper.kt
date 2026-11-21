package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * BatteryOptimizationHelper
 *
 * Blindagem anti-encerramento em segundo plano para dispositivos Xiaomi (MIUI/HyperOS),
 * Samsung (OneUI), Motorola e Realme. Garante que os serviços de leitura de rotas
 * e cálculo de sinergia continuem rodando 24/7 na pista sem serem mortos pelo sistema operacional.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    /**
     * Verifica se o aplicativo já possui isenção de otimização de bateria (whitelist).
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    /**
     * Abre a tela de configurações gerais de bateria do sistema Android (compatível com Play Store).
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations(context)) {
            openGeneralBatterySettings(context)
        }
    }

    /**
     * Fallback para abrir a tela de gerenciamento de bateria do aplicativo.
     */
    fun openGeneralBatterySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appSettingsIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Falha crítica ao abrir configurações de app: ${e2.message}")
            }
        }
    }
}
