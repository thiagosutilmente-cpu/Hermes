package com.example

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card de Gestão de Notificações em Segundo Plano.
 * Permite ao entregador visualizar o status das notificações push locais de alta prioridade
 * e testar o disparo de alerta sonoro e vibratório enquanto o aplicativo estiver em segundo plano.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LocalNotificationStatusCard(
    hasNotificationPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (hasNotificationPermission) NeonGreen.copy(alpha = 0.35f) else Color(0xFFE5A000).copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("local_notification_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NOTIFICAÇÕES EM SEGUNDO PLANO",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (hasNotificationPermission) "Alertas Heads-up Ativos no Bolso" else "Permissão de Notificação Pendente",
                            color = if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hasNotificationPermission) NeonGreen.copy(alpha = 0.15f) else Color(0xFFE5A000).copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (hasNotificationPermission) "ALTA PRIORIDADE" else "PENDENTE",
                        color = if (hasNotificationPermission) NeonGreen else Color(0xFFE5A000),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Quando você alternar para o Waze, Google Maps ou bloquear a tela, o Radar Coordinator emitirá um banner flutuante com som e vibração para ofertas com ganho/km vantajoso.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasNotificationPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5A000),
                            contentColor = DarkBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_request_notification_permission")
                    ) {
                        Text(
                            text = "🔔 Ativar Alertas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onTestNotification,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCardElevated,
                        contentColor = NeonGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .testTag("btn_test_notification")
                ) {
                    Text(
                        text = "⚡ Testar Alerta Push",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
