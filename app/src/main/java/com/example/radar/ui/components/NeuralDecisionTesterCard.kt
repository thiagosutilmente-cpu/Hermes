package com.example.radar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radar.ui.theme.DarkBackground
import com.example.radar.ui.theme.DarkSurface
import com.example.radar.ui.theme.DarkSurfaceBorder
import com.example.radar.ui.theme.DarkSurfaceElevated
import com.example.radar.ui.theme.NeonEmerald
import com.example.radar.ui.theme.StatusOffline
import com.example.radar.ui.theme.TextMuted
import com.example.radar.ui.theme.TextPrimary
import com.example.radar.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun NeuralDecisionTesterCard(
    onSimulateDecision: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offerValue by remember { mutableFloatStateOf(32.0f) }
    var distanceKm by remember { mutableFloatStateOf(4.5f) }
    var selectedApp by remember { mutableStateOf("iFood + Rappi") }

    val gainPerKm = if (distanceKm > 0) offerValue / distanceKm else offerValue
    val decisionResult = remember(offerValue, distanceKm) {
        when {
            gainPerKm >= 5.0 -> Triple("ACEITAR", NeonEmerald, "Ganho/km acima da meta (R$ ${String.format(Locale.GERMANY, "%.2f", gainPerKm)}/km)")
            gainPerKm >= 3.5 && distanceKm <= 4.0 -> Triple("ACEITAR", NeonEmerald, "Distância curta compensa o trajeto")
            distanceKm > 6.0 -> Triple("RECUSAR", StatusOffline, "Distância excessiva (> 6.0 km)")
            else -> Triple("RECUSAR", StatusOffline, "Ganho/km abaixo do ideal")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🧠", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SIMULADOR DE DECISÃO NEURAL",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Avaliador de algoritmo em tempo real",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { isExpanded = !isExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = NeonEmerald
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isExpanded) "RECOLHER" else "TESTAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Slider Valor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valor da Oferta:", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "R$ ${String.format(Locale.GERMANY, "%.2f", offerValue)}",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = offerValue,
                        onValueChange = { offerValue = it },
                        valueRange = 8f..80f,
                        steps = 71,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonEmerald,
                            activeTrackColor = NeonEmerald,
                            inactiveTrackColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.testTag("slider_value")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Slider Distância
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distância do Trajeto:", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "${String.format(Locale.GERMANY, "%.1f", distanceKm)} km",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = distanceKm,
                        onValueChange = { distanceKm = it },
                        valueRange = 1f..15f,
                        steps = 27,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = TextPrimary,
                            inactiveTrackColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.testTag("slider_distance")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Resultado da Avaliação
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .border(1.dp, decisionResult.second.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PARECER JARVIS:",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = decisionResult.third,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(decisionResult.second.copy(alpha = 0.2f))
                                    .border(1.dp, decisionResult.second, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = decisionResult.first,
                                    color = decisionResult.second,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onSimulateDecision(offerValue.toDouble(), distanceKm.toDouble(), selectedApp)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("inject_offer_button")
                    ) {
                        Text("🚀 INJETAR OFERTA NO RADAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
