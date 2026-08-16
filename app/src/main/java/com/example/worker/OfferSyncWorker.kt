package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.coordinator.RadarCoordinator
import com.example.data.AppDatabase
import com.example.data.FirebaseAuthManager
import com.example.data.FirestoreManager
import com.example.data.OfferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Worker do WorkManager responsável por sincronizar periodicamente em segundo plano
 * as ofertas das plataformas parceiras integradas (iFood, Rappi, Uber Direct, 99 Food)
 * com o banco de dados Room local e com o Firestore em tempo real.
 */
class OfferSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "OfferSyncWorker"
        const val WORK_NAME_PERIODIC = "PeriodicOfferSyncWork"
        const val WORK_NAME_ONETIME = "OneTimeOfferSyncWork"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "Iniciando ciclo de sincronização de ofertas em segundo plano (WorkManager)...")

        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val offerDao = database.offerDao()
            val riderId = FirebaseAuthManager.getCurrentRiderId()

            // 1. Gera / Coleta ofertas simuladas inteligentes das 4 plataformas integradas
            val simulatedPlatformOffers = generatePlatformCandidateOffers()

            // 2. Persiste as novas ofertas no banco de dados local SQLite (Room)
            offerDao.insertOffers(simulatedPlatformOffers)
            Log.d(TAG, "${simulatedPlatformOffers.size} novas ofertas salvas no Room local.")

            // 3. Obtém ofertas recentes para sincronização consolidada no Firestore
            val recentOffers = offerDao.getRecentOffers(limit = 15)

            // 4. Sincroniza em tempo real com o Firestore (coleção 'offers' e 'pedidos')
            FirestoreManager.syncBatchOffersToFirestore(
                offers = recentOffers,
                riderId = riderId
            )

            // 5. Atualiza o pulso de integridade do sistema no Firestore
            val anomalies = mutableListOf<String>()
            val healthScore = if (FirestoreManager.isFirestoreConnected.value) 98 else 75
            if (!FirestoreManager.isFirestoreConnected.value) {
                anomalies.add("FIRESTORE_SYNC_LATENCY")
            }
            FirestoreManager.uploadSystemPulse(
                score = healthScore,
                anomalies = anomalies,
                riderId = riderId
            )

            val elapsedMs = System.currentTimeMillis() - startTime
            Log.i(TAG, "Sincronização WorkManager concluída com sucesso em ${elapsedMs}ms. (${recentOffers.size} sincronizadas)")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Falha durante a sincronização do WorkManager: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Gera ofertas realistas de mercado brasileiro das 4 plataformas integradas
     */
    private fun generatePlatformCandidateOffers(): List<OfferEntity> {
        val platforms = listOf("iFood", "Rappi", "Uber Direct", "99 Food")
        val pickupLocations = listOf(
            "Burger King - Av. Paulista, 1000",
            "McDonald's - R. Augusta, 1500",
            "Habib's - R. da Consolação, 2200",
            "Pizza Hut - Alameda Santos, 800",
            "Starbucks - Shopping Cidade São Paulo",
            "Madero Container - Av. Brigadeiro Faria Lima, 2000"
        )
        val deliveryLocations = listOf(
            "Rua Bela Cintra, 450 - Consolação",
            "Alameda Campinas, 980 - Jardim Paulista",
            "Rua Haddock Lobo, 1300 - Cerqueira César",
            "Rua Pamplona, 620 - Bela Vista",
            "Av. Nove de Julho, 3500 - Itaim Bibi",
            "Rua Oscar Freire, 1100 - Pinheiros"
        )

        val generatedCount = Random.nextInt(2, 5)
        val now = System.currentTimeMillis()

        return (0 until generatedCount).map { index ->
            val app = platforms[Random.nextInt(platforms.size)]
            val pickup = pickupLocations[Random.nextInt(pickupLocations.size)]
            val delivery = deliveryLocations[Random.nextInt(deliveryLocations.size)]
            val distance = Random.nextDouble(1.8, 6.5)
            val baseRatePerKm = Random.nextDouble(3.8, 7.5)
            val fare = (distance * baseRatePerKm).coerceAtLeast(8.5)
            val timeMinutes = (distance * 3.5) + Random.nextInt(3, 8)
            val gainPerKm = fare / distance

            val (suggestion, reason) = when {
                gainPerKm >= 5.5 -> "aceitar" to "Ganho/KM excelente (R$ ${String.format("%.2f", gainPerKm)}/km)"
                gainPerKm >= 4.0 -> "aceitar" to "Rentabilidade favorável no corredor central"
                gainPerKm >= 3.0 -> "considerar" to "Valor médio viável para encadeamento Ghost"
                else -> "recusar" to "Ganho/KM abaixo do parâmetro configurado"
            }

            OfferEntity(
                appName = app,
                fareValue = Math.round(fare * 100.0) / 100.0,
                pickupAddress = pickup,
                deliveryAddress = delivery,
                totalDistance = Math.round(distance * 10.0) / 10.0,
                totalTime = Math.round(timeMinutes * 10.0) / 10.0,
                detourDistance = 0.0,
                detourTime = 0.0,
                suggestion = suggestion,
                reason = reason,
                timestamp = now - (index * 15000L),
                speedKmhAtDecision = RadarCoordinator.currentSpeedKmh.value,
                isChained = false,
                activeDeliveryDestination = null,
                userAction = null
            )
        }
    }
}
