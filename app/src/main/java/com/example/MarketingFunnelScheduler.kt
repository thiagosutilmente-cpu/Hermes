package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Agendador oficial de WorkManager para o Funil de Vendas Automatizado do Jarvis.
 * Gerencia a programação das mensagens de Dia 1, Dia 3 e o Fechamento Agressivo do Dia 7.
 */
object MarketingFunnelScheduler {

    private const val PREFS_NAME = "jarvis_funnel_scheduler_prefs"
    private const val KEY_SCHEDULED_AT = "funnel_scheduled_at"
    private const val KEY_LAST_PHONE = "funnel_last_phone"
    private const val KEY_IS_ACCELERATED = "funnel_is_accelerated"

    private const val WORK_TAG_FUNNEL = "tag_jarvis_marketing_funnel"
    private const val WORK_NAME_DAY_1 = "work_funnel_stage_day_1"
    private const val WORK_NAME_DAY_3 = "work_funnel_stage_day_3"
    private const val WORK_NAME_DAY_7 = "work_funnel_stage_day_7"

    data class FunnelStageInfo(
        val stageKey: String,
        val title: String,
        val targetDelayDescription: String,
        val isDispatched: Boolean,
        val dispatchedAtFormatted: String? = null
    )

    /**
     * Agenda os 3 estágios oficiais do funil via WorkManager
     *
     * @param isAccelerated Se true, agenda para segundos (5s, 20s, 40s) para simulação e teste imediato.
     *                      Se false, agenda para o cronograma real (1h, 3 dias, 7 dias).
     */
    fun scheduleFull7DayFunnel(
        context: Context,
        phone: String,
        userName: String = "Piloto Jarvis",
        isAccelerated: Boolean = false
    ) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val cleanPhone = phone.ifBlank { "5511999999999" }

        // =====================================================================
        // 1. ESTÁGIO DIA 1 (Boas-vindas & Onboarding)
        // =====================================================================
        val day1Delay = if (isAccelerated) 5L to TimeUnit.SECONDS else 1L to TimeUnit.HOURS
        val day1Work = OneTimeWorkRequestBuilder<MarketingFunnelWorker>()
            .setConstraints(constraints)
            .setInitialDelay(day1Delay.first, day1Delay.second)
            .addTag(WORK_TAG_FUNNEL)
            .setInputData(
                workDataOf(
                    MarketingFunnelWorker.KEY_STAGE to MarketingFunnelWorker.STAGE_DAY_1_WELCOME,
                    MarketingFunnelWorker.KEY_PHONE to cleanPhone,
                    MarketingFunnelWorker.KEY_USER_NAME to userName,
                    MarketingFunnelWorker.KEY_DRY_RUN to isAccelerated
                )
            )
            .build()

        workManager.enqueueUniqueWork(WORK_NAME_DAY_1, ExistingWorkPolicy.REPLACE, day1Work)

        // =====================================================================
        // 2. ESTÁGIO DIA 3 (Engajamento & Dica de Rota Dupla)
        // =====================================================================
        val day3Delay = if (isAccelerated) 20L to TimeUnit.SECONDS else 3L to TimeUnit.DAYS
        val day3Work = OneTimeWorkRequestBuilder<MarketingFunnelWorker>()
            .setConstraints(constraints)
            .setInitialDelay(day3Delay.first, day3Delay.second)
            .addTag(WORK_TAG_FUNNEL)
            .setInputData(
                workDataOf(
                    MarketingFunnelWorker.KEY_STAGE to MarketingFunnelWorker.STAGE_DAY_3_ENGAGEMENT,
                    MarketingFunnelWorker.KEY_PHONE to cleanPhone,
                    MarketingFunnelWorker.KEY_USER_NAME to userName,
                    MarketingFunnelWorker.KEY_DRY_RUN to isAccelerated
                )
            )
            .build()

        workManager.enqueueUniqueWork(WORK_NAME_DAY_3, ExistingWorkPolicy.REPLACE, day3Work)

        // =====================================================================
        // 3. ESTÁGIO DIA 7 (O Fechamento Agressivo: Conversão Plano Anual R$ 650)
        // =====================================================================
        val day7Delay = if (isAccelerated) 40L to TimeUnit.SECONDS else 7L to TimeUnit.DAYS
        val day7Work = OneTimeWorkRequestBuilder<MarketingFunnelWorker>()
            .setConstraints(constraints)
            .setInitialDelay(day7Delay.first, day7Delay.second)
            .addTag(WORK_TAG_FUNNEL)
            .setInputData(
                workDataOf(
                    MarketingFunnelWorker.KEY_STAGE to MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE,
                    MarketingFunnelWorker.KEY_PHONE to cleanPhone,
                    MarketingFunnelWorker.KEY_USER_NAME to userName,
                    MarketingFunnelWorker.KEY_DRY_RUN to isAccelerated
                )
            )
            .build()

        workManager.enqueueUniqueWork(WORK_NAME_DAY_7, ExistingWorkPolicy.REPLACE, day7Work)

        // Salva metadados do agendamento
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_SCHEDULED_AT, System.currentTimeMillis())
            .putString(KEY_LAST_PHONE, cleanPhone)
            .putBoolean(KEY_IS_ACCELERATED, isAccelerated)
            .apply()
    }

    /**
     * Executa imediatamente um estágio para testes em tempo real
     */
    fun triggerStageImmediately(
        context: Context,
        stage: String,
        phone: String = "5511999999999",
        userName: String = "Piloto Jarvis"
    ) {
        val workManager = WorkManager.getInstance(context)
        val work = OneTimeWorkRequestBuilder<MarketingFunnelWorker>()
            .addTag(WORK_TAG_FUNNEL)
            .setInputData(
                workDataOf(
                    MarketingFunnelWorker.KEY_STAGE to stage,
                    MarketingFunnelWorker.KEY_PHONE to phone,
                    MarketingFunnelWorker.KEY_USER_NAME to userName,
                    MarketingFunnelWorker.KEY_DRY_RUN to true
                )
            )
            .build()

        workManager.enqueue(work)
    }

    /**
     * Cancela todas as tarefas de disparo de marketing agendadas
     */
    fun cancelAllFunnelWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(WORK_TAG_FUNNEL)
        workManager.cancelUniqueWork(WORK_NAME_DAY_1)
        workManager.cancelUniqueWork(WORK_NAME_DAY_3)
        workManager.cancelUniqueWork(WORK_NAME_DAY_7)
    }

    /**
     * Registra quando um estágio foi executado com sucesso pelo Worker
     */
    fun recordStageDispatched(context: Context, stage: String, phone: String, success: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowFormatted = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        prefs.edit()
            .putBoolean("dispatched_$stage", success)
            .putString("dispatched_at_$stage", nowFormatted)
            .apply()
    }

    /**
     * Retorna a lista com o status dos estágios para exibição na UI
     */
    fun getFunnelStages(context: Context): List<FunnelStageInfo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAccelerated = prefs.getBoolean(KEY_IS_ACCELERATED, false)

        return listOf(
            FunnelStageInfo(
                stageKey = MarketingFunnelWorker.STAGE_DAY_1_WELCOME,
                title = "Dia 1 • Boas-Vindas & Ativação",
                targetDelayDescription = if (isAccelerated) "5 segundos (Simulação)" else "+1 hora após o cadastro",
                isDispatched = prefs.getBoolean("dispatched_${MarketingFunnelWorker.STAGE_DAY_1_WELCOME}", false),
                dispatchedAtFormatted = prefs.getString("dispatched_at_${MarketingFunnelWorker.STAGE_DAY_1_WELCOME}", null)
            ),
            FunnelStageInfo(
                stageKey = MarketingFunnelWorker.STAGE_DAY_3_ENGAGEMENT,
                title = "Dia 3 • Economia de Gasolina & Rota Dupla",
                targetDelayDescription = if (isAccelerated) "20 segundos (Simulação)" else "+3 dias após o cadastro",
                isDispatched = prefs.getBoolean("dispatched_${MarketingFunnelWorker.STAGE_DAY_3_ENGAGEMENT}", false),
                dispatchedAtFormatted = prefs.getString("dispatched_at_${MarketingFunnelWorker.STAGE_DAY_3_ENGAGEMENT}", null)
            ),
            FunnelStageInfo(
                stageKey = MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE,
                title = "Dia 7 • Fechamento Plano Anual R$ 650",
                targetDelayDescription = if (isAccelerated) "40 segundos (Simulação)" else "+7 dias (Fim do Trial)",
                isDispatched = prefs.getBoolean("dispatched_${MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE}", false),
                dispatchedAtFormatted = prefs.getString("dispatched_at_${MarketingFunnelWorker.STAGE_DAY_7_ANNUAL_CLOSE}", null)
            )
        )
    }

    fun isFunnelScheduled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_SCHEDULED_AT, 0L) > 0L
    }

    fun getLastScheduledPhone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_PHONE, "5511999999999") ?: "5511999999999"
    }
}
