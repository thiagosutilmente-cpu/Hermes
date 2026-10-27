package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdminSecurityManager
 *
 * Gerencia a autenticação, proteção por PIN e desbloqueio do Modo Desenvolvedor / Administrador.
 * Garante que dados confidenciais (VPS, Webhooks Asaas, métricas internas do dono)
 * permaneçam estritamente invisíveis aos motoristas comuns.
 */
object AdminSecurityManager {

    private const val PREFS_NAME = "radar_admin_security_prefs"
    private const val KEY_ADMIN_PIN = "admin_secret_pin"
    private const val DEFAULT_PIN = "748597"

    // Estado reativo em memória da sessão administrativa
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    // Contador de toques no rodapé para modo desenvolvedor
    private var developerTapCount = 0
    private var lastTapTimestamp = 0L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retorna o PIN configurado pelo proprietário ou o PIN padrão.
     */
    fun getAdminPin(context: Context): String {
        return getPrefs(context).getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    /**
     * Verifica se o PIN digitado confere com o PIN cadastrado.
     * Em caso afirmativo, desbloqueia o modo administrador na sessão atual.
     */
    fun verifyAndUnlock(context: Context, enteredPin: String): Boolean {
        val currentPin = getAdminPin(context)
        val isValid = enteredPin.trim() == currentPin.trim()
        if (isValid) {
            _isAdminUnlocked.value = true
            developerTapCount = 0
        }
        return isValid
    }

    /**
     * Bloqueia o modo administrador imediatamente.
     */
    fun lockAdmin() {
        _isAdminUnlocked.value = false
    }

    /**
     * Permite ao proprietário alterar o PIN de segurança.
     */
    fun changePin(context: Context, currentPin: String, newPin: String): Boolean {
        if (!verifyAndUnlock(context, currentPin)) {
            return false
        }
        val cleanNew = newPin.trim()
        if (cleanNew.length < 4) {
            return false
        }
        getPrefs(context).edit().putString(KEY_ADMIN_PIN, cleanNew).apply()
        return true
    }

    /**
     * Registra toque no rodapé de versão.
     * Retorna true se atingiu o limiar de 5 toques contínuos em menos de 3 segundos.
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
     * Retorna quantos toques faltam para ativar o prompt de PIN.
     */
    fun getRemainingTaps(): Int {
        return (5 - developerTapCount).coerceAtLeast(0)
    }
}
