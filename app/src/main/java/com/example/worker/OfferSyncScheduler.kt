package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Utilitário e Agendador central para gerenciar as tarefas do WorkManager
 * responsáveis pela sincronização em segundo plano das ofertas e telemetria.
 */
object OfferSyncScheduler {
    private const val TAG = "OfferSyncScheduler"

    /**
     * Agenda a sincronização periódica das ofertas em segundo plano.
     * O intervalo mínimo permitido pelo Android WorkManager é de 15 minutos.
     */
    fun schedulePeriodicSync(
        context: Context,
        intervalMinutes: Long = 15,
        requiresCharging: Boolean = false
    ) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(requiresCharging)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<OfferSyncWorker>(
                intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("RADAR_OFFER_SYNC")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                OfferSyncWorker.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            Log.i(TAG, "Sincronização periódica WorkManager agendada a cada $intervalMinutes minutos.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao agendar sincronização periódica WorkManager: ${e.message}", e)
        }
    }

    /**
     * Dispara uma sincronização única imediata (OneTimeWorkRequest) sob demanda,
     * útil para atualizações rápidas quando o usuário clica em atualizar ou ao abrir o app.
     */
    fun triggerImmediateSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<OfferSyncWorker>()
                .setConstraints(constraints)
                .addTag("RADAR_OFFER_SYNC_IMMEDIATE")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                OfferSyncWorker.WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )

            Log.i(TAG, "Sincronização imediata OneTimeWorkRequest disparada com sucesso.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao disparar sincronização imediata: ${e.message}", e)
        }
    }

    /**
     * Cancela todas as rotinas de sincronização em segundo plano ativas
     */
    fun cancelPeriodicSync(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(OfferSyncWorker.WORK_NAME_PERIODIC)
            Log.i(TAG, "Sincronização periódica cancelada.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cancelar sincronização periódica: ${e.message}", e)
        }
    }
}
