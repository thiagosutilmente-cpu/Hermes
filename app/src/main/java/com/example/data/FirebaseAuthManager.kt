package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"
    
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseAuth: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        // Observe auth state changes
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            Log.d(TAG, "Auth state changed. Current user: ${firebaseAuth.currentUser?.email ?: "None"}")
        }
    }

    fun getCurrentRiderId(): String {
        return currentUser.value?.uid ?: FirestoreManager.DEFAULT_RIDER_ID
    }

    fun isUserLoggedIn(): Boolean {
        return currentUser.value != null
    }

    fun getCurrentUserEmail(): String {
        return currentUser.value?.email ?: ""
    }

    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("FirebaseAuth not initialized"))
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).awaitTask()
            val user = result.user ?: throw Exception("Login succeeded but user is null")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("FirebaseAuth not initialized"))
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).awaitTask()
            val user = result.user ?: throw Exception("Signup succeeded but user is null")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            auth?.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
    }
}
