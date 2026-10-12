package com.example

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card de Cockpit para o HapticFeedbackEngine.
 * Permite ao entregador:
 * 1. Visualizar o status de calibração para o suporte de guidão da moto
 * 2. Sentir e auditar em tempo real as 3 assinaturas táteis exclusivas:
 *    - Alta Rentabilidade (Ta-Ta-TUM!)
 *    - Corrida Longa (Pulso estendido contínuo)
 *    - Rejeição Automática (Corte seco duplo)
 * 3. Alternar o perfil de amortecimento/montagem do aparelho.
 */
@Composable
fun HapticCockpitControlCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = remember { HapticFeedbackEngine.getInstance(context) }
    val isEnabled by engine.isHapticEnabled.collectAsState()
    val mountingProfile by engine.mountingProfile.collectAsState()
    val lastCategory by engine.lastTriggeredCategory.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("haptic_cockpit_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141923)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isEnabled) Color(0xFF9D4EDD).copy(alpha = 0.5f) else Color.DarkGray
            ),
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertiling if false else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9D4EDD).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Haptic Engine",
                            tint = Color(0xFFC77DFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HAPTIC GUIDÃO ENGINE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CALIBRADO GUIDÃO",
                                    color = Color(0xFF00E676),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (isEnabled) "Identificação tátil cega ativa para o piloto" else "Feedback tátil pausado",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { engine.setHapticEnabled(it) },
                        modifier = Modifier.testTag("switch_haptic_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFC77DFF),
                            checkedTrackColor = Color(0xFF5A189A)
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir Haptic",
                        tint = Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Status do último disparo
            lastCategory?.let { category ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF241438))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Último pulso tátil: ${category.label}",
                            color = Color(0xFFE0AAFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Painel Expandido com Testes Práticos e Perfis de Guidão
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Text(
                        text = "TESTE TÁTIL NO GUIDÃO (SINTA AS ASSINATURAS)",
                        color = Color(0xFFC77DFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botão 1: Alta Rentabilidade
                    ElevatedButton(
                        onClick = { engine.vibrateHighProfit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_high_profit"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF1B4332),
                            contentColor = Color(0xFF74C69D)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🏆 Alta Rentabilidade (Ta-Ta-TUM!)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Pulso Triplo",
                                fontSize = 10.sp,
                                color = Color(0xFFB7E4C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Botão 2: Corrida Longa
                    ElevatedButton(
                        onClick = { engine.vibrateLongDistance() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_long_distance"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF003049),
                            contentColor = Color(0xFF669BBC)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🛣️ Corrida Longa (>10 km)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Onda Sustentada",
                                fontSize = 10.sp,
                                color = Color(0xFFAED9E0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Botão 3: Rejeição Automática
                    ElevatedButton(
                        onClick = { engine.vibrateAutoReject() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_auto_reject"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF4A0E17),
                            contentColor = Color(0xFFFF758F)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⛔ Rejeição Automática (Tuc-Tuc)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Corte Seco",
                                fontSize = 10.sp,
                                color = Color(0xFFFFB3C1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seleção do Perfil de Montagem Física
                    Text(
                        text = "PERFIL DE MONTAGEM FÍSICA NO VEÍCULO",
                        color = Color.LightGray,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PhoneMountingProfile.values().forEach { profile ->
                        val isSelected = mountingProfile == profile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2E1A47) else Color(0xFF1E222D))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFC77DFF) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { engine.setMountingProfile(profile) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile.label,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "${(profile.amplitudeMultiplier * 100).toInt()}% Potência",
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
