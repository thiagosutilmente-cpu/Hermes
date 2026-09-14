package com.example

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * GERENCIADOR DE PERSISTÊNCIA DO ONBOARDING
 * Armazena no SharedPreferences local se o entregador já concluiu o tour guiado.
 */
object OnboardingPreferencesManager {
    private const val PREFS_NAME = "radar_onboarding_prefs"
    private const val KEY_COMPLETED = "has_completed_onboarding_tour"

    fun hasCompletedOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    }

    fun resetOnboarding(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_COMPLETED).apply()
    }
}

/**
 * TELA DO TOUR GUIADO (ONBOARDING)
 *
 * Apresentada na primeira inicialização do app (ou sob demanda através do botão de ajuda).
 * Explica detalhadamente:
 * 1. O Cockpit Neural & Stacking Multi-App
 * 2. Como configurar os Filtros de Ofertas (Raio máximo, Valor mínimo e Multiplicador R$/hora)
 * 3. Como utilizar os Comandos de Voz Mãos-Livres no capacete/fone
 * 4. Conclusão e checklist de prontidão
 */
@Composable
fun GuidedOnboardingTour(
    onFinishTour: () -> Unit,
    onOpenFilterSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Transição infinita para pulsação de destaque
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_guided_onboarding"),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // BARRA SUPERIOR DO TOUR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador textual de passo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(DarkCardElevated, CircleShape)
                            .border(1.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${currentStep + 1}",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tour de Operação (${currentStep + 1}/$totalSteps)",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Botão de pular tour
                Text(
                    text = "Pular Tour ➔",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .testTag("btn_skip_onboarding")
                        .clickable {
                            OnboardingPreferencesManager.setOnboardingCompleted(context, true)
                            onFinishTour()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // BARRA DE PROGRESSO COM TRAÇOS COLORIDOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalSteps) { index ->
                    val isCurrentOrPassed = index <= currentStep
                    val barColor by animateColorAsState(
                        targetValue = if (isCurrentOrPassed) NeonGreen else DarkBorder,
                        label = "barColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            // CONTEÚDO DINÂMICO DO PASSO ATUAL (COM ANIMAÇÃO DE SLIDE)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "onboardingSlide"
                ) { step ->
                    when (step) {
                        0 -> OnboardingIntroSlide(pulseScale = pulseScale)
                        1 -> OnboardingFiltersSlide(onOpenFilterSettings = onOpenFilterSettings)
                        2 -> OnboardingVoiceCommandsSlide()
                        3 -> OnboardingReadySlide(
                            onStart = {
                                OnboardingPreferencesManager.setOnboardingCompleted(context, true)
                                onFinishTour()
                            },
                            onOpenFilterSettings = onOpenFilterSettings
                        )
                    }
                }
            }

            // BARRA DE NAVEGAÇÃO INFERIOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_onboarding_back"),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextLight
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Voltar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_onboarding_next"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = DarkBg
                        )
                    ) {
                        Text(
                            text = "Próximo Passo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Avançar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            OnboardingPreferencesManager.setOnboardingCompleted(context, true)
                            onFinishTour()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_onboarding_finish"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = DarkBg
                        )
                    ) {
                        Text(
                            text = "🚀 Começar a Rodar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * PASSO 1: INTRODUÇÃO AO COCKPIT NEURAL & STACKING
 */
@Composable
private fun OnboardingIntroSlide(pulseScale: Float) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Icon com efeito pulsante
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .border(2.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎯", fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Radar Coordinator",
            color = TextLight,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Jarvis Neural Cockpit para Entregadores",
            color = NeonGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "O aplicativo definitivo para quem roda de moto ou bike em São Paulo e capitais. O Jarvis monitora chamadas de múltiplos apps ao mesmo tempo e filtra apenas as corridas que dão lucro real.",
            color = TextLight.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Card 1: Multi-App Stacking
        OnboardingFeatureCard(
            emoji = "📦",
            title = "Multi-App Stacking",
            subtitle = "iFood, Rappi, Uber e 99 sincronizados",
            description = "Agrupa pedidos com trajetos e bairros compatíveis, gerando rotas combinadas que aumentam seu faturamento por hora em até 70%."
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card 2: Inteligência de Combustível
        OnboardingFeatureCard(
            emoji = "⛽",
            title = "Cálculo de Lucro Líquido Real",
            subtitle = "Desconta o custo de gasolina e tempo",
            description = "O algoritmo calcula quanto sobra no seu bolso por quilômetro rodado antes mesmo de você tocar na tela."
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * PASSO 2: COMO CONFIGURAR OS FILTROS DE OFERTAS
 */
@Composable
private fun OnboardingFiltersSlide(onOpenFilterSettings: (() -> Unit)?) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(text = "⚙️", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Filtros de Ofertas",
                    color = TextLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Como calibrar o que toca no seu celular",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "O segredo para não rodar no prejuízo é configurar seus limites. Ofertas que não batem seus critérios são recusadas automaticamente pelo Jarvis.",
            color = TextLight.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pilar 1: Raio Máximo
        OnboardingPillarCard(
            badge = "1. Raio de Distância Máximo (km)",
            badgeColor = Color(0xFF00D2FF),
            title = "Evite viagens longas demais",
            detail = "Defina até quantos quilômetros você aceita se deslocar (ex: 4 a 6 km). Isso impede que pedidos em bairros distantes te tirem da sua zona lucrativa.",
            exampleText = "Exemplo recomendado: 5.0 km para centros urbanos densos."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pilar 2: Valor Mínimo por Corrida
        OnboardingPillarCard(
            badge = "2. Valor Mínimo por Corrida (R$)",
            badgeColor = NeonGreen,
            title = "Elimine corridas baratas",
            detail = "Estabeleça um piso mínimo de faturamento (ex: R$ 15,00 a R$ 20,00). O Radar descarta corridas de R$ 6 a R$ 8 que não cobrem o custo de saída.",
            exampleText = "Exemplo recomendado: Mínimo de R$ 14,00 a R$ 18,00."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pilar 3: Multiplicador de Ganho por Hora e R$/km
        OnboardingPillarCard(
            badge = "3. Multiplicador de Ganho / Hora",
            badgeColor = Color(0xFFFFD700),
            title = "Meta de rentabilidade horária",
            detail = "Ajuste o multiplicador (1.0x a 2.5x) para mirar entre R$ 35/h e R$ 85/h e garanta ganho mínimo por km (ex: R$ 5,00/km) para proteger sua margem.",
            exampleText = "Exemplo recomendado: Multiplicador 1.3x (~R$ 45/hora) e R$ 5,00/km."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Pilar 4: Presets Táticos Rápidos
        OnboardingPillarCard(
            badge = "4. Presets Operacionais",
            badgeColor = Color(0xFFFF8800),
            title = "Estratégias a um clique",
            detail = "Use os modos rápidos na tela de filtros: Modo Chuva (bônus alto, tiros curtos), Lucro Máximo (apenas R$ 7+/km) ou Noturno (rotas expressas).",
            exampleText = "Alterne rapidamente a qualquer hora pelo botão de filtros."
        )

        if (onOpenFilterSettings != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenFilterSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCardElevated,
                    contentColor = NeonGreen
                ),
                border = BorderStroke(1.dp, NeonGreen)
            ) {
                Text(
                    text = "⚙️ Abrir Configuração de Filtros Agora",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * PASSO 3: COMO UTILIZAR OS COMANDOS DE VOZ NO CAPACETE
 */
@Composable
private fun OnboardingVoiceCommandsSlide() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(text = "🎙️", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Comandos de Voz Mãos-Livres",
                    color = TextLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Pilotagem 100% segura com comunicador/fone",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "Nunca tire as mãos do guidão nem desvie o olhar no trânsito. O Radar possui reconhecimento de voz calibrado para português e sintetização neural no seu fone Bluetooth.",
            color = TextLight.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card de Comandos Principais
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "VOCÊ FALA NO MICROFONE:",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                VoiceCommandRow(
                    command = "\"Aceitar\" ou \"Sim\"",
                    action = "Aceita a melhor oferta imediatamente",
                    tagColor = NeonGreen
                )
                VoiceCommandRow(
                    command = "\"Recusar\" ou \"Não\"",
                    action = "Descarta a oferta e silencia o alerta",
                    tagColor = Color(0xFFFF4757)
                )
                VoiceCommandRow(
                    command = "\"Ouvir Oferta\" ou \"Detalhes\"",
                    action = "A IA lê restaurante, valor e km no fone",
                    tagColor = Color(0xFF00D2FF)
                )
                VoiceCommandRow(
                    command = "\"Modo Foco\"",
                    action = "Ativa o painel HUD de alta legibilidade",
                    tagColor = Color(0xFFFFD700)
                )
                VoiceCommandRow(
                    command = "\"Modo Chuva\"",
                    action = "Aplica preset de chuva com bônus e raio curto",
                    tagColor = Color(0xFF00E5FF)
                )
                VoiceCommandRow(
                    command = "\"Navegar\"",
                    action = "Abre rota direto no Google Maps ou Waze",
                    tagColor = Color(0xFFB388FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dica de Fone Bluetooth
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💡", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Dica de Pilotagem com Comunicador",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "O app ignora ruídos de vento e escapamento. Fale em tom natural próximo ao microfone do capacete.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * PASSO 4: PRONTO PARA FATURAR & CHECKLIST OPERACIONAL
 */
@Composable
private fun OnboardingReadySlide(
    onStart: () -> Unit,
    onOpenFilterSettings: (() -> Unit)?
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(NeonGreen.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🚀", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tudo Pronto para Rodar!",
            color = TextLight,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Checklist de ativação do seu cockpit",
            color = NeonGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Checklist de Prontidão
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ChecklistItem(
                    title = "Filtros de Rentabilidade Ativos",
                    detail = "Raio de distância e valor mínimo configurados"
                )
                ChecklistItem(
                    title = "Comandos de Voz Habilitados",
                    detail = "Fale 'Aceitar' ou 'Recusar' no capacete"
                )
                ChecklistItem(
                    title = "Multi-App Stacking Conectado",
                    detail = "iFood, Rappi, Uber e 99 prontos para sincronizar"
                )
                ChecklistItem(
                    title = "Cálculo de Gasolina Automático",
                    detail = "Desconto em tempo real na tela de cada pedido"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botão Primário de Início
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_onboarding_start_driving"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = DarkBg
            )
        ) {
            Text(
                text = "ENTRAR NO COCKPIT AGORA ➔",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        if (onOpenFilterSettings != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenFilterSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextLight
                )
            ) {
                Text(
                    text = "⚙️ Personalizar Meus Filtros Antes de Rodar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// COMPONENTES AUXILIARES DO ONBOARDING
// -----------------------------------------------------------------------------

@Composable
private fun OnboardingFeatureCard(
    emoji: String,
    title: String,
    subtitle: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingPillarCard(
    badge: String,
    badgeColor: Color,
    title: String,
    detail: String,
    exampleText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = detail,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = exampleText,
                color = TextLight.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VoiceCommandRow(
    command: String,
    action: String,
    tagColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .border(1.dp, tagColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = command,
                color = tagColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = action,
            color = TextLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(NeonGreen.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = detail,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
