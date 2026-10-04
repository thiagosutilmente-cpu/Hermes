package com.example

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modo B: Troca Rápida Assistida (Assisted Quick-Switch HUD)
 *
 * Arquitetura 100% Anti-Ban e Segura:
 * O Jarvis calcula a rota dupla (iFood + 99/Uber), valida o SLA e projeta o card flutuante.
 * Ao tocar em "Aceitar & Trocar Rápido", o app dispara o Intent nativo que traz o segundo app
 * instantaneamente para o polegar do piloto com feedback tátil (vibração dupla).
 * Nenhum clique robótico é injetado, garantindo conformidade total e zero risco de bloqueio.
 */
object AssistedQuickSwitchManager {

    // Pacotes oficiais dos aplicativos de entrega suportados
    const val PACKAGE_IFOOD = "com.ifood.driver"
    const val PACKAGE_UBER = "com.ubercab.driver"
    const val PACKAGE_99 = "com.taxis99"
    const val PACKAGE_RAPPI = "com.grability.rappi"

    data class DualRouteOpportunity(
        val id: String,
        val primaryApp: String, // ex: "iFood"
        val primaryValue: Double, // ex: 18.00
        val secondaryApp: String, // ex: "99 Moto"
        val secondaryPackage: String, // ex: PACKAGE_99
        val secondaryValue: Double, // ex: 16.00
        val totalCombinedValue: Double = primaryValue + secondaryValue,
        val extraDeviationMeters: Int = 450,
        val extraMinutes: Int = 3,
        val slaProtectionScore: Int = 99, // 99% de garantia contra atrasos
        val destinationCorridor: String = "Av. Paulista / Jardins"
    )

    /**
     * Alterna instantaneamente para o segundo aplicativo e aciona feedback tátil
     */
    fun switchToSecondaryApp(context: Context, targetPackage: String) {
        // 1. Vibração tática de confirmação (dois pulsos curtos e firmes no guidão)
        vibrateTacticalConfirmation(context)

        // 2. Disparo do Intent nativo do Android para o segundo app
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrateTacticalConfirmation(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 50, 120)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }
    }
}

/**
 * Card Flutuante de Rota Dupla Assistida para o Piloto no Guidão
 */
@Composable
fun AssistedDualRouteCard(
    opportunity: AssistedQuickSwitchManager.DualRouteOpportunity,
    onAcceptAndSwitch: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isPaidSubscriber: Boolean = true,
    onUnlockPlan: () -> Unit = {}
) {
    val borderColor = if (isPaidSubscriber) Color(0xFF10B981) else Color(0xFFFFB800)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header do Card: Alerta de Rota Dupla + Selo Anti-Ban
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (isPaidSubscriber) Icons.Default.ElectricBolt else Icons.Default.Lock,
                        contentDescription = "Rota Dupla",
                        tint = borderColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isPaidSubscriber) "ROTA DUPLA DETECTADA!" else "ROTA DUPLA (EXCLUSIVO PRO)",
                        color = borderColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = borderColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "SLA Seguro",
                            tint = if (isPaidSubscriber) Color(0xFF34D399) else Color(0xFFFFB800),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isPaidSubscriber) "SLA ${opportunity.slaProtectionScore}% SEGURO" else "LIBERE NO PLANO",
                            color = if (isPaidSubscriber) Color(0xFF34D399) else Color(0xFFFFB800),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detalhe das Duas Corridas Combinadas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${opportunity.primaryApp} (R$ ${"%.2f".format(opportunity.primaryValue)}) + ${opportunity.secondaryApp} (R$ ${"%.2f".format(opportunity.secondaryValue)})",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Desvio: +${opportunity.extraDeviationMeters}m (+${opportunity.extraMinutes} min) no mesmo trajeto",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                // Valor Total em Destaque
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R$ ${"%.2f".format(opportunity.totalCombinedValue)}",
                        color = borderColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Faturamento Duplo",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Gigante de Ação Rápida sob o Polegar do Piloto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.35f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Pular", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (isPaidSubscriber) {
                    Button(
                        onClick = onAcceptAndSwitch,
                        modifier = Modifier.weight(0.65f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF022C22)
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Aceitar",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ACEITAR & ABRIR APP 2",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onUnlockPlan,
                        modifier = Modifier.weight(0.65f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB800),
                            contentColor = Color(0xFF1F1200)
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Desbloquear",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DESBLOQUEAR (R$ 25)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
