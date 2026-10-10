package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modelo de anúncio para tráfego pago (Meta Ads / TikTok Ads).
 */
data class PaidTrafficCampaign(
    val id: String,
    val platform: String, // "Meta Ads" ou "TikTok Ads"
    val title: String,
    val headline: String,
    val primaryText: String,
    val visualHookDescription: String,
    val callToAction: String,
    val targetAudience: String,
    val suggestedDailyBudget: String,
    val estimatedCpaBrl: Double,
    val utmLink: String
)

/**
 * Modelo de estratégia de tráfego orgânico (Reels, TikTok, Shorts e Grupos).
 */
data class OrganicTrafficStrategy(
    val id: String,
    val channel: String,
    val title: String,
    val format: String, // Vídeo Guidão, Print de Faturamento, Áudio no Grupo
    val hookText: String,
    val scriptOrCopy: String,
    val callToAction: String,
    val bestTimeToPost: String,
    val hashtags: List<String>
)

/**
 * Gerenciador de campanhas e inteligência de tráfego pago e orgânico do Jarvis.
 */
object TrafficGrowthEngineManager {

    private const val PREFS_NAME = "jarvis_traffic_growth_prefs"
    private const val KEY_CHECKOUT_BASE_URL = "key_checkout_base_url"

    var checkoutBaseUrl: String = "https://www.asaas.com/c/jarvis-pro-oficial"
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        checkoutBaseUrl = prefs.getString(KEY_CHECKOUT_BASE_URL, checkoutBaseUrl) ?: checkoutBaseUrl
    }

    fun updateCheckoutUrl(context: Context, newUrl: String) {
        checkoutBaseUrl = newUrl
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CHECKOUT_BASE_URL, newUrl).apply()
    }

    /**
     * Retorna campanhas validadas para Meta Ads (Instagram/Facebook) e TikTok Ads.
     */
    fun getPaidTrafficCampaigns(): List<PaidTrafficCampaign> {
        return listOf(
            PaidTrafficCampaign(
                id = "meta_gasolina_30",
                platform = "Meta Ads (Instagram / Facebook)",
                title = "Campanha: R$ 30 a Menos na Bomba Todo Dia",
                headline = "Economize até R$ 900/mês só recusando corrida lixo no iFood",
                primaryText = "Você passa 10h em cima da moto e quando vai abastecer o lucro sumiu? O Jarvis é o copiloto neural que calcula o R$/KM na hora e rejeita corrida de R$ 6 que dá prejuízo. Teste grátis!",
                visualHookDescription = "Vídeo gravado no guidão da moto em frente ao posto de gasolina com a bomba marcando R$ 80,00. Corta para a tela do Jarvis mostrando 'R$ 45 economizados hoje'.",
                callToAction = "Baixar Grátis no WhatsApp",
                targetAudience = "Homens 18-45 anos, Interesses: iFood para Entregadores, Uber Moto, 99Food, Motociclismo, Honda CG 160",
                suggestedDailyBudget = "R$ 15,00 a R$ 30,00 / dia",
                estimatedCpaBrl = 4.20,
                utmLink = "$checkoutBaseUrl?utm_source=meta_ads&utm_medium=video_posto&utm_campaign=gasolina_30"
            ),
            PaidTrafficCampaign(
                id = "tiktok_rejeicao_automatica",
                platform = "TikTok Ads (Spark Ads / In-Feed)",
                title = "Campanha: O Filtro Anti-Prejuízo dos Motoboys",
                headline = "Nunca mais aceite corrida abaixo de R$ 4,50/KM",
                primaryText = "O aplicativo toca a corrida, o Jarvis lê a tela em 0.2s e fala no seu fone Bluetooth: 'Corrida descartada, R$ 2,10/km'. Menos corrida rodada, mais dinheiro no bolso no fim da noite.",
                visualHookDescription = "Câmera POV no capacete. Toca a corrida de R$ 7,50 para 9km. O Jarvis recusa automaticamente com som de 'beep'. Logo em seguida cai uma de R$ 22,00 para 4km e aceita com luz verde.",
                callToAction = "Instalar Agora",
                targetAudience = "Usuários Android, Localização: Capitais (SP, RJ, BH, Curitiba, Salvador), Interesse em Delivery e Aplicativos de Renda Extra",
                suggestedDailyBudget = "R$ 20,00 / dia",
                estimatedCpaBrl = 3.80,
                utmLink = "$checkoutBaseUrl?utm_source=tiktok_ads&utm_medium=video_pov&utm_campaign=rejeicao_automatica"
            ),
            PaidTrafficCampaign(
                id = "meta_rota_dupla_multiplataforma",
                platform = "Meta Ads (Carrossel & Reels)",
                title = "Campanha: Duas Corridas no Mesmo Trajeto (iFood + 99)",
                headline = "Dobre seu faturamento usando o Modo Rota Dupla",
                primaryText = "Enquanto um motoboy comum faz 1 corrida por vez, o Jarvis cruza chamados do iFood e da 99Moto na mesma direção e te avisa no HUD. R$ 38 em uma única rota sem gastar o dobro de pneu.",
                visualHookDescription = "Carrossel de 3 telas: 1) Notificação de corrida iFood (R$ 18); 2) Notificação 99 (R$ 20); 3) Painel do Jarvis consolidando R$ 38 em 6km.",
                callToAction = "Liberar Acesso Pro",
                targetAudience = "Entregadores multiapp que rodam com mais de uma bag na moto.",
                suggestedDailyBudget = "R$ 25,00 / dia",
                estimatedCpaBrl = 5.10,
                utmLink = "$checkoutBaseUrl?utm_source=meta_ads&utm_medium=carrossel&utm_campaign=rota_dupla"
            )
        )
    }

    /**
     * Retorna estratégias orgânicas de viralização para redes sociais e grupos.
     */
    fun getOrganicTrafficStrategies(): List<OrganicTrafficStrategy> {
        return listOf(
            OrganicTrafficStrategy(
                id = "org_video_guidao_real",
                channel = "TikTok & Instagram Reels",
                title = "Vídeo Orgânico: 'Mostrando quanto o iFood esconde'",
                format = "Vídeo Vertical (POV Guidão com moto acelerando)",
                hookText = "Se você é motoboy e não sabe quanto tá ganhando por KM, você tá pagando pra trabalhar e não sabe!",
                scriptOrCopy = "1. Comece gravando o guidão e a rua em movimento.\n2. Mostre o HUD flutuante do Jarvis aberto sobre o iFood.\n3. Quando tocar a corrida, mostre o cálculo instantâneo: 'Olha aí, R$ 1,90 o km... RECUSA!'.\n4. Mostre a próxima que paga R$ 6,20/km e fale: 'Essa sim paga a parcela da moto!'.\n5. Finalize mandando a galera comentar 'KM' pra receber o link no direct.",
                callToAction = "Comente 'KM' que te mando o link do APK no privado!",
                bestTimeToPost = "11:30 às 13:30 (Pico do almoço) ou 18:00 às 20:00 (Pico da janta)",
                hashtags = listOf("#motoboy", "#ifood", "#ubereats", "#entregador", "#motovlog", "#rendaextra")
            ),
            OrganicTrafficStrategy(
                id = "org_print_faturamento_grupos",
                channel = "Grupos de WhatsApp & Telegram",
                title = "Tática de Infiltração: Print de R$ 340 com Menos KM",
                format = "Foto / Print Real de Fechamento do Jarvis",
                hookText = "Rapaziada, fiz R$ 340 hoje rodando só 54 km! A gasolina rendeu demais...",
                scriptOrCopy = "Mande no grupo de entregadores um print da tela de resumo do Jarvis mostrando: R$ 340,00 de lucro, 54 km rodados (R$ 6,29/km) e 8 corridas lixo recusadas.\nQuando alguém perguntar que app é esse, responda com naturalidade: 'É o Jarvis, um copiloto que filtra as corridas ruins sozinho'. E solte seu link de indicação do Asaas.",
                callToAction = "Link no grupo: https://wa.me/?text=Baixa%20ai%20o%20Jarvis",
                bestTimeToPost = "22:30 às 00:00 (Quando os motoboys param no posto pós-expediente)",
                hashtags = listOf("#grupos", "#motoboys", "#fechamentododia")
            ),
            OrganicTrafficStrategy(
                id = "org_audio_polemico_grupos",
                channel = "Áudio Tático no WhatsApp",
                title = "Áudio Autêntico: 'Descobri o segredo das taxas'",
                format = "Áudio curto de 25 segundos",
                hookText = "Mano do céu, eu descobri por que tanta gente volta pra casa no vermelho...",
                scriptOrCopy = "Grave com barulho ambiente de rua: 'Rapaziada do grupo, quem aqui roda no escuro tá perdendo dinheiro feio. Instalei um aplicativo chamado Jarvis que calcula a taxa por KM na hora e recusa corrida abaixo da margem. Minha gasolina tá durando 2 dias a mais. Baixa aí quem quiser testar'.",
                callToAction = "Mandar no grupo de WhatsApp com o link do APK logo abaixo.",
                bestTimeToPost = "07:30 da manhã (Início da escala de entrega)",
                hashtags = listOf("#audio", "#whatsapp", "#motoboysp")
            )
        )
    }
}
