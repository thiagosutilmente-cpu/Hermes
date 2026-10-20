package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.network.AiDealModel
import com.example.network.AiSalesMetricsData
import com.example.network.AiSalesToggleRequest
import com.example.network.AiSimulateRequest
import com.example.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Card executivo de controle da IA Vendedora 100% Autônoma no WhatsApp com
 * ativação instantânea via Pix Asaas na VPS Hostinger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSalesPilotCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAiActive by remember { mutableStateOf(true) }
    var metrics by remember { mutableStateOf(AiSalesMetricsData()) }
    var isLoadingMetrics by remember { mutableStateOf(false) }

    // Simulador
    var simPrompt by remember { mutableStateOf("Quanto custa o app? É caro?") }
    var simResponse by remember { mutableStateOf<String?>(null) }
    var simActionType by remember { mutableStateOf<String?>(null) }
    var isSimulating by remember { mutableStateOf(false) }

    // Vendas Recentes da IA
    var recentDeals by remember {
        mutableStateOf(
            listOf(
                AiDealModel(
                    phone = "5511987654321",
                    name = "Robson Motoboy (iFood Leste)",
                    status = "100% ATIVADO (PIX CONFIRMADO)",
                    valueReais = 19.90,
                    closedBy = "IA Autônoma (Sem intervenção)",
                    timestamp = "Hoje, 11:04"
                ),
                AiDealModel(
                    phone = "5511971239845",
                    name = "Marcos Entregas (Zona Sul)",
                    status = "100% ATIVADO (PIX CONFIRMADO)",
                    valueReais = 19.90,
                    closedBy = "IA Autônoma (Sem intervenção)",
                    timestamp = "Hoje, 10:42"
                )
            )
        )
    }

    // Carrega dados iniciais da VPS
    fun refreshData() {
        isLoadingMetrics = true
        coroutineScope.launch {
            try {
                val statusRes = withContext(Dispatchers.IO) {
                    ApiClient.vpsApi.getAiSalesStatus()
                }
                if (statusRes.isSuccessful && statusRes.body()?.data != null) {
                    val d = statusRes.body()!!.data!!
                    metrics = d
                    isAiActive = d.enabled
                }
                val dealsRes = withContext(Dispatchers.IO) {
                    ApiClient.vpsApi.getAiSalesDeals()
                }
                if (dealsRes.isSuccessful && dealsRes.body()?.deals != null) {
                    recentDeals = dealsRes.body()!!.deals
                }
            } catch (e: Exception) {
                // Mantém métricas padrão em caso de timeout
            } finally {
                isLoadingMetrics = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_sales_pilot_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1B2A) // Azul Profundo Executivo
        ),
        border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // CABEÇALHO DO CARD COM STATUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "IA Autônoma",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "IA VENDEDORA 100% AUTÔNOMA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "VPS Hostinger • Negociação & Pix Instantâneo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF80D8FF),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = { refreshData() },
                    modifier = Modifier.testTag("refresh_ai_sales_btn")
                ) {
                    if (isLoadingMetrics) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF00E5FF),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BADGE DE STATUS + TOGGLE
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1B2A47),
                border = BorderStroke(1.dp, if (isAiActive) Color(0xFF00E676) else Color(0xFFFF5252).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isAiActive) Color(0xFF00E676) else Color(0xFFFF5252))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAiActive) "PILOTO AUTOMÁTICO ATIVO (24H)" else "IA PAUSADA TEMPORARIAMENTE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAiActive) Color(0xFF00E676) else Color(0xFFFF8A80)
                                )
                            )
                        }
                        Text(
                            text = if (isAiActive)
                                "IA atende no WhatsApp, tira dúvidas e libera contas via Pix sem ação humana."
                            else
                                "Atendimento pausado. Clique na chave para religar.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Switch(
                        checked = isAiActive,
                        onCheckedChange = { newState ->
                            isAiActive = newState
                            coroutineScope.launch {
                                try {
                                    val res = withContext(Dispatchers.IO) {
                                        ApiClient.vpsApi.toggleAiSales(AiSalesToggleRequest(enabled = newState))
                                    }
                                    if (res.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            if (newState) "IA Vendedora Ligada 24h na VPS!" else "IA Pausada",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        refreshData()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao alterar IA na VPS", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E676),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2C3E50)
                        ),
                        modifier = Modifier.testTag("toggle_ai_sales_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // GRID DE MÉTRICAS DA IA VENDEDORA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Caixa 1: Vendas Fechadas e Ativadas
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${metrics.pixPaidAuto}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676),
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "VENDAS 100% AUTO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFB9F6CA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                // Caixa 2: Faturamento Automático
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "R$ ${"%.2f".format(metrics.totalRevenueReais)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "FATURADO NO PILOTO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFFF9C4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                // Caixa 3: Taxa de Conversão
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${metrics.conversionRatePercent}%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "CONVERSÃO DA IA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF80D8FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LINHA DE SUPORTE: Objeções & Pix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💬 ${metrics.totalConversations} motoboys atendidos",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF90A4AE), fontSize = 11.sp)
                )
                Text(
                    text = "🛡️ ${metrics.objectionsHandled} objeções quebradas",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF80D8FF), fontSize = 11.sp)
                )
                Text(
                    text = "⚡ ${metrics.pixGenerated} Pix enviados",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFD54F), fontSize = 11.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1E3A5F))
            Spacer(modifier = Modifier.height(14.dp))

            // SIMULADOR DE NEGOCIAÇÃO AO VIVO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Simulador",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SIMULAR RESPOSTA DA IA (TESTE AO VIVO)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Digite qualquer pergunta ou objeção que um motoboy faria para ver como a IA convence e fecha:",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips rápidos de perguntas comuns de motoboys
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val samplePrompts = listOf(
                    "Quanto custa? É caro?",
                    "Funciona com celular no bolso?",
                    "Manda a chave Pix que eu pago",
                    "Avisa blitz escondida e comando?"
                )
                samplePrompts.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF162A45),
                        border = BorderStroke(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        onClick = { simPrompt = p }
                    ) {
                        Text(
                            text = p,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF80D8FF),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = simPrompt,
                onValueChange = { simPrompt = it },
                label = { Text("Pergunta ou Objeção do Motoboy", color = Color(0xFF80D8FF), fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sim_prompt_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF1E3A5F),
                    focusedContainerColor = Color(0xFF091422),
                    unfocusedContainerColor = Color(0xFF091422)
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (simPrompt.isBlank()) return@Button
                    isSimulating = true
                    simResponse = null
                    coroutineScope.launch {
                        try {
                            val res = withContext(Dispatchers.IO) {
                                ApiClient.vpsApi.simulateAiNegotiation(
                                    AiSimulateRequest(
                                        phone = "5511999999999",
                                        message = simPrompt
                                    )
                                )
                            }
                            if (res.isSuccessful && res.body()?.simulation != null) {
                                val s = res.body()!!.simulation!!
                                simResponse = s.aiResponse
                                simActionType = s.actionType
                            } else {
                                simResponse = "A IA responderia de forma persuasiva tirando dúvidas e enviando a chave Pix Asaas."
                            }
                        } catch (e: Exception) {
                            simResponse = "Simulação local: Fala parceiro! O app avisa blitz com som no fone e celular no bolso. Custa menos de R$ 0,70/dia. Manda o 'quero' que libero seu Pix na hora!"
                        } finally {
                            isSimulating = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("test_ai_simulation_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                enabled = !isSimulating
            ) {
                if (isSimulating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF0D1B2A),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerando Resposta Persuasiva...", color = Color(0xFF0D1B2A), fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Simular",
                        tint = Color(0xFF0D1B2A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GERAR RESPOSTA DA IA NO WHATSAPP",
                        color = Color(0xFF0D1B2A),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            AnimatedVisibility(visible = simResponse != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF082032),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sucesso",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RESPOSTA QUE A IA ENVIARÁ NO WHATSAPP:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF00E676),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                if (simActionType != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1B5E20)
                                    ) {
                                        Text(
                                            text = simActionType!!,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = simResponse ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1E3A5F))
            Spacer(modifier = Modifier.height(14.dp))

            // REGISTRO DE VENDAS FECHADAS 100% AUTOMÁTICAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Paid,
                        contentDescription = "Vendas",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VENDAS FECHADAS NO PILOTO AUTOMÁTICO",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1B5E20)
                ) {
                    Text(
                        text = "ZERO TOQUE MANUAL",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFB9F6CA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentDeals.forEach { deal ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF102035),
                        border = BorderStroke(0.8.dp, Color(0xFF00E676).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deal.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "📱 ${deal.phone} • ${deal.timestamp}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF90A4AE),
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "R$ ${"%.2f".format(deal.valueReais)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "CONTA LIBERADA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF80D8FF),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
