package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DeepOrderPoolCockpitCard
 *
 * Painel no Cockpit para visualização e execução de mesclagens profundas
 * extraídas diretamente das abas de "Corridas Disponíveis / Sob Demanda"
 * dos aplicativos parceiros (iFood, 99Food, Rappi).
 */
@Composable
fun DeepOrderPoolCockpitCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val poolEngine = remember { DeepOrderPoolMergingEngine.getInstance(context) }

    val poolOffers by poolEngine.scannedPoolOffers.collectAsState()
    val rankedCombos by poolEngine.rankedCombos.collectAsState()
    val lastScan by poolEngine.lastScanTimestamp.collectAsState()
    val appsCount by poolEngine.scannedAppsCount.collectAsState()

    var expandedComboId by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1329)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFEC4899))
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("card_deep_order_pool")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABEÇALHO DO AGREGADOR DE POOL
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MultipleStop,
                            contentDescription = "Mesclador de Pool",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MESCLADOR PROFUNDO DE CORRIDAS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (rankedCombos.isNotEmpty()) Color(0xFF059669) else Color(0xFF475569)
                            ) {
                                Text(
                                    text = if (rankedCombos.isNotEmpty()) "${rankedCombos.size} COMBOS" else "MONITORANDO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Varredura contínua de listas e feeds abertos de pedidos",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BARRA DE TELEMETRIA DA VARREDURA
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POOLS MONITORADOS",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$appsCount Apps • ${poolOffers.size} Corridas Ativas",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ÚLTIMA VARREDURA",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lastScan > 0) {
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastScan))
                            } else "Aguardando feed...",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // LISTA DE COMBOS RANKED OU ESTADO VAZIO COM BOTÃO DE SIMULAÇÃO
            if (rankedCombos.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abra a aba 'Disponíveis' do iFood, 99Food ou Rappi",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "O Jarvis lê os cards em segundo plano via Acessibilidade e calcula os combos de rota mais lucrativos.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Botão de simulação rápida para demonstração imediata
                        OutlinedButton(
                            onClick = {
                                simulateDemoPoolOffers(poolEngine)
                                Toast.makeText(context, "⚡ 5 Corridas simuladas no pool! Combos gerados.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simular Varredura de Corridas Disponíveis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = "MELHORES COMBOS ENCONTRADOS (LUCRO LÍQUIDO MÁXIMO):",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rankedCombos.forEachIndexed { index, combo ->
                        ComboItemView(
                            combo = combo,
                            position = index + 1,
                            isExpanded = expandedComboId == combo.id,
                            onToggleExpand = {
                                expandedComboId = if (expandedComboId == combo.id) null else combo.id
                            },
                            onAcceptCombo = {
                                executeComboQuickSwitch(context, combo)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Valores descontam custo de combustível",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )

                    OutlinedButton(
                        onClick = {
                            poolEngine.clearPool()
                            Toast.makeText(context, "Lista de pool resetada.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpar Pool", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComboItemView(
    combo: MergedDeliveryCombo,
    position: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAcceptCombo: () -> Unit
) {
    val borderColor = if (combo.isDiamondDeal) Color(0xFFEC4899) else Color(0xFF38BDF8).copy(alpha = 0.6f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF131C35),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // CABEÇALHO DO COMBO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (combo.isDiamondDeal) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFBE185D)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DIAMANTE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7)
                        ) {
                            Text(
                                text = "COMBO #$position",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${combo.primaryOffer.appName} + ${combo.secondaryOffer.appName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R$ ${String.format(Locale.US, "%.2f", combo.totalGrossRevenue)}",
                        color = Color(0xFF10B981),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Líquido: R$ ${String.format(Locale.US, "%.2f", combo.netProfit)}",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            Spacer(modifier = Modifier.height(6.dp))

            // IDENTIFICAÇÃO DO CORREDOR GEOGRÁFICO (VETOR DE DIREÇÃO & DESVIO)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = if (combo.corridorAlignmentDeg <= 30f) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = combo.corridorLabel,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Coleta +${combo.pickupDeviationMeters}m / Entrega +${combo.dropoffDeviationMeters}m",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MÉTRICAS-CHAVE: DESVIO, SLA E RENDIMENTO POR HORA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${combo.combinedDistanceKm} km (+${combo.extraDeviationMeters}m desvio)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "R$ ${String.format(Locale.US, "%.2f", combo.netProfitPerHour)}/h",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SLA ${combo.slaSafetyScore}%",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // EXPANSÃO DETALHADA COM PASSOS DA ROTA
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(8.dp))

                    combo.haversineResult?.let { hResult ->
                        HaversineDeviationBadge(result = hResult)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = combo.recommendationReason,
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ROTA DE EXECUÇÃO OTIMIZADA:",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    combo.executionSteps.forEach { step ->
                        Text(
                            text = step,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onAcceptCombo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (combo.isDiamondDeal) Color(0xFFBE185D) else Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aceitar Combo e Alternar (${combo.primaryOffer.appName} + ${combo.secondaryOffer.appName})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BOTÃO DE TOQUE PARA EXPANDIR / RECOLHER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dispara a transição e aceite assistido do combo em tela dividida (50% superior / 50% inferior).
 */
private fun executeComboQuickSwitch(context: Context, combo: MergedDeliveryCombo) {
    try {
        HapticFeedbackHelper.vibrateSuccess(context)
        val msg = "Iniciando combo de ${combo.primaryOffer.appName} no topo e ${combo.secondaryOffer.appName} na base. Ganho de R$ ${String.format(Locale.US, "%.2f", combo.totalGrossRevenue)}."
        TextToSpeechManager.getInstance(context).speakVoiceAlert(msg)
        Toast.makeText(context, "📱 Dividindo tela: ${combo.primaryOffer.appName} + ${combo.secondaryOffer.appName}", Toast.LENGTH_SHORT).show()

        // Lança ambos os aplicativos em tela dividida através do coordenador multi-janela
        val coordinator = SplitScreenDualRadarCoordinator.getInstance(context)
        val pair = SplitScreenAppPair(
            id = "combo_pair_${combo.id.take(8)}",
            title = "${combo.primaryOffer.appName} + ${combo.secondaryOffer.appName}",
            topPackage = combo.primaryOffer.packageName,
            topAppName = combo.primaryOffer.appName,
            bottomPackage = combo.secondaryOffer.packageName,
            bottomAppName = combo.secondaryOffer.appName,
            description = "Combo no mesmo corredor viário (+${combo.extraDeviationMeters}m)"
        )
        coordinator.launchSplitScreenPair(context, pair)
    } catch (e: Exception) {
        Toast.makeText(context, "Falha ao acionar tela dividida: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Simula ofertas realistas para teste do motoboy fora do horário de pico.
 */
private fun simulateDemoPoolOffers(engine: DeepOrderPoolMergingEngine) {
    val ifoodOffers = listOf(
        ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Madero Container - Paulista",
            pickupAddress = "Av. Paulista, 1200",
            dropoffAddress = "Rua Augusta, 1800 - Consolação",
            value = 19.50,
            distanceKm = 3.2,
            pickupLat = -23.5615,
            pickupLng = -46.6558,
            dropoffLat = -23.5532,
            dropoffLng = -46.6612
        ),
        ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Pão de Açúcar Express",
            pickupAddress = "Alameda Santos, 980",
            dropoffAddress = "Rua Haddock Lobo, 1300 - Jardins",
            value = 16.00,
            distanceKm = 2.8,
            pickupLat = -23.5650,
            pickupLng = -46.6520,
            dropoffLat = -23.5590,
            dropoffLng = -46.6650
        ),
        ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Habib's Brigadeiro",
            pickupAddress = "Av. Brig. Luís Antônio, 2200",
            dropoffAddress = "Rua Tutóia, 400 - Paraíso",
            value = 14.50,
            distanceKm = 2.4,
            pickupLat = -23.5710,
            pickupLng = -46.6480,
            dropoffLat = -23.5760,
            dropoffLng = -46.6510
        )
    )

    val noventaOffers = listOf(
        ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Outback Steakhouse - Pátio Paulista",
            pickupAddress = "Rua Treze de Maio, 1947",
            dropoffAddress = "Rua Bela Cintra, 1100 - Consolação",
            value = 21.00,
            distanceKm = 3.6,
            pickupLat = -23.5618,
            pickupLng = -46.6552,
            dropoffLat = -23.5540,
            dropoffLng = -46.6620
        ),
        ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Drogaria São Paulo",
            pickupAddress = "Av. Paulista, 800",
            dropoffAddress = "Alameda Lorena, 1400 - Jardins",
            value = 17.50,
            distanceKm = 2.5,
            pickupLat = -23.5640,
            pickupLng = -46.6530,
            dropoffLat = -23.5600,
            dropoffLng = -46.6640
        )
    )

    engine.ingestPoolOffers("iFood", ifoodOffers)
    engine.ingestPoolOffers("99Food", noventaOffers)
}
