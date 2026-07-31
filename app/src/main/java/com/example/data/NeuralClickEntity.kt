package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neural_clicks")
data class NeuralClickEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val keyword: String,
    val semanticKey: String, // Fingerprint do nó
    val lastX: Float,
    val lastY: Float,
    val usageCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
