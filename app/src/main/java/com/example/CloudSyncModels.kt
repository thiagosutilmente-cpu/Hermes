package com.example

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class RiderGoalProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dailyGoalBrl: Double = 300.0,
    val minGainPerKm: Double = 4.5,
    val minOfferValue: Double = 15.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appSource: String = "",
    val restaurant: String = "",
    val payoutBrl: Double = 0.0,
    val distanceKm: Double = 0.0,
    val merged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}
