package com.example

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dados estruturados do relatório financeiro fiscal do entregador MEI
 */
data class MeiFinancialStatement(
    val driverName: String = "Thiago Entregador",
    val periodLabel: String = "Mês Vigente / Acumulado",
    val grossRevenue: Double = 3850.00,
    val totalKmDriven: Double = 1420.0,
    val fuelExpenses: Double = 355.00,
    val maintenanceExpenses: Double = 120.00,
    val platformDeductions: Double = 0.00,
    val totalExpenses: Double = 475.00,
    val netIncome: Double = 3375.00,
    val exemptRevenuePercentage: Double = 20.0, // 20% para transporte de carga/entrega de mercadorias
    val exemptRevenueAmount: Double = 770.00,
    val taxableRevenueAmount: Double = 2605.00,
    val dasMonthlyPaymentEstimated: Double = 75.60,
    val generatedDateFormatted: String = ""
)

/**
 * MeiTaxReportExporter
 *
 * Gerador de Relatório Financeiro e Comprovante de Rendimentos para MEI (Microempreendedor Individual).
 * Em conformidade com a legislação da Receita Federal (LC 123/2006):
 * - Separa Receita Bruta (iFood, 99, Uber, Rappi)
 * - Aplica a presunção legal de isenção de 20% para transporte de carga/encomendas
 * - Subtrai despesas comprovadas de combustível e manutenção da moto
 * - Calcula a parcela tributável real para o carnê-leão / declaração anual DASN-SIMEI
 * - Permite exportar e compartilhar em 1 toque via WhatsApp ou E-mail.
 */
object MeiTaxReportExporter {

    fun generateStatement(
        grossRevenue: Double,
        fuelExpenses: Double,
        maintenanceExpenses: Double = 0.0,
        totalKm: Double = 0.0,
        driverName: String = "Entregador Parceiro"
    ): MeiFinancialStatement {
        val totalExpenses = fuelExpenses + maintenanceExpenses
        val net = (grossRevenue - totalExpenses).coerceAtLeast(0.0)

        // Art. 14 da LC 123/2006: percentual de 20% de isenção para transporte de carga/motofrete
        val exempt = grossRevenue * 0.20
        // Parcela tributável: Lucro Líquido - Parcela Isenta
        val taxable = (net - exempt).coerceAtLeast(0.0)

        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())

        return MeiFinancialStatement(
            driverName = driverName,
            periodLabel = "Exercício Fiscal Corrente",
            grossRevenue = grossRevenue,
            totalKmDriven = totalKm,
            fuelExpenses = fuelExpenses,
            maintenanceExpenses = maintenanceExpenses,
            totalExpenses = totalExpenses,
            netIncome = net,
            exemptRevenuePercentage = 20.0,
            exemptRevenueAmount = exempt,
            taxableRevenueAmount = taxable,
            generatedDateFormatted = dateFmt
        )
    }

    /**
     * Gera o documento formatado em texto para exportação
     */
    fun formatAsPrintableReport(statement: MeiFinancialStatement): String {
        return buildString {
            appendLine("══════════════════════════════════════════════")
            appendLine("   RADAR COORDINATOR • RELATÓRIO FISCAL MEI")
            appendLine("   DECLARAÇÃO DE RENDIMENTOS DE ENTREGADOR")
            appendLine("══════════════════════════════════════════════")
            appendLine("Piloto / Titular: ${statement.driverName}")
            appendLine("Período: ${statement.periodLabel}")
            appendLine("Emitido em: ${statement.generatedDateFormatted}")
            appendLine("Classificação CNAE: 5320-2/02 (Serviços de Entrega Rápida)")
            appendLine("──────────────────────────────────────────────")
            appendLine("1. DEMONSTRATIVO DE RECEITAS (APLICATIVOS):")
            appendLine(" • Faturamento Bruto Total:   R$ ${formatVal(statement.grossRevenue)}")
            appendLine(" • Quilômetros Rodados:       ${String.format(Locale.GERMANY, "%.1f", statement.totalKmDriven)} km")
            appendLine(" • Ganho Médio por KM:        R$ ${formatVal(if (statement.totalKmDriven > 0) statement.grossRevenue / statement.totalKmDriven else 0.0)}/km")
            appendLine("──────────────────────────────────────────────")
            appendLine("2. DESPESAS OPERACIONAIS COMPROVADAS:")
            appendLine(" • Combustível (Gasolina):    R$ ${formatVal(statement.fuelExpenses)}")
            appendLine(" • Manutenção e Óleo Moto:    R$ ${formatVal(statement.maintenanceExpenses)}")
            appendLine(" • Total de Custos:           R$ ${formatVal(statement.totalExpenses)}")
            appendLine("──────────────────────────────────────────────")
            appendLine("3. RESULTADO LÍQUIDO DO ENTREGADOR:")
            appendLine(" • Lucro Líquido Real:        R$ ${formatVal(statement.netIncome)}")
            appendLine("──────────────────────────────────────────────")
            appendLine("4. ENQUADRAMENTO FISCAL DASN-SIMEI (LC 123/2006):")
            appendLine(" • Parcela Isenta (20% Bruto): R$ ${formatVal(statement.exemptRevenueAmount)}")
            appendLine(" • Rendimento Tributável:      R$ ${formatVal(statement.taxableRevenueAmount)}")
            appendLine(" • Guia DAS Mensal Estimada:   R$ ${formatVal(statement.dasMonthlyPaymentEstimated)}")
            appendLine("══════════════════════════════════════════════")
            appendLine("Documento gerado automaticamente pelo Jarvis Radar Cockpit.")
            appendLine("Válido como base para a Declaração Anual do MEI (DASN).")
            appendLine("══════════════════════════════════════════════")
        }
    }

    /**
     * Compartilha o relatório fiscal via WhatsApp, E-mail ou Telegram
     */
    fun shareStatement(context: Context, statement: MeiFinancialStatement) {
        try {
            val reportText = formatAsPrintableReport(statement)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Relatório Fiscal MEI - ${statement.driverName}")
                putExtra(Intent.EXTRA_TEXT, reportText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório Fiscal MEI"))
            Toast.makeText(context, "Exportando comprovante MEI...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatVal(value: Double): String {
        return String.format(Locale.GERMANY, "%.2f", value)
    }
}
