package com.example

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Utilitário de Feedback Háptico Tático para o Piloto de Moto.
 * Fornece assinaturas táteis físicas distintas (vibrações) para que o entregador
 * saiba exatamente o que foi executado (aceite, recusa, comando de voz, clique)
 * sem precisar desviar a visão da via.
 */
object HapticFeedbackHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Erro ao obter Vibrator: ${e.message}")
            null
        }
    }

    /**
     * Feedback Háptico de Confirmação de Aceite (Oferta Aceita).
     * Assinatura física: Pulso duplo afirmativo de alta energia ("Vrum-Vrum")
     * Facilmente sentido através das luvas e vibrações do guidão.
     */
    fun vibrateAccept(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Timings: 0ms delay, 120ms vibração forte, 80ms pausa, 200ms vibração confirmativa
                val timings = longArrayOf(0, 120, 80, 200)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 120, 80, 200), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateAccept: ${e.message}")
        }
    }

    /**
     * Feedback Háptico de Recusa (Oferta Recusada ou Descartada).
     * Assinatura física: Pulso curto único e seco (80ms).
     */
    fun vibrateDecline(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(80, 180)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateDecline: ${e.message}")
        }
    }

    /**
     * Feedback Háptico para Comando de Voz Reconhecido.
     * Assinatura física: Pulso tático de resposta neural imediata (40ms vibração, 40ms pausa, 60ms vibração).
     */
    fun vibrateVoiceCommandRecognized(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 40, 70)
                val amplitudes = intArrayOf(0, 200, 0, 240)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 40, 70), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateVoice: ${e.message}")
        }
    }

    /**
     * Feedback Háptico para Toques e Ações Secundárias (ex: Abrir Mapa, Alternar Filtro).
     * Assinatura física: Clique tátil rápido e sutil (35ms).
     */
    fun vibrateTap(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(35, 140)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateTap: ${e.message}")
        }
    }
}
