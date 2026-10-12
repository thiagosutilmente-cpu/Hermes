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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.LicenseStatusState
import com.example.network.SubscriptionViewModel

/**
 * SubscriptionStatusLiveCard
 *
 * Card reativo para o Cockpit que monitora em tempo real o status da licença do entregador
 * (ATIVA vs BLOQUEADA), consumindo o SubscriptionViewModel e SubscriptionRepository.
 */
@Composable
fun SubscriptionStatusLiveCard(
    viewModel: SubscriptionViewModel,
    modifier: Modifier = Modifier,
    onOpenPaywall: () -> Unit = {}
) {
    val state by viewModel.licenseState.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var customerIdInput by remember { mutableStateOf(state.customerId) }

    val statusColor = when {
        state.isLicenseActive -> Color(0xFF00E676) // Verde Neon
        state.status.contains("OVERDUE", ignoreCase = true) -> Color(0xFFFF5252) // Vermelho Alerta
        else -> Color(0xFFFFB300) // Ambar Atenção
    }

    val statusTitle = when {
        state.isLicenseActive -> "LICENÇA PRO ATIVA (7 DIAS)"
        state.status.contains("OVERDUE", ignoreCase = true) -> "ACESSO PRO BLOQUEADO (INADIMPLENTE)"
        else -> "STATUS: ${state.status.uppercase()}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subscription_status_live_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141724)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, statusColor.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho com Badge de Status e Botão de Sincronização
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isLicenseActive) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Status da Licença",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = statusTitle,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (state.isLicenseActive)
                                "Plano Semanal R$ 25,00 • Split 40% Líder"
                            else
                                "Recursos neurais suspensos até regularização",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    }
                }

                // Botão de Refresh / Sincronizar com FastAPI
                IconButton(
                    onClick = { viewModel.refreshStatus() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_license_status_button"),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF00E676),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizar Licença",
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detalhes da Licença: Cliente, Chave de Ativação e Dias Restantes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D0F18))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ID DO CONDUTOR (ASAAS)",
                        color = Color(0xFF757575),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.customerId,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CHAVE DE ATIVAÇÃO",
                        color = Color(0xFF757575),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.activationKey.isNotEmpty()) state.activationKey else "NÃO GERADA",
                        color = if (state.activationKey.isNotEmpty()) Color(0xFFFFD700) else Color(0xFF757575),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Aviso de Conexão",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.errorMessage ?: "",
                        color = Color(0xFFFF8A80),
                        fontSize = 10.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ações Rápidas: Renovar / Testar Webhooks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.isLicenseActive) {
                    Button(
                        onClick = onOpenPaywall,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_regularizar_licenca_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF0A0A0F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REGULARIZAR (R$ 25)",
                            color = Color(0xFF0A0A0F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.weight(if (!state.isLicenseActive) 0.6f else 1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9))
                ) {
                    Text(
                        text = if (isExpanded) "RECUAR" else "SIMULADOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gaveta retrátil do Simulador de Webhooks FastAPI
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090A10))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "AUDITORIA DE WEBHOOKS & TESTE DE REGRAS (PASSO 1)",
                        color = Color(0xFF81D4FA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerIdInput,
                        onValueChange = {
                            customerIdInput = it
                            viewModel.setCustomerId(it)
                        },
                        label = { Text("Customer ID Asaas", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color(0xFF424242)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulatePaymentReceived() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_payment_received_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text(
                                text = "RECEBIDO (+7D)",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = { viewModel.simulatePaymentOverdue() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_payment_overdue_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text(
                                text = "VENCIDO (BLOQUEIO)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
