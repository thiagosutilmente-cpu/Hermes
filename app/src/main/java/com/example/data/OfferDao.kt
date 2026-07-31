package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Query("SELECT * FROM offers ORDER BY timestamp DESC")
    fun getAllOffers(): Flow<List<OfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: OfferEntity): Long

    @Query("UPDATE offers SET userAction = :action WHERE id = (SELECT id FROM offers ORDER BY timestamp DESC LIMIT 1)")
    suspend fun updateLatestUserAction(action: String)

    @Query("SELECT AVG(fareValue/totalDistance) FROM offers WHERE userAction = 'RECUSADO' AND totalDistance > 0")
    suspend fun getAverageRejectedValuePerKm(): Float?

    @Query("SELECT AVG(fareValue) FROM offers WHERE userAction = 'RECUSADO'")
    suspend fun getAverageRejectedFare(): Float?

    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun deleteOffer(id: Int)

    @Query("DELETE FROM offers")
    suspend fun clearHistory()
}
