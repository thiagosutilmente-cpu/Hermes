package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * Worker do WorkManager para execução em segundo plano do funil de marketing automatizado.
 * Dispara mensagens via API de mensageria e emite alertas na barra de notificações do celular.
 */
class MarketingFunnelWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "MarketingFunnelWorker"

        const val KEY_STAGE = "key_stage"
        const val KEY_PHONE = "key_phone"
        const val KEY_USER_NAME = "key_user_name"
        const val KEY_DRY_RUN = "key_dry_run"

        const val STAGE_DAY_1_WELCOME = "DAY_1_WELCOME"
        const val STAGE_DAY_3_ENGAGEMENT = "DAY_3_ENGAGEMENT"
        const val STAGE_DAY_7_ANNUAL_CLOSE = "DAY_7_ANNUAL_CLOSE"

        private const val CHANNEL_ID = "jarvis_marketing_funnel_channel"
        private const val CHANNEL_NAME = "Jarvis Funil de Vendas"
    }

    override suspend fun doWork(): Result {
        val stage = inputData.getString(KEY_STAGE) ?: STAGE_DAY_1_WELCOME
        val phone = inputData.getString(KEY_PHONE) ?: "5511999999999"
        val userName = inputData.getString(KEY_USER_NAME) ?: "Piloto"
        val isDryRun = inputData.getBoolean(KEY_DRY_RUN, false)

        Log.d(TAG, "Executando WorkManager para o estágio: $stage (Telefone: $phone, DryRun: $isDryRun)")

        val referralCode = ViralMarketingShareManager.getReferralCode(context)

        // Conteúdo da mensagem e notificação de acordo com o estágio do funil
        val (notificationTitle, notificationBody, fullMessageText) = when (stage) {
            STAGE_DAY_3_ENGAGEMENT -> Triple(
                "⛽ Jarvis: Economizou combustível hoje?",
                "Veja como a dica da Rota Dupla duplica seus ganhos na mesma viagem de moto!",
                """
E aí, $userName! Beleza? ⛽

Passando pra saber: já sentiu a diferença no bolso?
Com o Jarvis filtrando corridas lixo de R$ 6,00, a média dos entregadores economiza entre R$ 20 e R$ 35 de gasolina por turno.

🔥 DICA DE OURO: Quando tocar iFood e 99 pro mesmo bairro, o Jarvis te avisa pra fazer a ROTA DUPLA e dobrar o ganho!
                """.trimIndent()
            )

            STAGE_DAY_7_ANNUAL_CLOSE -> Triple(
                "🚨 OFERTA AGRESSIVA: Seu Teste de 7 Dias Acabou!",
                "Garanta o Plano Anual por R$ 650 (R$ 54/mês) ou Semanal de R$ 25 antes que o filtro expire!",
                """
🚨 ATENÇÃO, $userName! SEU TESTE GRÁTIS DE 7 DIAS ENCERRA HOJE! 🚨

Nessa semana de teste, o Jarvis filtrou suas corridas, evitou prejuízos e te avisou de blitz no fone.

Pra não ficar na mão e continuar rodando com o copiloto:
👉 PLANO ANUAL VIP (MAIOR ECONOMIA): Apenas R$ 650,00/ano (sai a R$ 54,16/mês via Pix!)
👉 PLANO MENSAL: R$ 99,90/mês com flexibilidade total.
👉 PLANO SEMANAL: Apenas R$ 25,00/semana no Pix Automático (menos que uma entrega curta).

Abra o Jarvis agora e toque em 'Ativar Plano no Pix' para manter a Rota Dupla e o Filtro ativos. Boas corridas! 👊
                """.trimIndent()
            )

            else -> Triple(
                "🚀 Jarvis: Seu Teste de 7 Dias Começou!",
                "Ative a bolha flutuante e conecte o fone Bluetooth para rodar protegido hoje.",
                """
Fala, $userName! 🚀 Aqui é o Jarvis.

Seu teste grátis de 7 DIAS começou agora!
1️⃣ Ative a Bolha Flutuante sobre os apps de entrega.
2️⃣ Ligue o fone Bluetooth para alertas de Blitz e Radar.
3️⃣ Deixe o Filtro Inteligente barrar corridas abaixo de R$ 4,50/km.
Boas corridas!
                """.trimIndent()
            )
        }

        // 1. Despacha mensagem para a API de Mensageria (WhatsApp / Webhook)
        val sendResult = MessagingApiClient.sendMessage(
            context = context,
            phone = phone,
            messageText = fullMessageText,
            stage = stage
        )

        // 2. Emite Notificação Heads-Up local no dispositivo do condutor
        showLocalFunnelNotification(
            stage = stage,
            title = notificationTitle,
            body = notificationBody
        )

        // 3. Registra execução histórica no agendador
        MarketingFunnelScheduler.recordStageDispatched(
            context = context,
            stage = stage,
            phone = phone,
            success = sendResult.success
        )

        val output = workDataOf(
            "stage" to stage,
            "success" to sendResult.success,
            "messageId" to (sendResult.messageId ?: "none"),
            "details" to sendResult.details
        )

        return Result.success(output)
    }

    private fun showLocalFunnelNotification(stage: String, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Disparos e lembretes automáticos do funil de vendas do Jarvis"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_PAYWALL", stage == STAGE_DAY_7_ANNUAL_CLOSE)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stage.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = when (stage) {
            STAGE_DAY_1_WELCOME -> 9001
            STAGE_DAY_3_ENGAGEMENT -> 9003
            STAGE_DAY_7_ANNUAL_CLOSE -> 9007
            else -> 9000
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
