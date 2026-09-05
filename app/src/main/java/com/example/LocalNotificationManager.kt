package com.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Gerenciador de Notificações Locais do Radar Coordinator.
 * Responsável por emitir notificações pop-up (Heads-Up) de alta prioridade quando
 * o aplicativo estiver em segundo plano e uma oferta vantajosa (ganho/km elevado)
 * for interceptada pelo radar.
 */
class LocalNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "radar_high_priority_offers"
        const val CHANNEL_NAME = "Ofertas de Alta Prioridade"
        const val CHANNEL_DESC = "Alertas imediatos de pedidos com alta rentabilidade (R$/km)"

        const val EXTRA_ACTION = "com.example.EXTRA_NOTIFICATION_ACTION"
        const val ACTION_ACCEPT = "ACTION_ACCEPT_OFFER"
        const val ACTION_DECLINE = "ACTION_DECLINE_OFFER"
        const val ACTION_OPEN_RADAR = "ACTION_OPEN_RADAR"

        const val EXTRA_OFFER_ID = "com.example.EXTRA_OFFER_ID"
        const val EXTRA_OFFER_APP = "com.example.EXTRA_OFFER_APP"
        const val EXTRA_OFFER_VALUE = "com.example.EXTRA_OFFER_VALUE"
        const val EXTRA_OFFER_DISTANCE = "com.example.EXTRA_OFFER_DISTANCE"
        const val EXTRA_OFFER_GAIN_KM = "com.example.EXTRA_OFFER_GAIN_KM"

        const val NOTIFICATION_ID_BASE = 7000
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.parseColor("#00FF88") // Verde Neon Jarvis
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250) // Pulso duplo tático
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            systemManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Verifica se o app possui permissão para emitir notificações (Android 13+).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Emite uma notificação Heads-Up flutuante de alta prioridade para o entregador.
     */
    @SuppressLint("MissingPermission")
    fun showHighPriorityOfferNotification(offer: RadarOffer) {
        if (!hasNotificationPermission()) return

        val formattedValue = String.format(Locale.GERMANY, "R$ %.2f", offer.value)
        val formattedPerKm = String.format(Locale.GERMANY, "R$ %.2f/km", offer.gainPerKm)

        val notificationId = NOTIFICATION_ID_BASE + (offer.id.hashCode() % 1000)

        // 1. Intent para abrir o Radar Coordinator ao tocar no corpo da notificação
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_RADAR
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_OPEN_RADAR)
            putExtra(EXTRA_OFFER_ID, offer.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Ação direta: ACEITAR PEDIDO
        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_ACCEPT)
            putExtra(EXTRA_OFFER_ID, offer.id)
            putExtra(EXTRA_OFFER_VALUE, offer.value)
            putExtra(EXTRA_OFFER_APP, offer.appName)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Ação direta: DESCARTAR
        val declineIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_DECLINE
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_DECLINE)
            putExtra(EXTRA_OFFER_ID, offer.id)
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIconBadge = when (offer.appName) {
            "iFood" -> "🔴 iFood"
            "Rappi" -> "🟠 Rappi"
            "Uber Direct", "Uber Flash" -> "⚫ Uber"
            "99Food", "99 Entrega" -> "🟡 99"
            else -> "📦 ${offer.appName}"
        }

        val bigText = StringBuilder().apply {
            append("🍔 ${offer.restaurant}\n")
            append("🛵 ${offer.distanceKm} km • Tempo estimado: ${offer.estimatedTimeMin} min\n")
            append("⚡ Rentabilidade: $formattedPerKm • Lucro líq.: R$ ${String.format(Locale.GERMANY, "%.2f", offer.netProfit)}\n")
            append("🧠 IA Jarvis: ${offer.neuralDecision.reason}")
        }.toString()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎯 ALTA PRIORIDADE: $formattedValue ($appIconBadge)")
            .setContentText("🍔 ${offer.restaurant} • 🛵 ${offer.distanceKm} km • $formattedPerKm")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setSummaryText("Radar Neural Jarvis • 95% Lucrativo")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setColor(Color.parseColor("#00FF88"))
            .setAutoCancel(true)
            .setOngoing(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                "✅ ACEITAR ($formattedValue)",
                acceptPendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "❌ RECUSAR",
                declinePendingIntent
            )

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Cancela a notificação de um pedido específico.
     */
    fun cancelOfferNotification(offerId: String) {
        val notificationId = NOTIFICATION_ID_BASE + (offerId.hashCode() % 1000)
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancela todas as notificações de ofertas ativas.
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
}
