package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val fareValue: Double,
    val pickupAddress: String,
    val deliveryAddress: String,
    val totalDistance: Double,
    val totalTime: Double,
    val detourDistance: Double = 0.0,
    val detourTime: Double = 0.0,
    val suggestion: String, // aceitar, considerar, recusar
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val speedKmhAtDecision: Float,
    val isChained: Boolean = false,
    val activeDeliveryDestination: String? = null
)
