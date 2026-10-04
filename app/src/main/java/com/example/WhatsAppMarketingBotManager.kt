package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gerenciador da Automação do Robô de WhatsApp na VPS.
 * Controla os disparos automáticos nos dias 1, 3 e 7 de teste grátis para conversão em assinantes.
 */
object WhatsAppMarketingBotManager {

    private const val VPS_BASE_URL = "http://187.77.248.73:8080"

    data class BotMessageSchedule(
        val day: Int,
        val stageTitle: String,
        val messageText: String,
        val callToAction: String,
        val isSent: Boolean = false
    )

    /**
     * Retorna os 3 roteiros oficiais de mensagens que o Robô do WhatsApp dispara
     */
    fun getOfficialBotFunnel(phone: String, referralCode: String): List<BotMessageSchedule> {
        return listOf(
            BotMessageSchedule(
                day = 1,
                stageTitle = "Dia 1 • Boas-Vindas & Ativação Imediata",
                messageText = """
Fala piloto! 🚀 Aqui é o Jarvis, seu copiloto neural de rotas.

Seu teste grátis de *7 DIAS* começou agora!
Pra tirar o máximo proveito hoje na rua:
1️⃣ Deixe a *Bolha Flutuante* ativa sobre os apps de entrega.
2️⃣ Coloque seu fone de ouvido para ouvir alertas de *Blitz e Radares*.
3️⃣ Ligue o *Filtro Inteligente* para ignorar corridas abaixo de R$ 4,50/km.

Qualquer dúvida, manda aqui. Boas corridas e piloto com segurança! 🏍️
                """.trimIndent(),
                callToAction = "Ativar Bolha e Fone"
            ),
            BotMessageSchedule(
                day = 3,
                stageTitle = "Dia 3 • Engajamento & Economia de Gasolina",
                messageText = """
E aí, parceiro! Beleza? ⛽

Passando pra saber: já viu a diferença no tanque da moto?
Com o Jarvis recusando corrida lixo de R$ 6,00, a média dos pilotos é economizar entre *R$ 20 e R$ 35 de combustível por turno*.

🔥 *Dica de Ouro de Hoje:*
Quando tocar chamada do iFood e da 99 pro mesmo bairro, o Jarvis te avisa pra fazer a *Rota Dupla* e dobrar o ganho da mesma viagem!
                """.trimIndent(),
                callToAction = "Ver Dica de Rota Dupla"
            ),
            BotMessageSchedule(
                day = 7,
                stageTitle = "Dia 7 • A Oferta Agressiva de Fechamento (Conversão)",
                messageText = """
🚨 *ATENÇÃO, PILOTO! SEU TESTE DE 7 DIAS ENCERRA HOJE.* 🚨

Nessa semana de teste, o Jarvis filtrou suas corridas e te ajudou a economizar horas e gasolina na rua.

Pra você não ficar na mão e continuar rodando com o copiloto:
👉 *Plano Semanal:* Apenas *R$ 25,00/semana* (menos que 1 corrida curta!)
👉 *Plano Anual Especial:* De R$ 1.200 por apenas *R$ 650,00* em até 12x (sai R$ 54/mês!).

Chave Pix Copia e Cola gerada no app.
Basta abrir o Jarvis e clicar em 'Manter Copiloto Ativo'. Tamo junto na pista! 👊
                """.trimIndent(),
                callToAction = "Pagar Pix e Renovar Copiloto"
            )
        )
    }

    /**
     * Registra o lead na VPS para iniciar a sequência de mensagens no WhatsApp
     */
    suspend fun registerWhatsAppLead(
        context: Context,
        phone: String,
        name: String = "Piloto Jarvis"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$VPS_BASE_URL/api/whatsapp-lead")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 7000
                readTimeout = 7000
                doOutput = true
            }

            val referralCode = ViralMarketingShareManager.getReferralCode(context)
            val payload = JSONObject().apply {
                put("phone", phone.replace("[^0-9]".toRegex(), ""))
                put("name", name)
                put("referralCode", referralCode)
                put("source", "android_app_trial")
                put("trialDays", 7)
                put("timestamp", System.currentTimeMillis())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode in 200..299) {
                Result.success("Lead cadastrado no robô com sucesso: $responseText")
            } else {
                Result.failure(Exception("HTTP $responseCode: $responseText"))
            }
        } catch (e: Exception) {
            // Em caso de offline, não bloqueia o motoboy
            Result.failure(e)
        }
    }
}
