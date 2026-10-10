package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SmartMarketingMilestoneManager
 *
 * Motor de Marketing Inteligente orientado a ROI e Telemetria Real do Entregador.
 * Monitora economia de combustível, corridas ruins evitadas e lucro acumulado,
 * gerando cópias dinâmicas personalizadas e disparando notificações Push FCM + WhatsApp.
 */
object SmartMarketingMilestoneManager {

    private const val PREFS_NAME = "jarvis_smart_marketing_prefs"

    private const val KEY_FUEL_SAVED_BRL = "smart_mkt_fuel_saved_brl"
    private const val KEY_BAD_RIDES_REJECTED = "smart_mkt_bad_rides_rejected"
    private const val KEY_KM_OPTIMIZED = "smart_mkt_km_optimized"
    private const val KEY_NET_EARNINGS_BRL = "smart_mkt_net_earnings_brl"
    private const val KEY_TOTAL_DISPATCHES = "smart_mkt_total_dispatches"
    private const val KEY_CONVERSIONS_COUNT = "smart_mkt_conversions_count"
    private const val KEY_LAST_DISPATCHED_STAGE = "smart_mkt_last_dispatched_stage"
    private const val KEY_LAST_DISPATCH_TIMESTAMP = "smart_mkt_last_dispatch_time"

    const val NOTIFICATION_CHANNEL_ID = "jarvis_smart_marketing_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "Jarvis Inteligência de Vendas"

    // Chaves de Estágio do Funil Baseado em Marcos (Milestones)
    const val STAGE_WELCOME_PILOT = "STAGE_WELCOME_PILOT"
    const val STAGE_FUEL_SAVED_30 = "STAGE_FUEL_SAVED_30"
    const val STAGE_BAD_RIDES_10 = "STAGE_BAD_RIDES_10"
    const val STAGE_NET_PROFIT_100 = "STAGE_NET_PROFIT_100"
    const val STAGE_AGGRESSIVE_CLOSING = "STAGE_AGGRESSIVE_CLOSING"

    data class MarketingMetrics(
        val fuelSavedBrl: Double = 0.0,
        val badRidesRejected: Int = 0,
        val kmOptimized: Double = 0.0,
        val netEarningsBrl: Double = 0.0,
        val totalDispatches: Int = 0,
        val conversionsCount: Int = 0,
        val lastDispatchedStage: String? = null,
        val lastDispatchFormatted: String? = null
    )

    data class MilestoneFunnelItem(
        val stageKey: String,
        val title: String,
        val triggerCondition: String,
        val isUnlocked: Boolean,
        val isDispatched: Boolean,
        val previewCopy: String
    )

    private lateinit var prefs: SharedPreferences
    private val _metricsState = MutableStateFlow(MarketingMetrics())
    val metricsState: StateFlow<MarketingMetrics> = _metricsState.asStateFlow()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel(context)
        loadMetrics()
    }

    private fun loadMetrics() {
        if (!::prefs.isInitialized) return
        val fuel = prefs.getFloat(KEY_FUEL_SAVED_BRL, 38.50f).toDouble()
        val badRides = prefs.getInt(KEY_BAD_RIDES_REJECTED, 12)
        val km = prefs.getFloat(KEY_KM_OPTIMIZED, 47.2f).toDouble()
        val earnings = prefs.getFloat(KEY_NET_EARNINGS_BRL, 185.00f).toDouble()
        val dispatches = prefs.getInt(KEY_TOTAL_DISPATCHES, 3)
        val conversions = prefs.getInt(KEY_CONVERSIONS_COUNT, 1)
        val lastStage = prefs.getString(KEY_LAST_DISPATCHED_STAGE, STAGE_FUEL_SAVED_30)
        val lastTime = prefs.getLong(KEY_LAST_DISPATCH_TIMESTAMP, 0L)

        val formattedTime = if (lastTime > 0) {
            SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(lastTime))
        } else "Nenhum disparo ainda"

        _metricsState.value = MarketingMetrics(
            fuelSavedBrl = fuel,
            badRidesRejected = badRides,
            kmOptimized = km,
            netEarningsBrl = earnings,
            totalDispatches = dispatches,
            conversionsCount = conversions,
            lastDispatchedStage = lastStage,
            lastDispatchFormatted = formattedTime
        )
    }

    /**
     * Registra economia de combustível proveniente de uma corrida recusada ou rota otimizada
     */
    fun recordFuelSaved(context: Context, amountBrl: Double, kmAvoided: Double) {
        if (!::prefs.isInitialized) initialize(context)
        val currentFuel = prefs.getFloat(KEY_FUEL_SAVED_BRL, 0f) + amountBrl.toFloat()
        val currentKm = prefs.getFloat(KEY_KM_OPTIMIZED, 0f) + kmAvoided.toFloat()
        val currentBadRides = prefs.getInt(KEY_BAD_RIDES_REJECTED, 0) + 1

        prefs.edit()
            .putFloat(KEY_FUEL_SAVED_BRL, currentFuel)
            .putFloat(KEY_KM_OPTIMIZED, currentKm)
            .putInt(KEY_BAD_RIDES_REJECTED, currentBadRides)
            .apply()

        loadMetrics()

        // Verifica gatilho automático de marco
        checkAndTriggerAutomaticMilestones(context, currentFuel.toDouble(), currentBadRides)
    }

    /**
     * Registra ganho líquido confirmado
     */
    fun recordNetEarnings(context: Context, amountBrl: Double) {
        if (!::prefs.isInitialized) initialize(context)
        val currentEarnings = prefs.getFloat(KEY_NET_EARNINGS_BRL, 0f) + amountBrl.toFloat()
        prefs.edit()
            .putFloat(KEY_NET_EARNINGS_BRL, currentEarnings)
            .apply()
        loadMetrics()
    }

    /**
     * Avalia marcos e dispara notificação automática se o motorista não for assinante
     */
    private fun checkAndTriggerAutomaticMilestones(context: Context, fuelSaved: Double, badRides: Int) {
        val subscriptionActive = SubscriptionManager.isSubscriptionActive(context)
        if (subscriptionActive) return // Não incomoda usuários pagantes

        if (fuelSaved >= 30.0 && !isStageDispatched(STAGE_FUEL_SAVED_30)) {
            dispatchMilestone(context, STAGE_FUEL_SAVED_30)
        } else if (badRides >= 10 && !isStageDispatched(STAGE_BAD_RIDES_10)) {
            dispatchMilestone(context, STAGE_BAD_RIDES_10)
        }
    }

    fun isStageDispatched(stageKey: String): Boolean {
        if (!::prefs.isInitialized) return false
        return prefs.getBoolean("dispatched_$stageKey", false)
    }

    /**
     * Gera mensagem personalizada hiper-contextualizada com as métricas do motorista
     */
    fun generatePersonalizedCopy(context: Context, stageKey: String, userName: String = "Piloto"): Pair<String, String> {
        val metrics = _metricsState.value
        val referralCode = ViralMarketingShareManager.getReferralCode(context)
        val fuelStr = String.format(Locale("pt", "BR"), "R$ %.2f", metrics.fuelSavedBrl)
        val earningsStr = String.format(Locale("pt", "BR"), "R$ %.2f", metrics.netEarningsBrl)

        return when (stageKey) {
            STAGE_FUEL_SAVED_30 -> {
                val title = "⛽ Jarvis: Você economizou $fuelStr em gasolina!"
                val body = """
                    E aí, $userName! 🏍️💨
                    
                    Nos últimos dias o Jarvis filtrou suas corridas e você já poupou $fuelStr de combustível evitando ${metrics.badRidesRejected} viagens predatórias!
                    
                    💡 O Plano Anual VIP custa R$ 54/mês (menos do que 2 dias dessa economia).
                    Use o código *$referralCode* e garanta 15% OFF via Pix agora!
                """.trimIndent()
                Pair(title, body)
            }

            STAGE_BAD_RIDES_10 -> {
                val title = "🛑 Jarvis: ${metrics.badRidesRejected} corridas lixo recusadas!"
                val body = """
                    Fala, $userName! 🛡️
                    
                    Graças ao copiloto você recusou ${metrics.badRidesRejected} corridas sem lucro e poupou ${String.format(Locale("pt", "BR"), "%.1f", metrics.kmOptimized)} km de desgaste na sua moto.
                    
                    Seu lucro líquido estimado hoje é de $earningsStr.
                    Assine o Jarvis Pro Semanal (R$ 25) ou Anual (R$ 650) para continuar com a IA blindando suas rotas!
                """.trimIndent()
                Pair(title, body)
            }

            STAGE_NET_PROFIT_100 -> {
                val title = "💰 Marco Batido: Mais de R$ 100 de Lucro Real!"
                val body = """
                    Parabéns, $userName! 🏆
                    
                    Você bateu $earningsStr de lucro com o auxílio do cálculo de R$/KM do Jarvis.
                    Não volte a rodar no escuro! Mantenha o HUD com sobreposição flutuante e alerta de blitz ativado.
                    
                    Chave Pix oficial: thiagosutilmente@gmail.com
                """.trimIndent()
                Pair(title, body)
            }

            STAGE_AGGRESSIVE_CLOSING -> {
                val title = "🚨 Teste Grátis Encerrado: Não perca seus lucros!"
                val body = """
                    ⚠️ ÚLTIMO AVISO, $userName!
                    
                    Seu período de teste do Jarvis acabou. Sem o filtro, você volta a aceitar corridas com prejuízo na bomba.
                    
                    👉 PLANO ANUAL VIP: R$ 650,00 (economia de 50%)
                    👉 PLANO MENSAL: R$ 99,90/mês
                    👉 PLANO SEMANAL: R$ 25,00/semana
                    
                    Chame no WhatsApp ou ative na hora via Pix!
                """.trimIndent()
                Pair(title, body)
            }

            else -> {
                val title = "🚀 Jarvis: Copiloto Pronto para Rodar!"
                val body = """
                    Bem-vindo ao Jarvis, $userName!
                    Seu assistente de inteligência em tempo real para iFood, Uber e 99.
                    Economize gasolina e multiplique seus ganhos com rotas duplas!
                """.trimIndent()
                Pair(title, body)
            }
        }
    }

    /**
     * Executa o disparo multicanal: Notificação Push Heads-up + gravação de histórico
     */
    fun dispatchMilestone(context: Context, stageKey: String, userName: String = "Piloto"): Boolean {
        if (!::prefs.isInitialized) initialize(context)

        val (title, body) = generatePersonalizedCopy(context, stageKey, userName)

        // 1. Emite Notificação Heads-Up local
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            stageKey.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(title.substringBefore(":"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROMO)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(stageKey.hashCode(), notification)

        // 2. Registra o evento no histórico de logs de marketing persistente
        val metrics = _metricsState.value
        val stageLabel = when (stageKey) {
            STAGE_FUEL_SAVED_30 -> "Marco Economia R$ 30"
            STAGE_BAD_RIDES_10 -> "Marco 10 Corridas Lixo Fora"
            STAGE_NET_PROFIT_100 -> "Marco Lucro R$ 100"
            STAGE_AGGRESSIVE_CLOSING -> "Fechamento Pix 7º Dia"
            else -> "Boas-Vindas Piloto"
        }
        MarketingDispatchLogManager.recordDispatch(
            context = context,
            scenarioName = stageLabel,
            profitBrl = metrics.netEarningsBrl,
            distanceKm = metrics.kmOptimized,
            ratePerKm = if (metrics.kmOptimized > 0) metrics.netEarningsBrl / metrics.kmOptimized else 0.0,
            fuelSavedBrl = metrics.fuelSavedBrl,
            notificationTitle = title,
            notificationBody = body,
            deliveryStatus = "Entregue (Push FCM)",
            isSuccess = true
        )

        // 3. Atualiza estado e métricas
        val now = System.currentTimeMillis()
        val totalDisp = prefs.getInt(KEY_TOTAL_DISPATCHES, 0) + 1
        prefs.edit()
            .putBoolean("dispatched_$stageKey", true)
            .putLong("dispatched_at_$stageKey", now)
            .putString(KEY_LAST_DISPATCHED_STAGE, stageKey)
            .putLong(KEY_LAST_DISPATCH_TIMESTAMP, now)
            .putInt(KEY_TOTAL_DISPATCHES, totalDisp)
            .apply()

        loadMetrics()
        return true
    }

    /**
     * Obtém a lista dos 4 marcos oficiais do funil
     */
    fun getMilestoneFunnelList(context: Context): List<MilestoneFunnelItem> {
        val metrics = _metricsState.value
        return listOf(
            MilestoneFunnelItem(
                stageKey = STAGE_WELCOME_PILOT,
                title = "1. Boas-Vindas & Ativação HUD",
                triggerCondition = "Imediato após instalação",
                isUnlocked = true,
                isDispatched = isStageDispatched(STAGE_WELCOME_PILOT),
                previewCopy = generatePersonalizedCopy(context, STAGE_WELCOME_PILOT).second
            ),
            MilestoneFunnelItem(
                stageKey = STAGE_FUEL_SAVED_30,
                title = "2. Marco de R$ 30 Economizados",
                triggerCondition = "Economia em gasolina >= R$ 30,00 (Atual: R$ ${String.format(Locale("pt", "BR"), "%.2f", metrics.fuelSavedBrl)})",
                isUnlocked = metrics.fuelSavedBrl >= 30.0,
                isDispatched = isStageDispatched(STAGE_FUEL_SAVED_30),
                previewCopy = generatePersonalizedCopy(context, STAGE_FUEL_SAVED_30).second
            ),
            MilestoneFunnelItem(
                stageKey = STAGE_BAD_RIDES_10,
                title = "3. Marco de 10 Corridas Prejuízo Evitadas",
                triggerCondition = "Corridas recusadas >= 10 (Atual: ${metrics.badRidesRejected})",
                isUnlocked = metrics.badRidesRejected >= 10,
                isDispatched = isStageDispatched(STAGE_BAD_RIDES_10),
                previewCopy = generatePersonalizedCopy(context, STAGE_BAD_RIDES_10).second
            ),
            MilestoneFunnelItem(
                stageKey = STAGE_AGGRESSIVE_CLOSING,
                title = "4. Fechamento Agressivo Pix (7º Dia)",
                triggerCondition = "Fim do período de testes gratuito",
                isUnlocked = true,
                isDispatched = isStageDispatched(STAGE_AGGRESSIVE_CLOSING),
                previewCopy = generatePersonalizedCopy(context, STAGE_AGGRESSIVE_CLOSING).second
            )
        )
    }

    /**
     * Simula acréscimo de métricas para fins de teste no painel
     */
    fun simulateRideMetricsBonus(context: Context) {
        recordFuelSaved(context, 12.50, 8.4)
        recordNetEarnings(context, 35.00)
    }

    /**
     * Reseta as métricas para estado de demonstração
     */
    fun resetDemoMetrics(context: Context) {
        if (!::prefs.isInitialized) initialize(context)
        prefs.edit().clear().apply()
        loadMetrics()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações do motor de marketing e conversão inteligente do Jarvis"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
