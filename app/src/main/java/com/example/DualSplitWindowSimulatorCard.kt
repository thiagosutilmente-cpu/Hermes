package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Cenário de teste para a simulação de tela dividida e mesclagem combinatória.
 */
data class SplitScenario(
    val id: Int,
    val title: String,
    val subtitle: String,
    val topAppName: String,
    val topPackage: String,
    val topRestaurant: String,
    val topPickup: String,
    val topDropoff: String,
    val topValue: Double,
    val topDistanceKm: Double,
    val bottomAppName: String,
    val bottomPackage: String,
    val bottomRestaurant: String,
    val bottomPickup: String,
    val bottomDropoff: String,
    val bottomValue: Double,
    val bottomDistanceKm: Double,
    val totalRevenue: Double,
    val netProfit: Double,
    val haversineMeters: Int,
    val extraMinutes: Double,
    val alignmentDeg: Float,
    val isApproved: Boolean,
    val badgeLabel: String,
    val badgeColor: Color,
    val reason: String
)

/**
 * DualSplitWindowSimulatorCard
 *
 * Componente visual interativo que simula a tela do celular do motoboy repartida
 * verticalmente em 50% superior (App 1) e 50% inferior (App 2), com HUD flutuante
 * central de ganho combinado, cálculo geodésico Haversine e botão de divisão real no Android.
 */
@Composable
fun DualSplitWindowSimulatorCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val scenarios = remember {
        listOf(
            SplitScenario(
                id = 1,
                title = "💎 Cenário Diamante (Mesmo Corredor)",
                subtitle = "Av. Paulista -> Consolação (iFood + 99Food)",
                topAppName = "iFood Entregas",
                topPackage = "com.ifood.entregador",
                topRestaurant = "Madero Container Paulista",
                topPickup = "Av. Paulista, 1200",
                topDropoff = "R. Augusta, 1800",
                topValue = 19.50,
                topDistanceKm = 3.2,
                bottomAppName = "99Food Driver",
                bottomPackage = "com.didiglobal.driver",
                bottomRestaurant = "Outback Pátio Paulista",
                bottomPickup = "R. Treze de Maio, 1947",
                bottomDropoff = "R. Bela Cintra, 1100",
                bottomValue = 21.00,
                bottomDistanceKm = 3.6,
                totalRevenue = 40.50,
                netProfit = 38.20,
                haversineMeters = 280,
                extraMinutes = 1.4,
                alignmentDeg = 12f,
                isApproved = true,
                badgeLabel = "🟢 DESVIO MÍNIMO (+280m)",
                badgeColor = Color(0xFF00FF88),
                reason = "As duas coletas distam 450m e entregas distam 280m no mesmo sentido viário. Lucro líquido: R$ 38,20 em 22 min."
            ),
            SplitScenario(
                id = 2,
                title = "🟡 Cenário Transversal (Ruas Paralelas)",
                subtitle = "Jardins -> Itaim Bibi (iFood + Rappi)",
                topAppName = "iFood Entregas",
                topPackage = "com.ifood.entregador",
                topRestaurant = "Pão de Açúcar Express",
                topPickup = "Al. Santos, 980",
                topDropoff = "R. Haddock Lobo, 1300",
                topValue = 16.00,
                topDistanceKm = 2.8,
                bottomAppName = "Rappi Entregador",
                bottomPackage = "com.grability.rappi.entregador",
                bottomRestaurant = "Bullguer Jardins",
                bottomPickup = "R. Bela Cintra, 1900",
                bottomDropoff = "Al. Lorena, 1500",
                bottomValue = 17.50,
                bottomDistanceKm = 3.1,
                totalRevenue = 33.50,
                netProfit = 31.10,
                haversineMeters = 650,
                extraMinutes = 3.2,
                alignmentDeg = 28f,
                isApproved = true,
                badgeLabel = "🟡 DESVIO MODERADO (+650m)",
                badgeColor = Color(0xFFFFB800),
                reason = "Ruas paralelas na mesma região gastronômica. Desvio aceitável de 3 min extras sem violar o SLA de comida quente."
            ),
            SplitScenario(
                id = 3,
                title = "🛑 Cenário de Bloqueio (Contra-fluxo)",
                subtitle = "Santana (Zona Norte) vs Moema (Zona Sul)",
                topAppName = "iFood Entregas",
                topPackage = "com.ifood.entregador",
                topRestaurant = "Habib's Santana",
                topPickup = "Av. Cruzeiro do Sul, 2000",
                topDropoff = "R. Voluntários da Pátria, 1500",
                topValue = 18.00,
                topDistanceKm = 4.0,
                bottomAppName = "99Food Driver",
                bottomPackage = "com.didiglobal.driver",
                bottomRestaurant = "Pizzaria Moema",
                bottomPickup = "Av. Moema, 450",
                bottomDropoff = "Al. dos Maracatins, 800",
                bottomValue = 22.00,
                bottomDistanceKm = 4.5,
                totalRevenue = 40.00,
                netProfit = 22.50,
                haversineMeters = 4850,
                extraMinutes = 24.0,
                alignmentDeg = 175f,
                isApproved = false,
                badgeLabel = "🔴 REJEITADO PELO MOTOR (-4.8km)",
                badgeColor = Color(0xFFFF5252),
                reason = "Divergência de rotas de 175° (direções opostas). Desvio causaria atraso grave e cancelamento pelo cliente."
            )
        )
    }

    var selectedScenarioIndex by remember { mutableIntStateOf(0) }
    val currentScenario = scenarios[selectedScenarioIndex]

    var isSimulatingAcceptance by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.5.dp, Color(0xFF00D2FF).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .testTag("dual_split_window_simulator_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ========================================================
            // 1. CABEÇALHO DO SIMULADOR
            // ========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D2FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Splitscreen, contentDescription = null, tint = Color(0xFF00D2FF), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SIMULADOR DE TELA DIVIDIDA NO GUIDÃO",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "50% Topo (App 1) + 50% Base (App 2) + HUD Tático",
                            color = Color(0xFF00D2FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = currentScenario.badgeColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentScenario.badgeColor)
                ) {
                    Text(
                        text = if (currentScenario.isApproved) "APROVADO" else "BLOQUEADO",
                        color = currentScenario.badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ========================================================
            // 2. SELETOR DE CENÁRIOS PRÁTICOS
            // ========================================================
            Text(
                text = "SELECIONE O CENÁRIO DE CORRIDA:",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.forEachIndexed { idx, sc ->
                    val isSelected = selectedScenarioIndex == idx
                    val btnBorder = if (isSelected) sc.badgeColor else Color(0xFF334155)
                    val btnBg = if (isSelected) sc.badgeColor.copy(alpha = 0.2f) else Color(0xFF0F172A)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = btnBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, btnBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedScenarioIndex = idx
                                isSimulatingAcceptance = false
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (idx) {
                                    0 -> "💎 Diamante"
                                    1 -> "🟡 Transversal"
                                    else -> "🛑 Conflito"
                                },
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (sc.isApproved) "+${sc.haversineMeters}m" else "Bloqueio",
                                color = sc.badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ========================================================
            // 3. MOCKUP INTERATIVO DA TELA DO CELULAR DIVIDIDA (50% / 50%)
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF030712))
                    .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ----------------------------------------------------
                    // 3.1 METADE SUPERIOR (50%): APP PRIMÁRIO (Ex: iFood)
                    // ----------------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF180A0A),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFEA1D2C).copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEA1D2C))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${currentScenario.topAppName.uppercase()} (SUPERIOR - 50%)",
                                        color = Color(0xFFEA1D2C),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = "R$ ${String.format(Locale.US, "%.2f", currentScenario.topValue)}",
                                    color = Color(0xFF00FF88),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏪 Coleta: ${currentScenario.topRestaurant}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📍 Entrega: ${currentScenario.topDropoff} • ${currentScenario.topDistanceKm} km",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ----------------------------------------------------
                    // 3.2 DIVISOR CENTRAL & FLOATING TACTICAL HUD
                    // ----------------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0B132B),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, currentScenario.badgeColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (currentScenario.isApproved) Icons.Default.Diamond else Icons.Default.Block,
                                        contentDescription = null,
                                        tint = currentScenario.badgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "HUD CENTRAL: R$ ${String.format(Locale.US, "%.2f", currentScenario.totalRevenue)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Text(
                                    text = "Lucro Líq: R$ ${String.format(Locale.US, "%.2f", currentScenario.netProfit)}",
                                    color = Color(0xFF00FF88),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Desvio Haversine: +${currentScenario.haversineMeters}m (+${currentScenario.extraMinutes} min)",
                                    color = currentScenario.badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Azimute: ${currentScenario.alignmentDeg.toInt()}°",
                                    color = Color(0xFF00D2FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ----------------------------------------------------
                    // 3.3 METADE INFERIOR (50%): APP SECUNDÁRIO (Ex: 99Food)
                    // ----------------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1608),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFB800).copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFB800))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${currentScenario.bottomAppName.uppercase()} (INFERIOR - 50%)",
                                        color = Color(0xFFFFB800),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = "R$ ${String.format(Locale.US, "%.2f", currentScenario.bottomValue)}",
                                    color = Color(0xFF00FF88),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏪 Coleta: ${currentScenario.bottomRestaurant}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📍 Entrega: ${currentScenario.bottomDropoff} • ${currentScenario.bottomDistanceKm} km",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ========================================================
            // 4. ITINERÁRIO CONSOLIDADO DE 4 PASSOS
            // ========================================================
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B101D)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "ROTA DE EXECUÇÃO OTIMIZADA PELO MOTOR:",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (currentScenario.isApproved) {
                        Text("1. Coletar no ${currentScenario.topRestaurant} (${currentScenario.topAppName})", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                        Text("2. Coletar no ${currentScenario.bottomRestaurant} (${currentScenario.bottomAppName}) - Desvio: +180m", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                        Text("3. Entregar em ${currentScenario.topDropoff}", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                        Text("4. Entregar em ${currentScenario.bottomDropoff} - Desvio final: +${currentScenario.haversineMeters}m", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                    } else {
                        Text(
                            text = "❌ CORRIDA BLOQUEADA: ${currentScenario.reason}",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ========================================================
            // 5. BOTÕES DE ACIONAMENTO REAL
            // ========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão 1: Acionar Tela Dividida Real no Android
                Button(
                    onClick = {
                        val coordinator = SplitScreenDualRadarCoordinator.getInstance(context)
                        val pair = SplitScreenAppPair(
                            id = "pair_sim_${currentScenario.id}",
                            title = "${currentScenario.topAppName} + ${currentScenario.bottomAppName}",
                            topPackage = currentScenario.topPackage,
                            topAppName = currentScenario.topAppName,
                            bottomPackage = currentScenario.bottomPackage,
                            bottomAppName = currentScenario.bottomAppName,
                            description = "Par ativado via simulador no guidão"
                        )

                        try {
                            HapticFeedbackHelper.vibrateSuccess(context)
                            coordinator.launchSplitScreenPair(context, pair)
                            Toast.makeText(context, "📱 Dividindo tela: ${currentScenario.topAppName} (topo) e ${currentScenario.bottomAppName} (base)", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Disparando alternância de tela dividida...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_trigger_real_split")
                ) {
                    Icon(Icons.Default.Splitscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dividir Tela Real", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Botão 2: Simular Aceite Simultâneo e Alerta de Voz
                Button(
                    onClick = {
                        isSimulatingAcceptance = true
                        try {
                            HapticFeedbackHelper.vibrateSuccess(context)
                            val voiceText = if (currentScenario.isApproved) {
                                "Combo aprovado de R$ ${String.format(Locale.US, "%.2f", currentScenario.totalRevenue)}. Coleta um no ${currentScenario.topRestaurant}. Desvio de apenas ${currentScenario.haversineMeters} metros."
                            } else {
                                "Atenção: Corrida descartada por desvio excessivo no sentido oposto."
                            }
                            TextToSpeechManager.getInstance(context).speakVoiceAlert(voiceText)
                            Toast.makeText(context, "⚡ $voiceText", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentScenario.isApproved) Color(0xFF059669) else Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_simulate_accept_combo")
                ) {
                    Icon(
                        imageVector = if (currentScenario.isApproved) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentScenario.isApproved) "Simular Aceite" else "Testar Bloqueio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
