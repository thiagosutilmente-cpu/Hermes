package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Estado do Modo Noturno Extremo OLED / Economia e Proteção Térmica de Bateria
 */
data class UltraOledState(
    val isOledModeActive: Boolean = false,
    val autoEnableOnLowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val currentBatteryPercent: Int = 85,
    val batteryTemperatureCelsius: Double = 32.4,
    val isThermalThrottlingWarning: Boolean = false,
    val autoEnableOnOverheating: Boolean = true
)

/**
 * UltraOledBatterySaverManager
 *
 * Gerenciador do Modo Noturno Extremo OLED (Pitch Black #000000) e Proteção Térmica no Guidão da Moto.
 *
 * Resolve o problema comum de celulares em suportes de moto expostos ao sol com GPS e 4G:
 * - Fundo 100% preto absoluto (#000000), desligando os pixels da tela AMOLED/OLED.
 * - Monitora a temperatura real da bateria via [BatteryManager.EXTRA_TEMPERATURE].
 * - Quando a bateria atinge 41.5°C sob calor do sol, ativa o escudo OLED para diminuir a dissipação
 *   de calor do display e evitar que o Android desligue o celular em plena rota.
 */
class UltraOledBatterySaverManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "UltraOledManager"
        private const val PREFS_NAME = "radar_oled_battery_prefs"
        private const val KEY_OLED_ACTIVE = "key_oled_active"
        private const val KEY_AUTO_ENABLE = "key_auto_enable_low_bat"
        private const val KEY_AUTO_THERMAL = "key_auto_enable_thermal"

        // Limiar crítico de superaquecimento no suporte de moto: 41.5°C
        const val OVERHEATING_THRESHOLD_CELSIUS = 41.5

        @Volatile
        private var INSTANCE: UltraOledBatterySaverManager? = null

        fun getInstance(context: Context): UltraOledBatterySaverManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UltraOledBatterySaverManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UltraOledState(
            isOledModeActive = prefs.getBoolean(KEY_OLED_ACTIVE, false),
            autoEnableOnLowBattery = prefs.getBoolean(KEY_AUTO_ENABLE, true),
            autoEnableOnOverheating = prefs.getBoolean(KEY_AUTO_THERMAL, true)
        )
    )
    val state: StateFlow<UltraOledState> = _state.asStateFlow()

    init {
        registerBatteryReceiver()
    }

    private fun registerBatteryReceiver() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)
            batteryStatus?.let { updateFromBatteryIntent(it) }

            // Observador contínuo
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    intent?.let { updateFromBatteryIntent(it) }
                }
            }
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao registrar receiver de bateria: ${e.message}")
        }
    }

    private fun updateFromBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) // décimos de grau Celsius (ex: 325 = 32.5°C)

        val percent = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85
        val tempCelsius = if (tempRaw > 0) tempRaw / 10.0 else 32.0

        updateBatteryTelemetry(percent, tempCelsius)
    }

    fun updateBatteryTelemetry(percent: Int, tempCelsius: Double) {
        val current = _state.value
        val isOverheating = tempCelsius >= OVERHEATING_THRESHOLD_CELSIUS

        _state.value = current.copy(
            currentBatteryPercent = percent,
            batteryTemperatureCelsius = tempCelsius,
            isThermalThrottlingWarning = isOverheating
        )

        // Ativação por nível baixo de bateria
        if (current.autoEnableOnLowBattery && percent <= current.lowBatteryThreshold && !current.isOledModeActive) {
            setOledMode(true, "Bateria crítica ($percent%). Modo Noturno Extremo ativado automaticamente.")
        }

        // Ativação automática preventiva por superaquecimento sob o sol
        if (current.autoEnableOnOverheating && isOverheating && !current.isOledModeActive) {
            val tempFmt = String.format(Locale("pt", "BR"), "%.1f", tempCelsius)
            setOledMode(true, "Atenção: Temperatura da bateria em $tempFmt graus. Modo OLED ativado para proteção térmica.")
        }
    }

    fun toggleOledMode(): Boolean {
        val newMode = !_state.value.isOledModeActive
        setOledMode(newMode)
        return newMode
    }

    fun setOledMode(enabled: Boolean, voiceMessage: String? = null) {
        prefs.edit().putBoolean(KEY_OLED_ACTIVE, enabled).apply()
        _state.value = _state.value.copy(isOledModeActive = enabled)

        try {
            HapticFeedbackHelper.vibrateTap(context)
            val msg = voiceMessage ?: if (enabled) {
                "Modo Noturno Extremo OLED ativado. Economia máxima de bateria e menor aquecimento."
            } else {
                "Modo Cockpit Padrão restaurado."
            }
            TextToSpeechManager.getInstance(context).speak(msg)
        } catch (_: Exception) {}
    }

    fun setAutoEnableOnLowBattery(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ENABLE, enabled).apply()
        _state.value = _state.value.copy(autoEnableOnLowBattery = enabled)
    }

    fun setAutoEnableOnOverheating(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_THERMAL, enabled).apply()
        _state.value = _state.value.copy(autoEnableOnOverheating = enabled)
    }
}
