package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado do Modo Noturno Extremo OLED / Economia Extrema de Bateria
 */
data class UltraOledState(
    val isOledModeActive: Boolean = false,
    val autoEnableOnLowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val currentBatteryPercent: Int = 85
)

/**
 * UltraOledBatterySaverManager
 *
 * Gerenciador do Modo Noturno Extremo OLED (Pitch Black #000000).
 * Reduz em até 40% o consumo de energia em telas AMOLED/OLED durante turnos de 10 a 12 horas:
 * - Fundo 100% preto absoluto (#000000), desligando os pixels da tela
 * - Desativa animações contínuas e transições custosas de GPU
 * - Alto contraste monocromático com toques de Neon Verde para leitura instantânea no trânsito
 */
class UltraOledBatterySaverManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "radar_oled_battery_prefs"
        private const val KEY_OLED_ACTIVE = "key_oled_active"
        private const val KEY_AUTO_ENABLE = "key_auto_enable_low_bat"

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
            autoEnableOnLowBattery = prefs.getBoolean(KEY_AUTO_ENABLE, true)
        )
    )
    val state: StateFlow<UltraOledState> = _state.asStateFlow()

    fun toggleOledMode(): Boolean {
        val newMode = !_state.value.isOledModeActive
        setOledMode(newMode)
        return newMode
    }

    fun setOledMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OLED_ACTIVE, enabled).apply()
        _state.value = _state.value.copy(isOledModeActive = enabled)

        try {
            HapticFeedbackHelper.performClick(context)
            val tts = OfferTextToSpeechEngine.getInstance(context)
            if (enabled) {
                tts.speak("Modo Noturno Extremo OLED ativado. Economia máxima de bateria.")
            } else {
                tts.speak("Modo Cockpit Padrão restaurado.")
            }
        } catch (_: Exception) {}
    }

    fun setAutoEnableOnLowBattery(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ENABLE, enabled).apply()
        _state.value = _state.value.copy(autoEnableOnLowBattery = enabled)
    }

    fun updateBatteryLevel(percent: Int) {
        val current = _state.value
        _state.value = current.copy(currentBatteryPercent = percent)

        // Ativação automática se a bateria estiver em nível crítico (<= 20%)
        if (current.autoEnableOnLowBattery && percent <= current.lowBatteryThreshold && !current.isOledModeActive) {
            setOledMode(true)
        }
    }
}
