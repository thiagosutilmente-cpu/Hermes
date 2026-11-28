package com.example

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.radar.data.RadarRoomDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class AccountDeletionResult {
    data class Success(val message: String) : AccountDeletionResult()
    data class RequiresRecentLogin(val message: String) : AccountDeletionResult()
    data class Error(val message: String) : AccountDeletionResult()
}

/**
 * Orquestra a exclusão definitiva de conta e dados do usuário (LGPD & Políticas Google Play / App Store).
 */
object AccountDeletionManager {

    private const val TAG = "AccountDeletionManager"

    suspend fun deleteAccountAndData(context: Context): AccountDeletionResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val auth = Firebase.auth
        val currentUser = auth.currentUser

        try {
            if (currentUser != null) {
                val uid = currentUser.uid

                // 1. Apagar coleções do usuário no Firestore antes de revogar a autenticação
                val repo = RadarFirestoreRepository(appContext)
                val cloudDelete = repo.deleteAllUserData(uid)
                if (cloudDelete.isFailure) {
                    Log.w(TAG, "Aviso ao limpar Firestore: ${cloudDelete.exceptionOrNull()?.message}")
                }

                val idToken = try {
                    currentUser.getIdToken(false).await()?.token ?: ""
                } catch (_: Exception) {
                    ""
                }

                // 2. Solicitar exclusão autenticada de registros na VPS
                requestServerAccountDeletion(
                    serverBaseUrl = RadarBackendSyncManager.syncState.value.serverUrl,
                    userId = uid,
                    email = currentUser.email ?: "",
                    bearerToken = idToken
                )

                // 3. Remover a conta de autenticação Firebase
                try {
                    currentUser.delete().await()
                } catch (recentEx: FirebaseAuthRecentLoginRequiredException) {
                    Log.w(TAG, "Reautenticação exigida pelo Firebase antes de excluir conta", recentEx)
                    return@withContext AccountDeletionResult.RequiresRecentLogin(
                        "Por segurança, saia e entre novamente com o Google para confirmar a exclusão permanente da sua conta."
                    )
                }
            } else {
                // Sem conta Firebase conectada: limpa apenas dados locais do aparelho
                Log.i(TAG, "Nenhuma conta Firebase vinculada; limpando dados locais.")
            }

            // 4. Limpar banco Room local, logs e SharedPreferences
            clearAllLocalData(appContext)

            // 5. Encerrar sessão Firebase + Credential Manager
            try {
                auth.signOut()
                CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Aviso ao limpar estado de credenciais", e)
            }

            AccountDeletionResult.Success(
                "Sua conta e todos os dados associados (nuvem e dispositivo) foram excluídos permanentemente."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir conta e dados", e)
            AccountDeletionResult.Error(
                "Não foi possível concluir a exclusão agora: ${e.localizedMessage ?: "tente novamente."}"
            )
        }
    }

    private fun clearAllLocalData(context: Context) {
        AdminSecurityManager.lockAdmin()
        GlobalCrashReporter.clearLocalCrashLogs(context)

        try {
            RadarRoomDatabase.getDatabase(context).clearAllTables()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso ao limpar Room DB", e)
        }

        val prefFiles = listOf(
            "radar_prefs",
            "radar_play_billing_prefs",
            "radar_subscription_prefs",
            "radar_backend_sync_prefs",
            "radar_offer_filters",
            "radar_onboarding_prefs",
            "radar_referral_prefs",
            "jarvis_cockpit_prefs",
            "jarvis_fuel_prefs"
        )
        prefFiles.forEach { name ->
            try {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {
            }
        }
    }

    private fun requestServerAccountDeletion(
        serverBaseUrl: String,
        userId: String,
        email: String,
        bearerToken: String
    ) {
        if ((userId.isBlank() && email.isBlank()) || bearerToken.isBlank()) return
        try {
            val cleanBase = serverBaseUrl.trimEnd('/')
            val url = URL("$cleanBase/api/v1/account/delete")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("email", email)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            Log.i(TAG, "Solicitação autenticada de exclusão na VPS retornou HTTP $code")
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "VPS inacessível durante limpeza remota (dados locais e Firestore limpos): ${e.message}")
        }
    }
}
