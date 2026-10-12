package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Modelo de evento de webhook recebido ou simulado do Asaas.
 */
data class AsaasWebhookEvent(
    val id: String = UUID.randomUUID().toString(),
    val eventType: String, // PAYMENT_RECEIVED, PAYMENT_OVERDUE, PAYMENT_CREATED, PAYMENT_REFUNDED
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val paymentValue: Double,
    val planType: String, // Semanal (R$ 9,90), Mensal (R$ 29,90), Trimestral (R$ 79,90)
    val timestampFormatted: String,
    val activationKey: String,
    val whatsappStatus: String = "Pendente",
    val statusColor: String = "#00FF88"
)

/**
 * Configurações da infraestrutura de automação (VPS e Evolution API).
 */
data class AsaasAutomationConfig(
    val vpsWebhookUrl: String = "http://seu-ip-vps:8000/webhook/asaas",
    val evolutionApiUrl: String = "https://sua-evolution-api.com",
    val evolutionApiKey: String = "SUA_EVOLUTION_API_KEY",
    val evolutionInstanceName: String = "jarvis_whatsapp",
    val isAutoWelcomeEnabled: Boolean = true,
    val isRetentionRulesEnabled: Boolean = true,
    val autoApkDownloadUrl: String = "https://ais-dev-pg6paszmsamfrpcpczq7n7-321882160298.us-west2.run.app"
)

/**
 * Gerenciador reativo e persistente para eventos do Asaas e automações de WhatsApp.
 */
object AsaasWebhookSimulatorManager {

    private const val PREFS_NAME = "jarvis_asaas_automation_prefs"
    private const val KEY_EVENTS_JSON = "key_asaas_events_json"
    private const val KEY_CONFIG_JSON = "key_asaas_config_json"

    val events = mutableStateListOf<AsaasWebhookEvent>()
    var config: AsaasAutomationConfig = AsaasAutomationConfig()
        private set

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Carrega configurações
        val configJson = prefs.getString(KEY_CONFIG_JSON, null)
        if (!configJson.isNullOrEmpty()) {
            try {
                val obj = JSONObject(configJson)
                config = AsaasAutomationConfig(
                    vpsWebhookUrl = obj.optString("vpsWebhookUrl", config.vpsWebhookUrl),
                    evolutionApiUrl = obj.optString("evolutionApiUrl", config.evolutionApiUrl),
                    evolutionApiKey = obj.optString("evolutionApiKey", config.evolutionApiKey),
                    evolutionInstanceName = obj.optString("evolutionInstanceName", config.evolutionInstanceName),
                    isAutoWelcomeEnabled = obj.optBoolean("isAutoWelcomeEnabled", true),
                    isRetentionRulesEnabled = obj.optBoolean("isRetentionRulesEnabled", true),
                    autoApkDownloadUrl = obj.optString("autoApkDownloadUrl", config.autoApkDownloadUrl)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Carrega eventos salvos
        val eventsJson = prefs.getString(KEY_EVENTS_JSON, null)
        events.clear()
        if (!eventsJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(eventsJson)
                for (i in 0 until arr.length()) {
                    events.add(parseEventFromJson(arr.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (events.isEmpty()) {
            loadInitialDemoEvents(context)
        }

        isInitialized = true
    }

    private fun loadInitialDemoEvents(context: Context) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val now = System.currentTimeMillis()

        val demo1 = AsaasWebhookEvent(
            eventType = "PAYMENT_RECEIVED",
            customerId = "cus_0000492819",
            customerName = "Marcos Silveira (Motoboy SP)",
            customerPhone = "+55 11 98765-4321",
            paymentValue = 25.00,
            planType = "Semanal PIX (R$ 25,00) • Split 40%",
            timestampFormatted = sdf.format(Date(now - 1000L * 60 * 18)),
            activationKey = "JARVIS-PRO-9842-MS",
            whatsappStatus = "Licença 7 Dias Ativada (Split R$ 10,00 Líder)",
            statusColor = "#00FF88"
        )

        val demo2 = AsaasWebhookEvent(
            eventType = "PAYMENT_RECEIVED",
            customerId = "cus_0000518392",
            customerName = "Rodrigo Ferreira (Ifood Campinas)",
            customerPhone = "+55 19 99123-8877",
            paymentValue = 25.00,
            planType = "Semanal PIX (R$ 25,00) • Split 40%",
            timestampFormatted = sdf.format(Date(now - 1000L * 60 * 120)),
            activationKey = "JARVIS-PRO-1149-RF",
            whatsappStatus = "Licença 7 Dias Ativada (Split R$ 10,00 Líder)",
            statusColor = "#00FF88"
        )

        val demo3 = AsaasWebhookEvent(
            eventType = "PAYMENT_OVERDUE",
            customerId = "cus_0000381029",
            customerName = "Gabriel Duarte (Uber Moto)",
            customerPhone = "+55 21 97654-1122",
            paymentValue = 25.00,
            planType = "Semanal PIX (R$ 25,00)",
            timestampFormatted = sdf.format(Date(now - 1000L * 60 * 60 * 5)),
            activationKey = "JARVIS-EXPIRED-3810",
            whatsappStatus = "Acesso Bloqueado • Inadimplência",
            statusColor = "#FF5252"
        )

        events.addAll(listOf(demo1, demo2, demo3))
        saveEventsToPrefs(context)
    }

    /**
     * Registra ou simula um evento de webhook vindo do Asaas.
     */
    fun recordWebhookEvent(
        context: Context,
        eventType: String,
        customerId: String,
        customerName: String,
        customerPhone: String,
        paymentValue: Double,
        planType: String,
        whatsappStatus: String = "Enviado"
    ): AsaasWebhookEvent {
        if (!isInitialized) initialize(context)

        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        val randomSuffix = (1000..9999).random()
        val key = if (eventType == "PAYMENT_RECEIVED") "JARVIS-PRO-$randomSuffix" else "PENDING-$randomSuffix"

        val color = when (eventType) {
            "PAYMENT_RECEIVED" -> "#00FF88"
            "PAYMENT_OVERDUE" -> "#FF5252"
            else -> "#00F2FE"
        }

        val event = AsaasWebhookEvent(
            eventType = eventType,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            paymentValue = paymentValue,
            planType = planType,
            timestampFormatted = sdf.format(Date(now)),
            activationKey = key,
            whatsappStatus = whatsappStatus,
            statusColor = color
        )

        events.add(0, event)
        if (events.size > 50) {
            events.removeRange(50, events.size)
        }

        saveEventsToPrefs(context)

        // Aciona a regra de negócio do PASSO 1 no app
        AsaasFinancialEngine.processWebhookEvent(
            context = context,
            eventType = eventType,
            customerId = customerId,
            value = paymentValue
        )

        return event
    }

    fun updateConfig(context: Context, newConfig: AsaasAutomationConfig) {
        config = newConfig
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject().apply {
            put("vpsWebhookUrl", newConfig.vpsWebhookUrl)
            put("evolutionApiUrl", newConfig.evolutionApiUrl)
            put("evolutionApiKey", newConfig.evolutionApiKey)
            put("evolutionInstanceName", newConfig.evolutionInstanceName)
            put("isAutoWelcomeEnabled", newConfig.isAutoWelcomeEnabled)
            put("isRetentionRulesEnabled", newConfig.isRetentionRulesEnabled)
            put("autoApkDownloadUrl", newConfig.autoApkDownloadUrl)
        }
        prefs.edit().putString(KEY_CONFIG_JSON, obj.toString()).apply()
    }

    fun clearEvents(context: Context) {
        events.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_EVENTS_JSON).apply()
    }

    private fun saveEventsToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (ev in events) {
            arr.put(JSONObject().apply {
                put("id", ev.id)
                put("eventType", ev.eventType)
                put("customerId", ev.customerId)
                put("customerName", ev.customerName)
                put("customerPhone", ev.customerPhone)
                put("paymentValue", ev.paymentValue)
                put("planType", ev.planType)
                put("timestampFormatted", ev.timestampFormatted)
                put("activationKey", ev.activationKey)
                put("whatsappStatus", ev.whatsappStatus)
                put("statusColor", ev.statusColor)
            })
        }
        prefs.edit().putString(KEY_EVENTS_JSON, arr.toString()).apply()
    }

    private fun parseEventFromJson(obj: JSONObject): AsaasWebhookEvent {
        return AsaasWebhookEvent(
            id = obj.optString("id", UUID.randomUUID().toString()),
            eventType = obj.optString("eventType", "PAYMENT_RECEIVED"),
            customerId = obj.optString("customerId", "cus_default"),
            customerName = obj.optString("customerName", "Cliente"),
            customerPhone = obj.optString("customerPhone", ""),
            paymentValue = obj.optDouble("paymentValue", 29.90),
            planType = obj.optString("planType", "Mensal"),
            timestampFormatted = obj.optString("timestampFormatted", ""),
            activationKey = obj.optString("activationKey", "JARVIS-PRO"),
            whatsappStatus = obj.optString("whatsappStatus", "Enviado"),
            statusColor = obj.optString("statusColor", "#00FF88")
        )
    }

    /**
     * Monta o texto padronizado do WhatsApp conforme o tipo de evento.
     */
    fun buildWhatsAppMessage(event: AsaasWebhookEvent): String {
        return when (event.eventType) {
            "PAYMENT_RECEIVED" -> """
🚀 *PAGAMENTO CONFIRMADO - JARVIS NEURAL PRO* 🚀

Olá, *${event.customerName}*! Seu pagamento de *R$ ${String.format(Locale("pt", "BR"), "%.2f", event.paymentValue)}* referente ao plano *${event.planType}* foi processado com sucesso pelo Asaas!

🔑 *Sua Chave de Acesso Exclusiva:*
` ${event.activationKey} `

📲 *Download do APK Atualizado:*
${config.autoApkDownloadUrl}

🛡️ *Benefícios Imediatos Ativados:*
• Análise neural e rejeição automática de corridas lixo
• Cálculo de lucro real e R$/KM no HUD flutuante
• Relatórios de combustível economizado em tempo real

Qualquer dúvida, nosso suporte tático está à disposição!
            """.trimIndent()

            "PAYMENT_OVERDUE" -> """
⚠️ *AVISO IMPORTANTE - SEU ACESSO AO JARVIS PRO EXPIROU* ⚠️

Olá, *${event.customerName}*! Notamos que a assinatura do seu plano *${event.planType}* (R$ ${String.format(Locale("pt", "BR"), "%.2f", event.paymentValue)}) não foi renovada no Asaas.

🚨 *O que acontece sem o Jarvis ligado:*
• Você voltará a pegar corridas predatórias com taxa abaixo de R$ 2,50/KM.
• Seu gasto médio de combustível pode aumentar em até R$ 45,00/dia.
• O HUD flutuante inteligente ficará bloqueado.

💡 *Renove agora mesmo e mantenha seu lucro protegido:*
https://www.asaas.com/c/${event.customerId}

Caso já tenha pago, ignore esta mensagem e seu acesso será reativado em instantes!
            """.trimIndent()

            else -> """
📢 *JARVIS MARKETING - ATUALIZAÇÃO DE CONTA*

Olá, ${event.customerName}! Identificamos um novo evento (${event.eventType}) na sua conta Asaas.
Acesse o app para consultar seu painel de telemetria!
            """.trimIndent()
        }
    }
}
