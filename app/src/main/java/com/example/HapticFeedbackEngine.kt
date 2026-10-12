package com.example

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Categoria tátil da oferta de entrega.
 * Cada tipo possui uma assinatura sensorial exclusiva calibrada para que
 * o entregador saiba a qualidade da corrida sem desviar os olhos do trânsito.
 */
enum class OfferHapticCategory(val label: String, val tactileDescription: String) {
    HIGH_PROFIT(
        label = "Alta Rentabilidade",
        tactileDescription = "Tríplice pulso cadenciado e afirmativo (Ta-Ta-TUM!) com energia máxima para ofertas de alto lucro."
    ),
    LONG_DISTANCE(
        label = "Corrida Longa",
        tactileDescription = "Pulso longo sustentado e ressonante indicando percurso de longa distância (> 10 km)."
    ),
    AUTO_REJECTED(
        label = "Rejeição Automática",
        tactileDescription = "Duplo corte seco e incisivo (Tuc-Tuc) informando que a oferta tóxica/prejuízo foi descartada."
    ),
    STANDARD(
        label = "Oferta Padrão",
        tactileDescription = "Vibração dupla equilibrada para ofertas regulares dentro do range aceitável."
    ),
    RISK_ALERT(
        label = "Alerta de Risco",
        tactileDescription = "Sequência rápida e intermitente de alerta para regiões ou parâmetros críticos."
    )
}

/**
 * Posição de montagem física do smartphone para calibração de amplitude.
 */
enum class PhoneMountingProfile(val label: String, val amplitudeMultiplier: Float) {
    HANDLEBAR_RIGID("Suporte Rígido no Guidão", 1.0f), // Máxima amplitude para romper ressonância de 4.000 a 6.500 RPM
    HANDLEBAR_DAMPED("Suporte com Amortecedor de Vibração", 0.85f),
    POCKET_OR_JACKET("Bolso / Jaqueta", 0.90f),
    DESK_TEST("Bancada / Teste Manual", 0.70f)
}

/**
 * Engine central de feedback háptico tático do piloto de motocicleta.
 *
 * Especialmente calibrado para suportes rígidos instalados no guidão de motos monocilíndricas,
 * onde as microvibrações mecânicas constantes da combustão e do asfalto mascaram vibrações
 * comuns de smartphones.
 *
 * Utiliza envelopes de onda (Waveforms) e pulsos de alta amplitude/frequência para criar
 * assinaturas táteis inequívocas:
 * - Alta Rentabilidade: Batida rítmica triunfal
 * - Corrida Longa: Pulsação sustentada estendida
 * - Rejeição Automática: Golpe duplo seco
 */
class HapticFeedbackEngine private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "HapticFeedbackEngine"

        // Thresholds padrão para classificação tátil instantânea
        const val HIGH_PROFIT_RATE_PER_KM = 3.20 // R$/km mínimo para alta rentabilidade
        const val HIGH_PROFIT_MIN_PAYOUT = 18.00 // R$ mínimo bruto
        const val LONG_DISTANCE_KM = 9.50       // Distância em km considerada longa

        @Volatile
        private var INSTANCE: HapticFeedbackEngine? = null

        fun getInstance(context: Context): HapticFeedbackEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HapticFeedbackEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Vibrador do sistema operacional Android
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e(TAG, "Falha ao inicializar Vibrator: ${e.message}")
        null
    }

    // Perfil de calibração ativa (por padrão, calibrado para suporte rígido no guidão)
    private val _mountingProfile = MutableStateFlow(PhoneMountingProfile.HANDLEBAR_RIGID)
    val mountingProfile: StateFlow<PhoneMountingProfile> = _mountingProfile.asStateFlow()

    // Último padrão tátil disparado
    private val _lastTriggeredCategory = MutableStateFlow<OfferHapticCategory?>(null)
    val lastTriggeredCategory: StateFlow<OfferHapticCategory?> = _lastTriggeredCategory.asStateFlow()

    // Habilitar/desabilitar feedback tático global
    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        if (!enabled) stop()
    }

    fun setMountingProfile(profile: PhoneMountingProfile) {
        _motorcycleCalibratedProfile = profile
        _mountingProfile.value = profile
        Log.i(TAG, "Perfil de montagem tátil ajustado para: ${profile.label}")
    }

    private var _motorcycleCalibratedProfile: PhoneMountingProfile = PhoneMountingProfile.HANDLEBAR_RIGID

    /**
     * Intercepta dados brutos da oferta de entrega, classifica a categoria e
     * reproduz a assinatura tátil correspondente em tempo real.
     */
    fun classifyAndVibrate(
        payout: Double,
        distanceKm: Double,
        isAutoRejected: Boolean,
        platform: String? = null
    ): OfferHapticCategory {
        val category = when {
            isAutoRejected -> OfferHapticCategory.AUTO_REJECTED
            isHighProfit(payout, distanceKm) -> OfferHapticCategory.HIGH_PROFIT
            distanceKm >= LONG_DISTANCE_KM -> OfferHapticCategory.LONG_DISTANCE
            payout > 0.0 && distanceKm > 0.0 -> OfferHapticCategory.STANDARD
            else -> OfferHapticCategory.STANDARD
        }

        vibrateForCategory(category)
        return category
    }

    /**
     * Dispara a vibração física no hardware calibrada para a categoria especificada.
     */
    fun vibrateForCategory(category: OfferHapticCategory) {
        if (!_isHapticEnabled.value) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        _lastTriggeredCategory.value = category

        try {
            when (category) {
                OfferHapticCategory.HIGH_PROFIT -> playHighProfitPattern(v)
                OfferHapticCategory.LONG_DISTANCE -> playLongDistancePattern(v)
                OfferHapticCategory.AUTO_REJECTED -> playAutoRejectPattern(v)
                OfferHapticCategory.STANDARD -> playStandardPattern(v)
                OfferHapticCategory.RISK_ALERT -> playRiskAlertPattern(v)
            }
            Log.d(TAG, "Haptic acionado com sucesso: ${category.label}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao disparar feedback háptico: ${e.message}")
        }
    }

    /**
     * 1. ALTA RENTABILIDADE (GOLDEN OFFER)
     * Assinatura: Tríplice pulso cadenciado em crescendo ("Ta - Ta - TUUUM!")
     * Rompe a ressonância do guidão e desperta atenção instantânea positiva.
     */
    fun vibrateHighProfit() = vibrateForCategory(OfferHapticCategory.HIGH_PROFIT)

    private fun playHighProfitPattern(v: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timings: 0ms delay, 90ms pulso 1, 60ms pausa, 110ms pulso 2, 70ms pausa, 260ms pulso forte final
            val timings = longArrayOf(0, 90, 60, 110, 70, 260)
            val baseAmp = (255 * _motorcycleCalibratedProfile.amplitudeMultiplier).toInt().coerceIn(1, 255)
            val amplitudes = intArrayOf(0, (baseAmp * 0.75f).toInt(), 0, (baseAmp * 0.90f).toInt(), 0, baseAmp)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 90, 60, 110, 70, 260), -1)
        }
    }

    /**
     * 2. CORRIDA LONGA (DISTÂNCIA ELEVADA)
     * Assinatura: Pulsação longa estendida modulada com eco (Vruuuum... vrum)
     * Permite ao piloto discernir que se trata de uma viagem extensa.
     */
    fun vibrateLongDistance() = vibrateForCategory(OfferHapticCategory.LONG_DISTANCE)

    private fun playLongDistancePattern(v: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timings: 0ms delay, 380ms onda contínua, 140ms pausa, 160ms eco
            val timings = longArrayOf(0, 380, 140, 160)
            val baseAmp = (255 * _motorcycleCalibratedProfile.amplitudeMultiplier).toInt().coerceIn(1, 255)
            val amplitudes = intArrayOf(0, baseAmp, 0, (baseAmp * 0.70f).toInt())
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 380, 140, 160), -1)
        }
    }

    /**
     * 3. REJEIÇÃO AUTOMÁTICA (CORRIDA DESCARTADA PELO JARVIS)
     * Assinatura: Duplo estalo seco de corte (Tuc - Tuc)
     * Rápido, firme e sem arrasto, indicando término do evento sem ação do piloto.
     */
    fun vibrateAutoReject() = vibrateForCategory(OfferHapticCategory.AUTO_REJECTED)

    private fun playAutoRejectPattern(v: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timings: 0ms delay, 75ms batida seca, 45ms pausa curta, 85ms batida seca final
            val timings = longArrayOf(0, 75, 45, 85)
            val baseAmp = (255 * _motorcycleCalibratedProfile.amplitudeMultiplier).toInt().coerceIn(1, 255)
            val amplitudes = intArrayOf(0, baseAmp, 0, baseAmp)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 75, 45, 85), -1)
        }
    }

    /**
     * 4. OFERTA PADRÃO
     * Assinatura: Duplo toque suave e regular
     */
    fun vibrateStandard() = vibrateForCategory(OfferHapticCategory.STANDARD)

    private fun playStandardPattern(v: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 120, 80, 120)
            val baseAmp = (210 * _motorcycleCalibratedProfile.amplitudeMultiplier).toInt().coerceIn(1, 255)
            val amplitudes = intArrayOf(0, baseAmp, 0, baseAmp)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 120, 80, 120), -1)
        }
    }

    /**
     * 5. ALERTA DE RISCO / URGÊNCIA
     * Assinatura: 4 pulsos rápidos intermitentes
     */
    fun vibrateRiskAlert() = vibrateForCategory(OfferHapticCategory.RISK_ALERT)

    private fun playRiskAlertPattern(v: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 60, 40, 60, 40, 60, 40, 180)
            val baseAmp = (255 * _motorcycleCalibratedProfile.amplitudeMultiplier).toInt().coerceIn(1, 255)
            val amplitudes = intArrayOf(0, baseAmp, 0, baseAmp, 0, baseAmp, 0, baseAmp)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 60, 40, 60, 40, 60, 40, 180), -1)
        }
    }

    /**
     * Interrompe qualquer vibração em andamento imediatamente.
     */
    fun stop() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cancelar vibração: ${e.message}")
        }
    }

    /**
     * Regra interna de alta rentabilidade:
     * - Se a taxa R$/km exceder HIGH_PROFIT_RATE_PER_KM (R$ 3,20/km)
     * - OU se o ganho absoluto for elevado com taxa justa (R$ 25+ e > R$ 2,50/km)
     */
    private fun isHighProfit(payout: Double, distanceKm: Double): Boolean {
        if (payout <= 0.0 || distanceKm <= 0.0) return false
        val ratePerKm = payout / distanceKm
        return ratePerKm >= HIGH_PROFIT_RATE_PER_KM || (payout >= HIGH_PROFIT_MIN_PAYOUT && ratePerKm >= 2.50)
    }
}
