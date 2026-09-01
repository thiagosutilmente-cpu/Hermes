package com.example.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.radar.ui.components.ApiConnectionStatusBadge
import com.example.radar.ui.components.LiveOfferCard
import com.example.radar.ui.components.MonitoringControlCard
import com.example.radar.ui.theme.App99Yellow
import com.example.radar.ui.theme.DarkBackground
import com.example.radar.ui.theme.DarkSurface
import com.example.radar.ui.theme.DarkSurfaceBorder
import com.example.radar.ui.theme.DarkSurfaceElevated
import com.example.radar.ui.theme.IFoodRed
import com.example.radar.ui.theme.NeonEmerald
import com.example.radar.ui.theme.RappiOrange
import com.example.radar.ui.theme.TextMuted
import com.example.radar.ui.theme.TextPrimary
import com.example.radar.ui.theme.TextSecondary
import com.example.radar.viewmodel.RadarMonitorViewModel
import java.util.Locale

@Composable
fun RadarMonitorScreen(
    viewModel: RadarMonitorViewModel,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val monitoringState by viewModel.monitoringState.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // TOP BAR: Logo + Indicador de Conexão com API
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "RADAR COORDINATOR",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Jarvis Neural Cockpit",
                                color = NeonEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Indicador de Status da Conexão com a API de Entregas
                    ApiConnectionStatusBadge(
                        status = connectionStatus,
                        onReconnectClick = { viewModel.reconnectApi() },
                        modifier = Modifier.testTag("api_connection_badge")
                    )
                }
            }

            // CONTROLE PRINCIPAL DE MONITORAMENTO (Ativar / Desativar)
            item {
                MonitoringControlCard(
                    isMonitoringActive = monitoringState.isMonitoringActive,
                    onToggleMonitoring = { viewModel.toggleMonitoring() },
                    onActivate = { viewModel.setMonitoringActive(true) },
                    onDeactivate = { viewModel.setMonitoringActive(false) },
                    modifier = Modifier.testTag("monitoring_card")
                )
            }

            // STATUS DAS APIS INTEGRADAS (iFood, Rappi, Uber, 99)
            item {
                ConnectedApisRow(activeServices = connectionStatus.activeServices)
            }

            // CARDS DE MÉTRICAS RÁPIDAS
            item {
                MetricsOverviewRow(
                    scanned = monitoringState.offersScannedToday,
                    accepted = monitoringState.acceptedOffers,
                    earnings = monitoringState.totalEarningsToday
                )
            }

            // SIMULADOR DE DECISÃO NEURAL JARVIS
            item {
                com.example.radar.ui.components.NeuralDecisionTesterCard(
                    onSimulateDecision = { valAmount, dist, app ->
                        viewModel.injectCustomOffer(valAmount, dist, app)
                    }
                )
            }

            // FLUXO DE OFERTA INTERCEPTADA EM TEMPO REAL
            item {
                Text(
                    text = "OFERTA INTERCEPTADA EM TEMPO REAL",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                val currentOffer = monitoringState.lastDetectedOffer
                if (currentOffer != null && monitoringState.isMonitoringActive) {
                    LiveOfferCard(
                        offer = currentOffer,
                        onAccept = { viewModel.acceptCurrentOffer() },
                        onDecline = { viewModel.declineCurrentOffer() }
                    )
                } else {
                    EmptyOfferState(isMonitoringActive = monitoringState.isMonitoringActive)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ConnectedApisRow(
    activeServices: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ApiPill(name = "iFood", color = IFoodRed)
        ApiPill(name = "Rappi", color = RappiOrange)
        ApiPill(name = "Uber", color = TextPrimary)
        ApiPill(name = "99", color = App99Yellow)
    }
}

@Composable
fun ApiPill(name: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MetricsOverviewRow(
    scanned: Int,
    accepted: Int,
    earnings: Double,
    modifier: Modifier = Modifier
) {
    val formattedEarnings = String.format(Locale.GERMANY, "R$ %.2f", earnings)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricItem(title = "SCAN HOJE", value = "$scanned", modifier = Modifier.weight(1f))
        MetricItem(title = "ACEITES", value = "$accepted", modifier = Modifier.weight(1f))
        MetricItem(
            title = "GANHO HOJE",
            value = formattedEarnings,
            valueColor = NeonEmerald,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
fun MetricItem(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun EmptyOfferState(isMonitoringActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMonitoringActive) "🛰️" else "⏸️",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isMonitoringActive) "Radar varrendo ofertas..." else "Monitoramento desativado",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isMonitoringActive) "Próxima oportunidade com R$ > 5.0/km aparecerá aqui" else "Ative o radar acima para receber ofertas inteligentes",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
