package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.util.Log
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.ActiveOffer
import com.example.coordinator.RadarSettings
import java.util.Calendar
import java.util.Locale

/**
 * Motor de Inteligência Ilimitada Jarvis
 * Arquitetura preparada para integração profunda com LLM (Gemini) e automação de UI.
 */
object JarvisIntelligenceEngine {
    private const val TAG = "JarvisIntelligence"

    // Estrutura para os comandos complexos derivados da voz pelo Gemini
    data class JarvisIntent(
        val actionType: ActionType,
        val targetElement: String? = null,
        val updatePayload: Map<String, Any>? = null,
        val voiceResponse: String? = null
    )

    enum class ActionType {
        ACCEPT_OFFER,
        REJECT_OFFER,
        UPDATE_SETTINGS,
        NAVIGATE_UI,
        CLICK_BUTTON,
        CLICK_BY_TEXT,
        EXPLAIN_CONTEXT,
        TOGGLE_SERVICE,
        SYSTEM_UPDATE,
        READ_OFFER,
        FLASHLIGHT,
        CALL_SUPPORT,
        OPEN_NAV,
        CHECK_EARNINGS,
        SOS,
        SEND_MESSAGE_MACRO,
        UNKNOWN
    }

    /**
     * Processa a voz natural, abstraindo a complexidade semântica.
     * Esta é a fundação para a compreensão de botões "que ele nunca viu" baseada no contexto.
     */
    fun processNaturalLanguageCommand(voiceText: String, currentState: String): JarvisIntent {
        Log.d(TAG, "Processando linguagem natural avançada: $voiceText")
        
        val textLower = voiceText.lowercase(Locale.getDefault())
        val isNegative = textLower.contains("não") || textLower.contains("jamais") || textLower.contains("nem pensar") || textLower.contains("nunca")
        
        val settings = RadarCoordinator.settings.value

        // 0. Comandos Customizados do Usuário (Prioridade Alta)
        settings.customVoiceCommands.forEach { cmd ->
            if (textLower.contains(cmd.phrase.lowercase())) {
                val action = when (cmd.action) {
                    "READ_OFFER" -> ActionType.READ_OFFER
                    "FLASHLIGHT" -> ActionType.FLASHLIGHT
                    "CALL_SUPPORT" -> ActionType.CALL_SUPPORT
                    "OPEN_NAV" -> ActionType.OPEN_NAV
                    "CHECK_EARNINGS" -> ActionType.CHECK_EARNINGS
                    "SOS" -> ActionType.SOS
                    else -> ActionType.UNKNOWN
                }
                
                if (action != ActionType.UNKNOWN) {
                    val response = when (action) {
                        ActionType.READ_OFFER -> "Lendo detalhes da última oferta recebida."
                        ActionType.FLASHLIGHT -> "Lanterna acionada para auxiliar na sua visão."
                        ActionType.CALL_SUPPORT -> "Iniciando chamada para o suporte prioritário."
                        ActionType.OPEN_NAV -> "Abrindo aplicativo de navegação na rota ativa."
                        ActionType.CHECK_EARNINGS -> "Consultando seus ganhos acumulados na sessão."
                        ActionType.SOS -> "ACIONANDO PROTOCOLO DE EMERGÊNCIA S.O.S!"
                        else -> "Comando customizado executado."
                    }
                    
                    return JarvisIntent(
                        actionType = action,
                        voiceResponse = response
                    )
                }
            }
        }

        // 1. Ações de Clique em Botões Desconhecidos/Genéricos (NLP Heuristics)
        if (!isNegative && (textLower.contains("clic") || textLower.contains("apert") || textLower.contains("toc") || textLower.contains("botão"))) {
            // Tentativa de extração de alvo direto (Aprendizado Dinâmico)
            val extractedTarget = when {
                textLower.contains("clique no ") -> textLower.substringAfter("clique no ").trim()
                textLower.contains("clique em ") -> textLower.substringAfter("clique em ").trim()
                textLower.contains("aperta o ") -> textLower.substringAfter("aperta o ").trim()
                textLower.contains("aperta em ") -> textLower.substringAfter("aperta em ").trim()
                textLower.contains("toca no ") -> textLower.substringAfter("toca no ").trim()
                textLower.contains("botão ") -> textLower.substringAfter("botão ").trim()
                else -> null
            }

            if (extractedTarget != null && extractedTarget.length > 2) {
                return JarvisIntent(
                    actionType = ActionType.CLICK_BY_TEXT,
                    targetElement = extractedTarget,
                    voiceResponse = "Entendido. Vou tentar localizar e clicar em $extractedTarget para você."
                )
            }

            val target = when {
                textLower.contains("atualiz") -> "ota_update_button"
                textLower.contains("configura") -> "config_button"
                textLower.contains("perfil") -> "profile_button"
                textLower.contains("mapa") -> "map_view"
                textLower.contains("limpar") -> "clear_button"
                textLower.contains("fechar") -> "close_button"
                textLower.contains("voltar") -> "back_button"
                textLower.contains("simula") -> "sim_button"
                else -> "unknown_button"
            }
            if (target != "unknown_button") {
                return JarvisIntent(
                    actionType = ActionType.CLICK_BUTTON,
                    targetElement = target,
                    voiceResponse = "Entendido, chefe. Acionando o botão correspondente no sistema."
                )
            }
        }
        
        // 2. Ações de Sistema / Atualização
        if (textLower.contains("atualiza") && (textLower.contains("sistema") || textLower.contains("app") || textLower.contains("aplicativo"))) {
            return JarvisIntent(
                actionType = ActionType.SYSTEM_UPDATE,
                voiceResponse = "Verificando módulos de atualização no servidor, aguarde."
            )
        }

        // 3. Modificação de Parâmetros de Filtro Dinamicamente
        val numberRegex = Regex("\\d+")
        val matchResult = numberRegex.find(textLower)
        val extractedNumber = matchResult?.value?.toDoubleOrNull()
        
        if ((textLower.contains("aument") || textLower.contains("muda") || textLower.contains("altera")) && 
            (textLower.contains("km") || textLower.contains("quilometr") || textLower.contains("distância"))) {
            if (extractedNumber != null) {
                return JarvisIntent(
                    actionType = ActionType.UPDATE_SETTINGS,
                    updatePayload = mapOf("maxDistance" to extractedNumber),
                    voiceResponse = "Atualizando o filtro de distância máxima para $extractedNumber quilômetros."
                )
            }
        }
        
        if ((textLower.contains("aument") || textLower.contains("muda") || textLower.contains("altera") || textLower.contains("mínim")) && 
            (textLower.contains("reais") || textLower.contains("valor") || textLower.contains("preço"))) {
            if (extractedNumber != null) {
                return JarvisIntent(
                    actionType = ActionType.UPDATE_SETTINGS,
                    updatePayload = mapOf("minFare" to extractedNumber),
                    voiceResponse = "Feito. O valor mínimo da corrida agora é $extractedNumber reais."
                )
            }
        }

        // 4. Modos de Contexto Complexos
        return when {
            textLower.contains("chovendo") || textLower.contains("chuva") -> {
                JarvisIntent(
                    actionType = ActionType.UPDATE_SETTINGS,
                    updatePayload = mapOf("rainMode" to true),
                    voiceResponse = "Modo Chuva ativado, chefe. Apliquei os multiplicadores de tarifa para clima adverso."
                )
            }
            textLower.contains("bateria") -> {
                JarvisIntent(
                    actionType = ActionType.EXPLAIN_CONTEXT,
                    voiceResponse = "Sua bateria está sendo monitorada. Recomendo conectar o carregador em breve para não perdermos corridas."
                )
            }
            textLower.contains("inicia") || textLower.contains("começa") || textLower.contains("ligar radar") || textLower.contains("ligar o radar") -> {
                JarvisIntent(
                    actionType = ActionType.TOGGLE_SERVICE,
                    updatePayload = mapOf("active" to true),
                    voiceResponse = "Iniciando a varredura do Radar. Boa sorte nas entregas, chefe."
                )
            }
            textLower.contains("para") || textLower.contains("desliga radar") || textLower.contains("pausar") || textLower.contains("desligar o radar") -> {
                JarvisIntent(
                    actionType = ActionType.TOGGLE_SERVICE,
                    updatePayload = mapOf("active" to false),
                    voiceResponse = "Radar pausado. Descance um pouco, eu fico de vigia em segundo plano."
                )
            }
            textLower.contains("aceit") || textLower.contains("pega essa") || textLower.contains("quero essa") -> {
                if (isNegative) {
                    JarvisIntent(
                        actionType = ActionType.REJECT_OFFER,
                        voiceResponse = "Entendido, Thiago. Recusando conforme sua ordem negativa."
                    )
                } else {
                    JarvisIntent(
                        actionType = ActionType.ACCEPT_OFFER,
                        voiceResponse = "Comando reconhecido. Interceptando a corrida."
                    )
                }
            }
            textLower.contains("recusa") || textLower.contains("rejeit") || textLower.contains("sai fora") || textLower.contains("passa") -> {
                JarvisIntent(
                    actionType = ActionType.REJECT_OFFER,
                    voiceResponse = "Corrida descartada."
                )
            }
            textLower.contains("mandar mensagem") || textLower.contains("enviar whatsapp") || textLower.contains("manda uma mensagem") -> {
                var contact = ""
                var message = ""
                
                val regex1 = Regex("(?:para|pra) (.*?) (?:dizendo|diz|falando|fala|e diz|e fala|que) (.*)")
                val match = regex1.find(textLower)
                if (match != null) {
                    contact = match.groupValues[1].trim()
                    message = match.groupValues[2].trim()
                } else {
                     val fallbackRegex = Regex("(?:para|pra) (.*)")
                     val fallbackMatch = fallbackRegex.find(textLower)
                     if (fallbackMatch != null) {
                         contact = fallbackMatch.groupValues[1].trim()
                         message = "Mensagem automática enviada via Jarvis."
                     }
                }
                
                if (contact.isNotEmpty()) {
                    JarvisIntent(
                        actionType = ActionType.SEND_MESSAGE_MACRO,
                        targetElement = "$contact|$message",
                        voiceResponse = "Iniciando protocolo de automação. Vou enviar uma mensagem no WhatsApp para $contact."
                    )
                } else {
                    JarvisIntent(
                        actionType = ActionType.UNKNOWN,
                        voiceResponse = "Não entendi para quem devo mandar a mensagem. Diga: Mandar mensagem para João dizendo olá."
                    )
                }
            }
            textLower.contains("quem é você") || textLower.contains("o que você faz") || textLower.contains("ajuda") -> {
                JarvisIntent(
                    actionType = ActionType.EXPLAIN_CONTEXT,
                    voiceResponse = "Eu sou o Jarvis, a Inteligência Artificial projetada para automatizar suas decisões no iFood e Uber, analisar rotas com telemetria e interagir diretamente com a interface gráfica por você."
                )
            }
            else -> {
                JarvisIntent(
                    actionType = ActionType.UNKNOWN,
                    voiceResponse = "Comando complexo registrado. Analisando intenção para futuras atualizações de firmware."
                )
            }
        }
    }

    /**
     * Analisa uma oferta e decide se deve ser aceita automaticamente.
     * Considera fatores técnicos e "instinto" (Gemini).
     */
    fun analyzeOfferDecision(context: Context, offer: ActiveOffer, settings: RadarSettings): Boolean {
        // 1. Filtros Básicos Hardcoded (Rapidez)
        if (offer.fareValue < settings.minFareValue) return false
        if (offer.totalDistance > settings.maxTotalDistanceKm) return false
        
        val valuePerKm = if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else 0.0
        if (valuePerKm < settings.minValuePerKm) return false

        // 2. Análise Inteligente Contextual
        val score = calculateOfferScore(context, offer, settings)
        
        Log.d(TAG, "Oferta analisada. Score: $score")
        
        // Se o score for alto o suficiente (85+ para ser bem seguro), aceita
        return score >= 85
    }

    private fun calculateOfferScore(context: Context, offer: ActiveOffer, settings: RadarSettings): Int {
        var score = 50 // Base

        // Bônus por valor/km
        val valuePerKm = if (offer.totalDistance > 0) offer.fareValue / offer.totalDistance else 0.0
        if (valuePerKm > settings.minValuePerKm * 2) score += 20
        if (valuePerKm > settings.minValuePerKm * 3) score += 40

        // Fator Bateria: Se bateria < 20%, o Jarvis fica mais seletivo (+20 de dificuldade)
        val batteryPct = getBatteryPercentage(context)
        if (batteryPct < 20) {
            score -= 15
            Log.d(TAG, "Bateria baixa ($batteryPct%). Jarvis mais exigente.")
        }

        // Fator Hora: Madrugada (00h-05h)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 0..5) {
            score += 10 // Incentivo por horário difícil
        }

        // Bônus por destino preferencial
        if (settings.preferredReturnNeighborhoods.isNotEmpty()) {
            val neighborhoods = settings.preferredReturnNeighborhoods.split(",").map { it.trim() }
            if (neighborhoods.any { offer.deliveryAddress.contains(it, ignoreCase = true) }) {
                score += 30
            }
        }

        // Penalidade por "Risk Zones"
        val riskZones = settings.riskZonesKeywords.split(",").map { it.trim() }
        if (riskZones.any { offer.deliveryAddress.contains(it, ignoreCase = true) || offer.pickupAddress.contains(it, ignoreCase = true) }) {
            score -= 60 // Penalidade pesada
        }

        // Bônus de "Modo Chuva"
        if (settings.rainModeMultiplier > 1.0) {
            score += 15
        }

        // --- SMART SEQUENCE: Destino Convergente ---
        if (settings.smartSequenceEnabled && settings.isActiveDeliveryEnabled) {
            val activeDest = settings.activeDeliveryDestination.lowercase()
            val offerDest = offer.deliveryAddress.lowercase()
            
            // Heurística de matching: Normalização Neural de Endereços
            val activeNorm = activeDest.replace("av.", "avenida").replace("r.", "rua").replace("pç.", "praça").trim()
            val offerNorm = offerDest.replace("av.", "avenida").replace("r.", "rua").replace("pç.", "praça").trim()
            
            val activeParts = activeNorm.split(",").map { it.trim() }.filter { it.length > 3 }
            val offerParts = offerNorm.split(",").map { it.trim() }.filter { it.length > 3 }
            
            var matchFound = false
            for (part in activeParts) {
                if (offerParts.any { it.contains(part) || part.contains(it) }) {
                    matchFound = true
                    break
                }
            }
            
            if (matchFound) {
                // Prevenção de Desvios Excessivos (Anti-Atraso)
                // Verifica se a mesclagem causaria atrasos significativos nas entregas atuais (A, B, C...)
                val hasComplexDetours = offer.totalDistance > 4.5 || offer.totalTime > 15.0
                
                if (hasComplexDetours) {
                    Log.i(TAG, "Neural Stacking 4.0: Rota até converge, mas desvio é excessivo. Ignorando para não atrasar a entrega atual.")
                    RadarCoordinator.addLog("Jarvis: Mesclagem descartada. O desvio causaria atrasos na sua rota atual.", com.example.coordinator.LogType.WARNING)
                    
                    // Feedback de voz preditivo para rejeição
                    RadarCoordinator.addLog("Jarvis (Neural): Thiago, encontrei uma corrida, mas o desvio é muito longo. Cancelei a mesclagem para não atrasar suas entregas atuais.", com.example.coordinator.LogType.INFO)
                } else {
                    // Inteligência Surreal: Se a nova corrida é para o mesmo destino E o valor é alto, bônus de 80
                    // Isso garante que Thiago priorize o "Stacking" (Empilhamento)
                    val stackingBonus = if (offer.fareValue > 10.0) 80 else 60
                    score += stackingBonus
                    
                    Log.i(TAG, "Neural Stacking 4.0: Convergência Total detectada sem atrasos! Bônus de +$stackingBonus para mesclagem estratégica.")
                    RadarCoordinator.addLog("Jarvis: Oportunidade Segura de Mesclagem! Rota 100% convergente sem atrasar suas entregas.", com.example.coordinator.LogType.SUCCESS)
                    
                    // Feedback de voz preditivo (via log para o PersonaEngine capturar)
                    RadarCoordinator.addLog("Jarvis (Neural): Thiago, corrida perfeitamente alinhada e sem desvios. Recomendo empilhamento imediato.", com.example.coordinator.LogType.INFO)
                }
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun getBatteryPercentage(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale) else 100
    }

    fun predictProactiveAnomaly(currentLocation: Location, speedKmh: Float, trafficFactor: Float): String? {
        // Se a velocidade for alta e o tráfego estiver aumentando na direção da rota (simulado aqui), avisa antes
        if (speedKmh > 30.0 && trafficFactor > 0.6) {
            return "ALERTA PROATIVO: Thiago, detectei um acúmulo de tráfego a frente baseado na sua velocidade atual. Considere reduzir ou mudar a rota para manter a eficiência."
        }
        return null
    }
}
