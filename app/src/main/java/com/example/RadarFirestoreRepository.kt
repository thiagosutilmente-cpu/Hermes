package com.example

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RadarFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val PROFILE_DOC_ID = "cockpit"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa entrar com o Google antes de sincronizar na nuvem.")
    }

    fun observeProfile(userId: String): Flow<RiderGoalProfile?> {
        val path = "users/$userId/profiles/$PROFILE_DOC_ID"
        return db.collection("users").document(userId)
            .collection("profiles").document(PROFILE_DOC_ID)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        RiderGoalProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeRecentDeliveries(userId: String): Flow<List<DeliveryLog>> {
        val path = "users/$userId/deliveries"
        return db.collection("users").document(userId)
            .collection("deliveries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DeliveryLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveProfileSettings(
        userId: String,
        dailyGoalBrl: Double,
        minGainPerKm: Double,
        minOfferValue: Double
    ): Result<Unit> {
        val docPath = "users/$userId/profiles/$PROFILE_DOC_ID"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sessão alterada; escrita entre usuários bloqueada." }

            val safeGoal = dailyGoalBrl.coerceIn(0.0, 50000.0)
            val safeMinGain = minGainPerKm.coerceIn(0.0, 500.0)
            val safeMinOffer = minOfferValue.coerceIn(0.0, 5000.0)

            val docRef = db.collection("users").document(uid)
                .collection("profiles").document(PROFILE_DOC_ID)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "dailyGoalBrl" to safeGoal,
                        "minGainPerKm" to safeMinGain,
                        "minOfferValue" to safeMinOffer,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dailyGoalBrl" to safeGoal,
                        "minGainPerKm" to safeMinGain,
                        "minOfferValue" to safeMinOffer,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordDeliveryLog(
        userId: String,
        appSource: String,
        restaurant: String,
        payoutBrl: Double,
        distanceKm: Double,
        merged: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/deliveries"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sessão alterada; escrita entre usuários bloqueada." }

            val cleanAppSource = appSource.trim().ifEmpty { "Radar" }.take(40)
            val cleanRestaurant = restaurant.trim().ifEmpty { "Entrega" }.take(120)
            val safePayout = payoutBrl.coerceIn(0.0, 10000.0)
            val safeDistance = distanceKm.coerceIn(0.0, 1000.0)

            val docRef = db.collection("users").document(uid)
                .collection("deliveries").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "appSource" to cleanAppSource,
                "restaurant" to cleanRestaurant,
                "payoutBrl" to safePayout,
                "distanceKm" to safeDistance,
                "merged" to merged,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
