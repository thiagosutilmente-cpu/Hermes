package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NeuralClickDao {
    @Query("SELECT * FROM neural_clicks WHERE packageName = :packageName AND keyword = :keyword LIMIT 1")
    suspend fun getLearning(packageName: String, keyword: String): NeuralClickEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(learning: NeuralClickEntity)

    @Update
    suspend fun update(learning: NeuralClickEntity)

    @Query("SELECT * FROM neural_clicks ORDER BY timestamp DESC")
    suspend fun getAllLearnings(): List<NeuralClickEntity>
}
