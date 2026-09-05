package com.example.radar.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * ENTIDADE ROOM: OFERTAS DE CORRIDAS (CACHE OFFLINE)
 * Armazena ofertas recebidas pelo Radar AI para consulta imediata mesmo sem sinal 4G/5G.
 */
@Entity(tableName = "cached_radar_offers")
data class CachedOfferEntity(
    @PrimaryKey val id: String,
    val appName: String,
    val restaurant: String,
    val value: Double,
    val distanceKm: Double,
    val timeMinutes: Int,
    val gainPerKm: Double,
    val pickupAddress: String,
    val deliveryAddress: String,
    val neuralDecision: String = "ACCEPT", // ACCEPT, DECLINE, MANUAL
    val neuralReason: String = "",
    val confidence: Double = 0.95,
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, COMPLETED
    val fuelCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ENTIDADE ROOM: ROTAS DE ENTREGA (CACHE OFFLINE)
 * Armazena as rotas e paradas de entregas concluídas e ativas para navegação offline.
 */
@Entity(tableName = "cached_delivery_routes")
data class CachedRouteEntity(
    @PrimaryKey val routeId: String,
    val offerId: String,
    val appName: String,
    val originName: String,
    val originAddress: String,
    val destinationName: String,
    val destinationAddress: String,
    val totalDistanceKm: Double,
    val estimatedMinutes: Int,
    val waypointsSummary: String, // Ex: "● Coleta: BK Paulista ➔ ● Entrega: R. Augusta 1508"
    val completedAt: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED" // PLANNED, IN_PROGRESS, COMPLETED
)

/**
 * DAO ROOM: ACESSO A OFERTAS CACHEADAS
 */
@Dao
interface CachedOfferDao {
    @Query("SELECT * FROM cached_radar_offers ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentOffers(limit: Int = 50): Flow<List<CachedOfferEntity>>

    @Query("SELECT * FROM cached_radar_offers WHERE status = 'ACCEPTED' OR status = 'COMPLETED' ORDER BY timestamp DESC")
    fun getAcceptedHistory(): Flow<List<CachedOfferEntity>>

    @Query("SELECT * FROM cached_radar_offers WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingOffers(): Flow<List<CachedOfferEntity>>

    @Query("SELECT COUNT(*) FROM cached_radar_offers")
    suspend fun getOfferCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: CachedOfferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<CachedOfferEntity>)

    @Query("UPDATE cached_radar_offers SET status = :newStatus WHERE id = :offerId")
    suspend fun updateOfferStatus(offerId: String, newStatus: String)

    @Query("DELETE FROM cached_radar_offers WHERE id = :offerId")
    suspend fun deleteOfferById(offerId: String)

    @Query("DELETE FROM cached_radar_offers")
    suspend fun clearAllOffers()
}

/**
 * DAO ROOM: ACESSO A ROTAS CACHEADAS
 */
@Dao
interface CachedRouteDao {
    @Query("SELECT * FROM cached_delivery_routes ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentRoutes(limit: Int = 40): Flow<List<CachedRouteEntity>>

    @Query("SELECT * FROM cached_delivery_routes WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveRoute(): Flow<CachedRouteEntity?>

    @Query("SELECT COUNT(*) FROM cached_delivery_routes")
    suspend fun getRouteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: CachedRouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<CachedRouteEntity>)

    @Query("UPDATE cached_delivery_routes SET status = :newStatus WHERE routeId = :routeId")
    suspend fun updateRouteStatus(routeId: String, newStatus: String)

    @Query("DELETE FROM cached_delivery_routes WHERE completedAt < :threshold")
    suspend fun clearOldRoutes(threshold: Long)

    @Query("DELETE FROM cached_delivery_routes")
    suspend fun clearAllRoutes()
}

/**
 * BANCO DE DADOS LOCAL ROOM
 */
@Database(
    entities = [CachedOfferEntity::class, CachedRouteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RadarRoomDatabase : RoomDatabase() {
    abstract fun cachedOfferDao(): CachedOfferDao
    abstract fun cachedRouteDao(): CachedRouteDao

    companion object {
        @Volatile
        private var INSTANCE: RadarRoomDatabase? = null

        fun getDatabase(context: Context): RadarRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadarRoomDatabase::class.java,
                    "radar_offline_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
