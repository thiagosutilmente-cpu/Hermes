package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * Gerenciador de alertas sonoros de venda e notificações VIP.
 * Sintetiza o som autêntico de caixa registradora ("Cha-Ching! 💰") via AudioTrack
 * nativo do Android sem necessidade de arquivos externos, com garantia de execução offline.
 */
object SaleAlertSoundManager {

    private const val CHANNEL_ID = "channel_vip_sales_alerts"
    private const val CHANNEL_NAME = "Alertas de Vendas VIP (Cha-Ching)"

    /**
     * Toca o som autêntico de caixa registradora ("Cha-Ching! 💰").
     * Combina um clique mecânico inicial com duas notas ressonantes metálicas brilhantes.
     */
    suspend fun playCashRegisterSound(context: Context) {
        withContext(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                // Duração total: ~0.8 segundos
                val numSamples = (sampleRate * 0.8).toInt()
                val samples = ShortArray(numSamples)

                val clickDuration = (sampleRate * 0.05).toInt()
                val bell1Duration = (sampleRate * 0.35).toInt()
                val bell2Duration = numSamples - clickDuration - bell1Duration

                // Frequências ricas metálicas
                val freqBell1 = 987.77 // Si5 (B5)
                val freqBell2 = 1318.51 // Mi6 (E6) - Acorde brilhante

                var idx = 0

                // 1. Clique mecânico da gaveta abrindo
                for (i in 0 until clickDuration) {
                    val progress = i.toDouble() / clickDuration
                    val envelope = 1.0 - progress
                    val noise = (Math.random() * 2.0 - 1.0) * 0.6
                    val tone = sin(2.0 * Math.PI * 400.0 * i / sampleRate) * 0.4
                    val value = ((noise + tone) * envelope * Short.MAX_VALUE * 0.5).toInt()
                    samples[idx++] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                // 2. Primeiro sino brilhante ("Cha-")
                for (i in 0 until bell1Duration) {
                    val progress = i.toDouble() / bell1Duration
                    val envelope = Math.exp(-progress * 5.0)
                    val s1 = sin(2.0 * Math.PI * freqBell1 * i / sampleRate)
                    val s2 = sin(2.0 * Math.PI * (freqBell1 * 2.0) * i / sampleRate) * 0.3
                    val s3 = sin(2.0 * Math.PI * (freqBell1 * 3.0) * i / sampleRate) * 0.15
                    val value = ((s1 + s2 + s3) * envelope * Short.MAX_VALUE * 0.75).toInt()
                    samples[idx++] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                // 3. Segundo sino brilhante sustentado ("-Ching! 💰")
                for (i in 0 until bell2Duration) {
                    val progress = i.toDouble() / bell2Duration
                    val envelope = Math.exp(-progress * 3.5)
                    val s1 = sin(2.0 * Math.PI * freqBell2 * i / sampleRate)
                    val s2 = sin(2.0 * Math.PI * (freqBell2 * 2.0) * i / sampleRate) * 0.35
                    val s3 = sin(2.0 * Math.PI * (freqBell2 * 2.75) * i / sampleRate) * 0.2
                    val value = ((s1 + s2 + s3) * envelope * Short.MAX_VALUE * 0.85).toInt()
                    samples[idx++] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
            } catch (e: Exception) {
                // Fallback silencioso seguro
            }
        }
    }

    /**
     * Envia notificação local de alta visibilidade com som comemorativo.
     */
    fun showSaleNotification(context: Context, motoboyName: String, value: Double = 19.90) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações instantâneas de novas assinaturas VIP pagas"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("💰 VENDA 100% AUTOMÁTICA CONFIRMADA!")
                .setContentText("Novo Pix de R$ ${"%.2f".format(value)} recebido de $motoboyName. Conta liberada na VPS!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            // Silencioso em caso de permissão pendente
        }
    }
}
