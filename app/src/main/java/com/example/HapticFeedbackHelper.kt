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

    fun vibrateSuccess(context: Context) = vibrateAccept(context)
    fun vibrateWarning(context: Context) = vibrateDecline(context)

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

    /**
     * Feedback Háptico Tático para Notificação de Oferta de Alta Prioridade em Segundo Plano.
     * Assinatura física: 3 pulsos ritmados fortes (atenção imediata para nova corrida lucrativa).
     */
    fun vibrateHighPriorityOffer(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 150, 100, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150, 100, 250), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateHighPriorityOffer: ${e.message}")
        }
    }

    /**
     * Feedback Háptico Discreto de Confirmação do Alerta Silencioso de SOS.
     * Assinatura tátil: 2 micro-pulsos suaves (45ms, pausa 90ms, 45ms) com baixa amplitude.
     * Projetado para confirmar ao condutor que o WorkManager disparou o alerta silencioso
     * sem alertar ou chamar a atenção de agressores ou pessoas próximas.
     */
    fun vibrateSilentSosConfirmation(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 45, 90, 45)
                val amplitudes = intArrayOf(0, 110, 0, 120)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 45, 90, 45), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateSilentSosConfirmation: ${e.message}")
        }
    }

    /**
     * Pulso sutil para cada segundo da contagem regressiva do SOS (5s, 4s, 3s...)
     */
    fun vibrateSosCountdownTick(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(25, 100)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateSosCountdownTick: ${e.message}")
        }
    }

    /**
     * Feedback Háptico para Corrida VIP / Ouro (Ticket Alto > R$ 30,00 ou > R$ 4,00/km).
     * Assinatura física: Arpeggio tátil em crescendo (fraco-médio-forte).
     */
    fun vibrateVipRideOffer(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 60, 120, 60, 220)
                val amplitudes = intArrayOf(0, 140, 0, 200, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 80, 60, 120, 60, 220), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateVipRideOffer: ${e.message}")
        }
    }

    /**
     * Feedback Háptico para Rota Dupla / Entrega Agrupada (Multi-Stack).
     * Assinatura física: Duplo clique mecânico com reverberação.
     */
    fun vibrateBatchRideOffer(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 110, 80, 110)
                val amplitudes = intArrayOf(0, 220, 0, 220)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 110, 80, 110), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateBatchRideOffer: ${e.message}")
        }
    }

    /**
     * Feedback Háptico para Alerta de Chuva / Pista Molhada (+35% de sobretaxa ativa).
     * Assinatura física: 3 micro-gotas táteis suaves.
     */
    fun vibrateRainModeSurge(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 80, 30, 80, 30)
                val amplitudes = intArrayOf(0, 130, 0, 130, 0, 130)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 80, 30, 80, 30), -1)
            }
        } catch (e: Exception) {
            Log.d("HapticFeedback", "Falha vibrateRainModeSurge: ${e.message}")
        }
    }

    /**
     * Feedback Háptico para Alta Rentabilidade calibrado para guidão de moto.
     */
    fun vibrateHighProfit(context: Context) {
        HapticFeedbackEngine.getInstance(context).vibrateHighProfit()
    }

    /**
     * Feedback Háptico para Corrida Longa calibrado para guidão de moto.
     */
    fun vibrateLongDistance(context: Context) {
        HapticFeedbackEngine.getInstance(context).vibrateLongDistance()
    }

    /**
     * Feedback Háptico para Rejeição Automática calibrado para guidão de moto.
     */
    fun vibrateAutoReject(context: Context) {
        HapticFeedbackEngine.getInstance(context).vibrateAutoReject()
    }
}
