package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * RealWorldNotificationHarness
 *
 * Gerador e disparador de notificações REAIS do sistema Android na barra de status.
 * Emite notificações com formato e metadados idênticos aos do iFood e da 99 Food,
 * permitindo ao entregador testar no próprio celular como o sistema intercepta,
 * avalia e mescla os pedidos na prática sem depender de estar na rua.
 */
object RealWorldNotificationHarness {

    private const val TAG = "RealWorldHarness"
    private const val CHANNEL_ID = "delivery_real_test_channel"
    private const val NOTIF_ID_IFOOD = 7701
    private const val NOTIF_ID_99 = 7702

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Simulação de Entregas Reais"
            val descriptionText = "Canal de teste para verificação em campo da mesclagem de pedidos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Dispara sequência de duas notificações reais com intervalo de 2,5 segundos
     * para testar a detecção de mesclagem na prática.
     */
    fun triggerDualSimulation(
        context: Context,
        onFirstPosted: (() -> Unit)? = null,
        onSecondPosted: (() -> Unit)? = null
    ) {
        ensureChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = NotificationManagerCompat.from(context)

        // 1. Notificação Real do iFood
        val ifoodNotif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.radar_delivery_logo_1788290986448)
            .setContentTitle("iFood Entregador")
            .setContentText("Nova entrega: Outback Shopping Paulista - R$ 24,50 (3.5 km)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Nova entrega disponível!\nRestaurante: Outback Shopping Paulista\nGanho: R$ 24,50\nDistância: 3.5 km\nTempo estimado: 14 min")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        try {
            notificationManager.notify(NOTIF_ID_IFOOD, ifoodNotif)
            Log.d(TAG, "Notificação iFood disparada na barra de status")
            onFirstPosted?.invoke()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida: ${e.message}")
        }

        // 2. Notificação Real da 99 Food (Após 2,5 segundos)
        Handler(Looper.getMainLooper()).postDelayed({
            val food99Notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.radar_delivery_logo_1788290986448)
                .setContentTitle("99 Food Motoboy")
                .setContentText("Nova corrida: Burger King Shopping Paulista - R$ 19,00 (4.1 km)")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Oferta de corrida recebida!\nColeta: Burger King Shopping Paulista\nGanho: R$ 19,00\nDistância: 4.1 km\nTempo: 16 min")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 300, 100, 300))
                .build()

            try {
                notificationManager.notify(NOTIF_ID_99, food99Notif)
                Log.d(TAG, "Notificação 99 Food disparada na barra de status")
                onSecondPosted?.invoke()
            } catch (e: SecurityException) {
                Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida para 99: ${e.message}")
            }
        }, 2500L)
    }
}
