package com.example

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.util.Locale

private val NeonEmerald = Color(0xFF00FF88)
private val CyberCyan = Color(0xFF00E5FF)
private val GoldAccent = Color(0xFFFFD700)
private val DarkCardSurface = Color(0xFF10141D)
private val CardBorderColor = Color(0xFF1F2636)
private val TextLight = Color(0xFFF3F4F8)
private val TextMuted = Color(0xFF8A93A6)

@Composable
fun MeiTaxReportCard(
    grossRevenue: Double = 3850.00,
    fuelExpense: Double = 355.00,
    maintenanceExpense: Double = 120.00,
    totalKm: Double = 1420.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statement = remember(grossRevenue, fuelExpense, maintenanceExpense, totalKm) {
        MeiTaxReportExporter.generateStatement(
            grossRevenue = grossRevenue,
            fuelExpenses = fuelExpense,
            maintenanceExpenses = maintenanceExpense,
            totalKm = totalKm,
            driverName = "Thiago Entregador"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_mei_tax_report"),
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
                            .background(GoldAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RELATÓRIO FISCAL & MEI (DASN)",
                                color = TextLight,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "Comprovante de rendimentos para Receita Federal",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Surface(
                    color = GoldAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "LC 123/2006",
                        color = GoldAccent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Box com os 4 Pilares Fiscais
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090D14))
                    .padding(12.dp)
            ) {
                FiscalLineItem("Faturamento Bruto (iFood/99/Uber)", "R$ ${formatCurrency(statement.grossRevenue)}", TextLight)
                Spacer(modifier = Modifier.height(6.dp))
                FiscalLineItem("(-) Combustível & Manutenção", "R$ ${formatCurrency(statement.totalExpenses)}", Color(0xFFFF5252))
                Spacer(modifier = Modifier.height(6.dp))
                FiscalLineItem("(=) Lucro Líquido Real", "R$ ${formatCurrency(statement.netIncome)}", NeonEmerald, isBold = true)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF1E2838))
                )
                Spacer(modifier = Modifier.height(8.dp))
                FiscalLineItem("Parcela Isenta MEI (20% Lei)", "R$ ${formatCurrency(statement.exemptRevenueAmount)}", CyberCyan)
                Spacer(modifier = Modifier.height(6.dp))
                FiscalLineItem("Rendimento Tributável Final", "R$ ${formatCurrency(statement.taxableRevenueAmount)}", GoldAccent, isBold = true)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botão de Exportação e Compartilhamento
            Button(
                onClick = {
                    MeiTaxReportExporter.shareStatement(context, statement)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_export_mei_statement"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color.Black
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "EXPORTAR COMPROVANTE FISCAL (WHATSAPP/PDF)", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun FiscalLineItem(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isBold) TextLight else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = if (isBold) 12.5.sp else 11.5.sp,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format(Locale.GERMANY, "%.2f", amount)
}
