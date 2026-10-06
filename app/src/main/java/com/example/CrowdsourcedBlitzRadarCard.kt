package com.example

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonEmerald = Color(0xFF00FF88)
private val AlertRed = Color(0xFFFF3366)
private val WarningAmber = Color(0xFFFFB300)
private val CyberCyan = Color(0xFF00E5FF)
private val DarkCardSurface = Color(0xFF10141D)
private val CardBorderColor = Color(0xFF1F2636)
private val TextLight = Color(0xFFF3F4F8)
private val TextMuted = Color(0xFF8A93A6)

@Composable
fun CrowdsourcedBlitzRadarCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blitzManager = remember { CrowdsourcedBlitzRadarManager.getInstance(context) }
    val hazardsList by blitzManager.activeHazards.collectAsState()
    val nearestHazard by blitzManager.closestHazard.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_crowdsourced_blitz_radar"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AlertRed.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🚔", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RADAR COMUNITÁRIO • BLITZ & RISCO",
                                color = TextLight,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "Waze para Motoboys com alerta por voz no fone",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Surface(
                    color = AlertRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${hazardsList.size} PONTOS ATIVOS",
                        color = AlertRed,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Botões de Reporte Rápido em 1 Toque
            Text(
                text = "REPORTAR EM 1 TOQUE (COMPARTILHAR COM A TROPA):",
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickReportButton(
                    icon = "🚔",
                    label = "Blitz PM",
                    color = AlertRed,
                    modifier = Modifier.weight(1f)
                ) {
                    blitzManager.reportHazard(
                        HazardType.POLICE_BLITZ,
                        -23.5616,
                        -46.6559,
                        "Av. Paulista cruzamento com Pamplona",
                        "Comando de trânsito fiscalizando"
                    )
                }

                QuickReportButton(
                    icon = "📸",
                    label = "Radar Móvel",
                    color = WarningAmber,
                    modifier = Modifier.weight(1f)
                ) {
                    blitzManager.reportHazard(
                        HazardType.SPEED_RADAR,
                        -23.5700,
                        -46.6500,
                        "Av. 23 de Maio",
                        "Radar móvel na descida"
                    )
                }

                QuickReportButton(
                    icon = "🌧️",
                    label = "Chuva",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    blitzManager.reportHazard(
                        HazardType.HEAVY_RAIN,
                        -23.5500,
                        -46.6600,
                        "Rua da Consolação",
                        "Alagamento na faixa da direita"
                    )
                }

                QuickReportButton(
                    icon = "⚠️",
                    label = "Perigo",
                    color = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f)
                ) {
                    blitzManager.reportHazard(
                        HazardType.DANGER_ZONE,
                        -23.5400,
                        -46.6300,
                        "Centro Histórico",
                        "Ponto de risco relatado"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lista dos 3 Alertas mais recentes ou próximos
            Text(
                text = "ALERTAS EM TEMPO REAL NA REGIÃO:",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            hazardsList.take(3).forEach { hazard ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C1018))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = hazard.type.icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = hazard.title,
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = hazard.address,
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF182230),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                blitzManager.confirmHazard(hazard.id)
                                Toast.makeText(context, "+1 Confirmação registrada!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "👍 ${hazard.confirmationsCount}",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botão para testar o alerta de voz preventivo no fone
            Button(
                onClick = {
                    blitzManager.updateLocationAndCheckProximity(-23.5780, -46.6430)
                    Toast.makeText(context, "🔊 Disparando alerta de blitz no fone...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("btn_test_blitz_voice_alert"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2B1218),
                    contentColor = AlertRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f))
            ) {
                Text(text = "TESTAR ALERTA DE BLITZ NO FONE (TTS) 🔊", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun QuickReportButton(
    icon: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF141A26),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
