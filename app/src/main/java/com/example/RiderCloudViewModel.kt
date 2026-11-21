package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RiderCloudViewModel(
    private val repository: RadarFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    val profileState: StateFlow<CloudUiState<RiderGoalProfile?>> =
        repository.observeProfile(currentUserId)
            .map<RiderGoalProfile?, CloudUiState<RiderGoalProfile?>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Erro ao observar perfil em nuvem", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Falha ao sincronizar perfil"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val deliveriesState: StateFlow<CloudUiState<List<DeliveryLog>>> =
        repository.observeRecentDeliveries(currentUserId)
            .map<List<DeliveryLog>, CloudUiState<List<DeliveryLog>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Erro ao observar entregas em nuvem", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Falha ao sincronizar entregas"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    fun saveFiltersToCloud(
        dailyGoalBrl: Double,
        minGainPerKm: Double,
        minOfferValue: Double
    ) {
        viewModelScope.launch {
            val result = repository.saveProfileSettings(
                userId = currentUserId,
                dailyGoalBrl = dailyGoalBrl,
                minGainPerKm = minGainPerKm,
                minOfferValue = minOfferValue
            )
            _syncStatusMessage.value = if (result.isSuccess) {
                "Filtros sincronizados na nuvem"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Erro ao salvar filtros na nuvem"
            }
        }
    }

    fun syncAcceptedDelivery(
        appSource: String,
        restaurant: String,
        payoutBrl: Double,
        distanceKm: Double,
        merged: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.recordDeliveryLog(
                userId = currentUserId,
                appSource = appSource,
                restaurant = restaurant,
                payoutBrl = payoutBrl,
                distanceKm = distanceKm,
                merged = merged
            )
            if (result.isFailure) {
                _syncStatusMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Erro ao registrar corrida na nuvem"
            }
        }
    }

    fun clearStatusMessage() {
        _syncStatusMessage.value = null
    }

    private companion object {
        const val TAG = "RiderCloudVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
