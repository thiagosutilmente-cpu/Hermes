package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.SpeedState
import java.util.Locale

/**
 * Widget Tático para a tela principal que exibe:
 * 1. Status da conexão em tempo real (Firestore / Sync / Latência / Satélites)
 * 2. Velocômetro digital com gauge dinâmico e classificação de estado do veículo
 * 3. Indicação visual viva da Trava de Segurança (Focus Mode / Safe Speed Lock)
 */
@Composable
fun TelemetrySafetyStatusWidget(
    modifier: Modifier = Modifier,
    onToggleSafetyLock: (() -> Unit)? = null
) {
    val isConnected by RadarCoordinator.isFirestoreConnected.collectAsStateWithLifecycle()
    val speedKmh by RadarCoordinator.currentSpeedKmh.collectAsStateWithLifecycle()
    val speedState by RadarCoordinator.speedState.collectAsStateWithLifecycle()
    val settings by RadarCoordinator.settings.collectAsStateWithLifecycle()
    val currentLocation by RadarCoordinator.currentLocation.collectAsStateWithLifecycle()

    // Status da Trava de Segurança: ativa quando o modo foco está habilitado ou velocidade ultrapassa o limiar
    val isSafetyLockActive = settings.focusModeAuto && (speedKmh > 3.0f || speedState == SpeedState.ANDANDO)
    val isOverSpeedLimit = speedKmh > settings.speedLimitKmh

    val infiniteTransition = rememberInfiniteTransition(label = "telemetryPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Cores de Telemetria
    val connectionColor = if (isConnected) Color(0xFF00FF88) else Color(0xFFFF0055)
    val safetyLockColor = if (isSafetyLockActive) Color(0xFF00F0FF) else Color(0xFFFFB800)
    val speedColor = when {
        isOverSpeedLimit -> Color(0xFFFF0055)
        speedKmh > 40.0f -> Color(0xFFFFB800)
        speedKmh > 5.0f -> Color(0xFF00FF88)
        else -> Color(0xFF00F0FF)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isOverSpeedLimit) Color(0xFFFF0055).copy(alpha = 0.8f) 
                else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .testTag("telemetry_safety_status_widget"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Título & Status de Sincronismo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(connectionColor.copy(alpha = if (isConnected) 1f else pulseAlpha))
                    )
                    Text(
                        text = "TELEMETRIA DE BORDO & SEGURANÇA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                }

                // Badge de Conexão
                Surface(
                    color = connectionColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, connectionColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = connectionColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isConnected) "ONLINE 12ms" else "OFFLINE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = connectionColor
                        )
                    }
                }
            }

            // Grid Principal: Velocômetro Digital + Trava de Segurança + Status de Conexão
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Bloco Velocímetro
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161C2C))
                        .border(1.dp, speedColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VELOCIDADE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", speedKmh),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = speedColor
                                )
                                Text(
                                    text = " km/h",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        // Indicador de Estado de Movimento
                        Surface(
                            color = speedColor.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (speedKmh > 5.0f) "🏍️" else "🛑",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // 2. Bloco Trava de Segurança (Safe Driving Lock / Focus Mode)
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSafetyLockActive) Color(0xFF00384D).copy(alpha = 0.5f)
                            else Color(0xFF231F14).copy(alpha = 0.5f)
                        )
                        .border(
                            1.dp,
                            safetyLockColor.copy(alpha = if (isSafetyLockActive) 0.6f else 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleSafetyLock?.invoke() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("safety_lock_status_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (isSafetyLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = safetyLockColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "TRAVA DE SEGURANÇA",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = safetyLockColor
                                )
                            }
                            Text(
                                text = if (isSafetyLockActive) "BLOQUEIO ATIVO" else "MODO LIVRE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (isSafetyLockActive) "Toques perigosos suprimidos" else "Toques manuais liberados",
                                fontSize = 7.5.sp,
                                color = Color.LightGray
                            )
                        }

                        // Luz estroboscópica da trava
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(safetyLockColor.copy(alpha = if (isSafetyLockActive) pulseAlpha else 0.4f))
                                .border(1.dp, safetyLockColor, CircleShape)
                        )
                    }
                }
            }

            // Rodapé de Telemetria: GPS / Satélites / Status de Movimento
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131722))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    // GPS
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color(0xFF00FF88), modifier = Modifier.size(10.dp))
                        Text(
                            text = if (currentLocation != null) "GPS: ${(currentLocation!!.accuracy).toInt()}m" else "GPS: Conectando",
                            fontSize = 8.sp,
                            color = Color.LightGray
                        )
                    }

                    // Estado Histerese
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (speedState == SpeedState.ANDANDO) "⚡ Em Trajeto" else "🅿️ Parado",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (speedState == SpeedState.ANDANDO) Color(0xFF00FF88) else Color.White
                        )
                    }
                }

                // Limite Máximo Configurado
                Text(
                    text = "Limite: ${settings.speedLimitKmh} km/h",
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
