package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para operações de persistência e cache offline da Ghost Sequence.
 */
@Dao
interface GhostRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: GhostRouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<GhostStopEntity>)

    @Transaction
    suspend fun insertFullRoute(route: GhostRouteEntity, stops: List<GhostStopEntity>) {
        insertRoute(route)
        insertStops(stops)
    }

    @Query("SELECT * FROM ghost_routes WHERE isRouteActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    fun getActiveRouteFlow(): Flow<GhostRouteEntity?>

    @Query("SELECT * FROM ghost_routes WHERE isRouteActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveRouteDirect(): GhostRouteEntity?

    @Query("SELECT * FROM ghost_routes WHERE batchId = :batchId LIMIT 1")
    suspend fun getRouteById(batchId: String): GhostRouteEntity?

    @Query("SELECT * FROM ghost_stops WHERE batchId = :batchId ORDER BY sequenceNumber ASC")
    fun getStopsForBatchFlow(batchId: String): Flow<List<GhostStopEntity>>

    @Query("SELECT * FROM ghost_stops WHERE batchId = :batchId ORDER BY sequenceNumber ASC")
    suspend fun getStopsForBatchDirect(batchId: String): List<GhostStopEntity>

    @Query("SELECT * FROM ghost_routes ORDER BY updatedAt DESC LIMIT 15")
    fun getAllCachedRoutesFlow(): Flow<List<GhostRouteEntity>>

    @Query("UPDATE ghost_routes SET currentStopIndex = :stopIndex, completedStopsCount = :completedCount, isRouteActive = :isActive, updatedAt = :updatedAt WHERE batchId = :batchId")
    suspend fun updateRouteProgress(batchId: String, stopIndex: Int, completedCount: Int, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ghost_stops SET status = :status WHERE id = :stopId")
    suspend fun updateStopStatus(stopId: String, status: String)

    @Query("DELETE FROM ghost_routes WHERE batchId = :batchId")
    suspend fun deleteRouteById(batchId: String)

    @Query("DELETE FROM ghost_stops WHERE batchId = :batchId")
    suspend fun deleteStopsByBatchId(batchId: String)

    @Transaction
    suspend fun deleteFullRoute(batchId: String) {
        deleteStopsByBatchId(batchId)
        deleteRouteById(batchId)
    }
}
