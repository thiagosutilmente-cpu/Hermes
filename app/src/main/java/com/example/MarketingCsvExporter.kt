package com.example

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resultado da exportação do relatório CSV de marketing e métricas.
 */
data class MarketingExportResult(
    val success: Boolean,
    val file: File? = null,
    val rowCount: Int = 0,
    val summaryProfit: Double = 0.0,
    val summaryKm: Double = 0.0,
    val avgRatePerKm: Double = 0.0,
    val errorMessage: String? = null
)

/**
 * Gerenciador responsável por exportar o histórico de disparos e métricas de conversão
 * para um arquivo CSV local, permitindo análise externa detalhada dos lucros por KM rodado.
 */
object MarketingCsvExporter {

    private const val EXPORT_SUBDIR = "marketing_reports"

    /**
     * Gera o conteúdo CSV completo e salva no armazenamento local do aplicativo.
     * Retorna o arquivo gerado e estatísticas do relatório.
     */
    fun exportToCsv(
        context: Context,
        dispatchLogs: List<MarketingDispatchLog> = MarketingDispatchLogManager.logs.toList(),
        campaignDataPoints: List<CampaignDataPoint> = emptyList()
    ): MarketingExportResult {
        return try {
            val exportDir = File(context.filesDir, EXPORT_SUBDIR).apply {
                if (!exists()) mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "jarvis_marketing_lucros_km_$timestamp.csv"
            val csvFile = File(exportDir, fileName)

            val totalProfitDispatches = dispatchLogs.sumOf { it.profitBrl }
            val totalKmDispatches = dispatchLogs.sumOf { it.distanceKm }
            val avgRate = if (totalKmDispatches > 0) totalProfitDispatches / totalKmDispatches else 0.0

            FileWriter(csvFile).use { writer ->
                // Escreve BOM UTF-8 para garantir abertura correta no Microsoft Excel / Google Sheets
                writer.write("\uFEFF")

                // CABEÇALHO DE METADADOS DO RELATÓRIO
                writer.appendLine("# RELATÓRIO DE TELEMETRIA E MARKETING AUTOMATIZADO - JARVIS PRO")
                writer.appendLine("# Data de Geração;${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())}")
                writer.appendLine("# Total de Disparos Registrados;${dispatchLogs.size}")
                writer.appendLine("# Lucro Consolidado Disparos (R$);${String.format(Locale("pt", "BR"), "%.2f", totalProfitDispatches)}")
                writer.appendLine("# KM Consolidado Disparos;${String.format(Locale("pt", "BR"), "%.2f", totalKmDispatches)}")
                writer.appendLine("# Taxa Média Ponderada (R$/KM);${String.format(Locale("pt", "BR"), "%.2f", avgRate)}")
                writer.appendLine("")

                // SEÇÃO 1: HISTÓRICO DE DISPAROS E CENÁRIOS DE GANHOS
                writer.appendLine("# SEÇÃO 1: HISTÓRICO DETALHADO DE DISPAROS REALIZADOS")
                writer.appendLine("ID;Data_Hora;Cenário;Lucro_Liquido_BRL;Distancia_KM;Taxa_BRL_por_KM;Economia_Combustivel_BRL;Status_Entrega_Push;Canal_Notificacao;Titulo_Push;Mensagem_Notificacao")

                for (log in dispatchLogs) {
                    val rate = if (log.distanceKm > 0) log.profitBrl / log.distanceKm else 0.0
                    writer.appendLine(
                        listOf(
                            escapeCsv(log.id),
                            escapeCsv(log.timestampFormatted),
                            escapeCsv(log.scenarioName),
                            String.format(Locale("pt", "BR"), "%.2f", log.profitBrl),
                            String.format(Locale("pt", "BR"), "%.2f", log.distanceKm),
                            String.format(Locale("pt", "BR"), "%.2f", rate),
                            String.format(Locale("pt", "BR"), "%.2f", log.fuelSavedBrl),
                            escapeCsv(log.deliveryStatus),
                            escapeCsv(log.channelId),
                            escapeCsv(log.notificationTitle),
                            escapeCsv(log.notificationBody)
                        ).joinToString(";")
                    )
                }

                // SEÇÃO 2: MÉTRICAS DE CONVERSÃO DAS CAMPANHAS (RECHARTS DATA)
                if (campaignDataPoints.isNotEmpty()) {
                    writer.appendLine("")
                    writer.appendLine("# SEÇÃO 2: MÉTRICAS DE CONVERSÃO DO FUNIL (DADOS DOS GRÁFICOS RECHARTS)")
                    writer.appendLine("Campanha_Periodo;Lucro_Liquido_BRL;Distancia_KM;Taxa_BRL_por_KM;Assinantes_Convertidos")
                    for (cp in campaignDataPoints) {
                        writer.appendLine(
                            listOf(
                                escapeCsv(cp.label),
                                String.format(Locale("pt", "BR"), "%.2f", cp.profitBrl),
                                String.format(Locale("pt", "BR"), "%.2f", cp.distanceKm),
                                String.format(Locale("pt", "BR"), "%.2f", cp.ratePerKm),
                                cp.conversionsCount.toString()
                            ).joinToString(";")
                        )
                    }
                }
            }

            MarketingExportResult(
                success = true,
                file = csvFile,
                rowCount = dispatchLogs.size + campaignDataPoints.size,
                summaryProfit = totalProfitDispatches,
                summaryKm = totalKmDispatches,
                avgRatePerKm = avgRate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MarketingExportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Erro desconhecido ao gerar arquivo CSV"
            )
        }
    }

    /**
     * Cria uma Intent de compartilhamento ou abertura do arquivo CSV exportado via FileProvider.
     */
    fun createShareIntent(context: Context, csvFile: File): Intent {
        val authority = "${context.packageName}.provider"
        val contentUri = FileProvider.getUriForFile(context, authority, csvFile)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório de Telemetria de Lucros por KM - Jarvis Marketing")
            putExtra(Intent.EXTRA_TEXT, "Segue anexo o relatório CSV exportado do MarketingDashboard contendo o histórico de disparos e métricas de conversão de lucros por KM rodado.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Trata caracteres especiais e delimitadores de CSV (ponto-e-vírgula e aspas).
     */
    private fun escapeCsv(value: String): String {
        var clean = value.replace("\r", " ").replace("\n", " ")
        if (clean.contains(";") || clean.contains("\"")) {
            clean = "\"" + clean.replace("\"", "\"\"") + "\""
        }
        return clean
    }
}
