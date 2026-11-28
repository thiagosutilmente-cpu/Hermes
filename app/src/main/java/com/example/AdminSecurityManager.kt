package com.example

import android.content.Context
import android.util.Log
import com.example.network.AdminChangePinRequest
import com.example.network.AdminLoginRequest
import com.example.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * AdminSecurityManager
 *
 * Gerencia a autenticação administrativa baseada em sessão efêmera na memória
 * validada exclusivamente pelo servidor VPS (/api/admin/login).
 * Nenhuma senha, PIN padrão ou chave mestra é embutida ou persistida no APK.
 */
object AdminSecurityManager {

    private const val TAG = "AdminSecurityManager"

    // Token de sessão mantido exclusivamente em memória enquanto o painel estiver desbloqueado
    @Volatile
    private var sessionToken: String? = null

    // Estado reativo em memória da sessão administrativa
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    // Contador de toques no rodapé para exibir prompt de login administrativo
    private var developerTapCount = 0
    private var lastTapTimestamp = 0L

    fun currentToken(): String? {
        return sessionToken
    }

    fun getAdminToken(@Suppress("UNUSED_PARAMETER") context: Context): String? {
        return sessionToken
    }

    fun getAuthorizationHeader(context: Context): String {
        val token = getAdminToken(context)
        return if (!token.isNullOrBlank()) "Bearer $token" else ""
    }

    /**
     * Autenticação administrativa estritamente via servidor VPS (/api/admin/login).
     * Não realiza validação local nem aceita credenciais fixas no cliente.
     */
    suspend fun authenticateWithServer(
        @Suppress("UNUSED_PARAMETER") context: Context,
        enteredPin: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanPin = enteredPin.trim()
        if (cleanPin.isEmpty()) return@withContext false

        val apiService = NetworkModule.provideApiService()
        try {
            val response = apiService.adminLogin(
                AdminLoginRequest(pin = cleanPin)
            )

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                if (data.success && !data.adminToken.isNullOrBlank()) {
                    sessionToken = data.adminToken
                    _isAdminUnlocked.value = true
                    developerTapCount = 0
                    Log.i(TAG, "Autenticação administrativa autorizada pelo servidor VPS.")
                    return@withContext true
                }
            }
            Log.w(TAG, "Falha de autenticação administrativa: HTTP ${response.code()}")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Servidor indisponível para autenticação administrativa: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Bloqueia o modo administrador imediatamente e revoga o token de sessão em memória.
     */
    fun lockAdmin() {
        sessionToken = null
        _isAdminUnlocked.value = false
    }

    /**
     * Altera o PIN administrativo diretamente no servidor VPS (/api/admin/change-pin).
     * Exige sessão administrativa autenticada com token válido em memória.
     */
    suspend fun changePinOnServer(
        context: Context,
        currentPin: String,
        newPin: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanNew = newPin.trim()
        val authHeader = getAuthorizationHeader(context)
        if (cleanNew.length < 6 || authHeader.isBlank()) return@withContext false

        val apiService = NetworkModule.provideApiService()
        try {
            val response = apiService.changeAdminPin(
                authHeader = authHeader,
                request = AdminChangePinRequest(currentPin = currentPin.trim(), newPin = cleanNew)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Log.i(TAG, "PIN/senha de administrador atualizado com sucesso no servidor VPS.")
                return@withContext true
            }
            Log.e(TAG, "Erro ao alterar credencial administrativa: HTTP ${response.code()}")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Erro de rede ao alterar credencial no servidor: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Registra toque no rodapé de versão.
     * Retorna true se atingiu o limiar de 5 toques contínuos.
     */
    fun registerDeveloperTap(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTapTimestamp > 2500L) {
            developerTapCount = 1
        } else {
            developerTapCount++
        }
        lastTapTimestamp = now

        if (developerTapCount >= 5) {
            developerTapCount = 0
            return true
        }
        return false
    }

    /**
     * Retorna quantos toques faltam para abrir o prompt administrativo.
     */
    fun getRemainingTaps(): Int {
        return (5 - developerTapCount).coerceAtLeast(0)
    }
}
