package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val NeonGreen = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00D2FF)
private val CyberPurple = Color(0xFF9D00FF)
private val CardSurface = Color(0xFF13131D)
private val CardBorder = Color(0xFF232335)
private val TextLight = Color(0xFFF0F0F8)
private val TextMuted = Color(0xFF8888A2)

@Composable
fun GhostApiCockpitCard(
    modifier: Modifier = Modifier,
    onOpenTacticalHud: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val telemetry by GhostApiVirtualBus.telemetry.collectAsState()
    val offers by GhostApiVirtualBus.offersState.collectAsState()

    var isTestingCall by remember { mutableStateOf(false) }
    var testResponseJson by remember { mutableStateOf<String?>(null) }
    var showApiDocs by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ghost_api_cockpit_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(NeonGreen.copy(alpha = 0.6f), CyberCyan.copy(alpha = 0.6f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho da GhostAPI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(NeonGreen.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.2f)))
                            )
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "GhostAPI",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GHOST-API VIRTUAL BUS",
                                color = TextLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberPurple.copy(alpha = 0.3f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ZERO-API",
                                    color = Color(0xFFD48BFF),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Ponte local de alta velocidade entre iFood, 99 e Uber",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Badge de Status Online
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (telemetry.isServerRunning) NeonGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isServerRunning) NeonGreen else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (telemetry.isServerRunning) ":${telemetry.port} ATIVO" else "OFFLINE",
                        color = if (telemetry.isServerRunning) NeonGreen else Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid de Métricas de Baixo Nível
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "LATÊNCIA",
                    value = "${telemetry.averageLatencyMs} ms",
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "REQUISIÇÕES",
                    value = "${telemetry.totalRequestsServed}",
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "INTERCEPTADAS",
                    value = "${telemetry.totalOffersIntercepted}",
                    accentColor = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Apps Acoplados
            Text(
                text = "APLICATIVOS ACOPLADOS VIA MEMÓRIA & SOCKET:",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                telemetry.activeMonitoredApps.forEach { appName ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E1E2C))
                            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "● $appName",
                            color = TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ações de Teste e Diagnóstico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isTestingCall = true
                        coroutineScope.launch {
                            val res = executeLocalHttpCall("http://127.0.0.1:8899/v1/status")
                            testResponseJson = res
                            isTestingCall = false
                            HapticFeedbackHelper.vibrateSuccess(context)
                            Toast.makeText(context, "GhostAPI respondeu em 3ms!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_test_ghost_api"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A22))
                ) {
                    if (isTestingCall) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Testar",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Testar REST",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = {
                        val sim = GhostApiVirtualBus.simulateSyntheticOffer("iFood", 28.50, 3.8)
                        HapticFeedbackHelper.vibrateAccept(context)
                        Toast.makeText(context, "iFood R$ 28,50 injetado na GhostAPI!", Toast.LENGTH_SHORT).show()
                        testResponseJson = sim.toJson().toString(2)
                        try {
                            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                                "GhostAPI interceptou iFood: R$ 28,50 por 3,8 km. Lucro de R$ 7,50 por km."
                            )
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_simulate_synthetic_offer"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2332))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Injetar",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Simular iFood",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showApiDocs = !showApiDocs },
                    modifier = Modifier.testTag("btn_toggle_api_docs"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Docs",
                        tint = TextLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Exibição do Retorno JSON do teste
            AnimatedVisibility(
                visible = testResponseJson != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF09090D))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RETORNO HTTP 200 (localhost:8899):",
                            color = NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Limpar",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { testResponseJson = null }
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = testResponseJson ?: "",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Documentação dos Endpoints REST
            AnimatedVisibility(visible = showApiDocs) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B0E14))
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ENDPOINTS REST LOCAIS (ZERO-API):",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiDocRow("GET", "/v1/status", "Telemetria de saúde e latência")
                    ApiDocRow("GET", "/v1/offers/live", "Feed de todas as ofertas unificadas")
                    ApiDocRow("GET", "/v1/offers/latest", "Melhor corrida por rentabilidade (R$/km)")
                    ApiDocRow("POST", "/v1/offers/accept", "Disparo do aceite atômico sem trocar de tela")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$ curl http://127.0.0.1:8899/v1/offers/latest",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181824))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ApiDocRow(method: String, endpoint: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = method,
            color = if (method == "GET") NeonGreen else Color(0xFFFFB800),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = endpoint,
            color = TextLight,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = "→ $description",
            color = TextMuted,
            fontSize = 8.sp,
            maxLines = 1
        )
    }
}

private suspend fun executeLocalHttpCall(urlString: String): String = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 1500
        connection.readTimeout = 1500
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val reader = BufferedReader(InputStreamReader(stream))
        val sb = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
            sb.append(line).append("\n")
            line = reader.readLine()
        }
        sb.toString().trim()
    } catch (e: Exception) {
        "{\"error\": \"Falha ao conectar: ${e.message}\"}"
    } finally {
        connection?.disconnect()
    }
}
