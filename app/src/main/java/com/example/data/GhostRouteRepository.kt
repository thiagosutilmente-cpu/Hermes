package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ActiveMergedRouteState
import com.example.model.MergedDeliveryStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repositório para gerenciamento e cache local offline de Rotas Mescladas da Ghost Sequence no Room SQLite.
 */
class GhostRouteRepository(private val ghostRouteDao: GhostRouteDao) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "GhostRouteRepository"

        @Volatile
        private var INSTANCE: GhostRouteRepository? = null

        fun getInstance(context: Context): GhostRouteRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val instance = GhostRouteRepository(db.ghostRouteDao())
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Salva ou atualiza a rota completa e suas paradas no banco de dados local Room
     */
    suspend fun saveActiveRoute(route: ActiveMergedRouteState) = withContext(Dispatchers.IO) {
        try {
            val routeEntity = route.toEntity()
            val stopEntities = route.stops.map { it.toEntity(route.batchId) }
            ghostRouteDao.insertFullRoute(routeEntity, stopEntities)
            Log.d(TAG, "Rota Ghost salva localmente no Room: ${route.batchId} (${stopEntities.size} paradas)")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar rota Ghost no Room: ${e.message}", e)
        }
    }

    /**
     * Atualiza o progresso da rota e status da parada atual
     */
    suspend fun updateRouteProgress(
        batchId: String,
        currentStopIndex: Int,
        completedStopsCount: Int,
        isRouteActive: Boolean,
        updatedStop: MergedDeliveryStop? = null
    ) = withContext(Dispatchers.IO) {
        try {
            ghostRouteDao.updateRouteProgress(
                batchId = batchId,
                stopIndex = currentStopIndex,
                completedCount = completedStopsCount,
                isActive = isRouteActive
            )
            updatedStop?.let { stop ->
                ghostRouteDao.updateStopStatus(stop.id, stop.status.name)
            }
            Log.d(TAG, "Progresso da rota $batchId atualizado no Room: etapa $currentStopIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar progresso no Room: ${e.message}", e)
        }
    }

    /**
     * Recupera a rota ativa diretamente do Room para inicialização offline
     */
    suspend fun getActiveRouteDirect(): ActiveMergedRouteState? = withContext(Dispatchers.IO) {
        try {
            val routeEntity = ghostRouteDao.getActiveRouteDirect() ?: return@withContext null
            val stopEntities = ghostRouteDao.getStopsForBatchDirect(routeEntity.batchId)
            val stops = stopEntities.map { it.toDomainModel() }
            routeEntity.toDomainModel(stops)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler rota ativa do Room: ${e.message}", e)
            null
        }
    }

    /**
     * Observa a lista de rotas salvas em cache local
     */
    fun getAllCachedRoutesFlow(): Flow<List<GhostRouteEntity>> {
        return ghostRouteDao.getAllCachedRoutesFlow()
    }
}
