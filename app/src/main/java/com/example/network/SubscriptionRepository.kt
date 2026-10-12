package com.example.network

import android.content.Context
import android.util.Log
import com.example.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modelo de Estado da Licença consumido pelo Repository e ViewModel
 */
data class LicenseStatusState(
    val customerId: String = "cus_pilot_sp_01",
    val status: String = "UNKNOWN", // "ACTIVE", "OVERDUE", "NOT_FOUND", "PENDING"
    val isLicenseActive: Boolean = false,
    val planType: String = "PLANO_SEMANAL_PIX",
    val activationKey: String = "",
    val expiresAt: String = "",
    val remainingDays: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncTimestamp: Long = 0L,
    val isLocalFallback: Boolean = false
)

/**
 * SubscriptionRepository
 *
 * Gerencia os estados de licença (ativa/bloqueada), sincroniza com os endpoints do FastAPI
 * via [ApiService] e atualiza automaticamente o ecossistema local do aplicativo ([SubscriptionManager]).
 */
class SubscriptionRepository(
    private val context: Context,
    private val apiService: ApiService = NetworkModule.provideApiService(),
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    companion object {
        private const val TAG = "SubscriptionRepository"
        private const val PREFS_NAME = "jarvis_subscription_repo_prefs"
        private const val KEY_CUSTOMER_ID = "persisted_customer_id"

        @Volatile
        private var INSTANCE: SubscriptionRepository? = null

        fun getInstance(context: Context): SubscriptionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _licenseState = MutableStateFlow(
        LicenseStatusState(
            customerId = prefs.getString(KEY_CUSTOMER_ID, "cus_pilot_sp_01") ?: "cus_pilot_sp_01"
        )
    )
    val licenseState: StateFlow<LicenseStatusState> = _licenseState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    init {
        // Inicializa o SubscriptionManager local
        SubscriptionManager.initialize(context)
        // Observa alterações no SubscriptionManager local para sincronia bidirecional
        externalScope.launch {
            SubscriptionManager.subscriptionState.collect { localSub ->
                val current = _licenseState.value
                if (localSub.isActive != current.isLicenseActive && !current.isLoading) {
                    _licenseState.value = current.copy(
                        isLicenseActive = localSub.isActive,
                        status = if (localSub.isActive) "ACTIVE" else "OVERDUE"
                    )
                }
            }
        }
    }

    /**
     * Atualiza o ID do cliente Asaas (identificador do motoboy)
     */
    fun updateCustomerId(newCustomerId: String) {
        val trimmed = newCustomerId.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_CUSTOMER_ID, trimmed).apply()
            _licenseState.value = _licenseState.value.copy(customerId = trimmed)
            refreshLicenseStatus()
        }
    }

    /**
     * Consulta o status da licença no servidor FastAPI (/api/v1/subscription/status/{customerId})
     * Atualiza o estado em tempo real e sincroniza com o SubscriptionManager local.
     */
    fun refreshLicenseStatus() {
        externalScope.launch {
            _licenseState.value = _licenseState.value.copy(isLoading = true, errorMessage = null)
            val customerId = _licenseState.value.customerId

            try {
                val response = apiService.getSubscriptionStatus(customerId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val isActive = body.isActive || (body.status.equals("ACTIVE", ignoreCase = true))

                    _licenseState.value = _licenseState.value.copy(
                        customerId = body.customerId,
                        status = body.status,
                        isLicenseActive = isActive,
                        planType = body.planType ?: "PLANO_SEMANAL_PIX",
                        activationKey = body.activationKey ?: "",
                        expiresAt = body.expiresAt ?: "",
                        remainingDays = calculateRemainingDays(body.expiresAt),
                        isLoading = false,
                        errorMessage = null,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        isLocalFallback = false
                    )

                    // Sincroniza localmente
                    withContext(Dispatchers.Main) {
                        if (isActive) {
                            SubscriptionManager.activateWeeklySubscription()
                        } else if (body.status.equals("OVERDUE", ignoreCase = true)) {
                            SubscriptionManager.resetToFree()
                        }
                    }

                    _eventFlow.emit("Status sincronizado: ${body.status} (Ativo: $isActive)")
                    Log.i(TAG, "Status da licença recebido do servidor: $body")
                } else {
                    handleSyncFailure("Erro no servidor: código HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha na sincronização de licença com o servidor: ${e.message}", e)
                handleSyncFailure("Servidor inacessível. Usando persistência local. (${e.localizedMessage ?: "Timeout"})")
            }
        }
    }

    /**
     * Cria ou renova a assinatura semanal de R$ 25,00 com split de 40% via Asaas
     */
    suspend fun createWeeklySubscription(
        customerId: String = _licenseState.value.customerId,
        leaderWalletId: String = "wal_general_praca_sp_01",
        nextDueDate: String = getNextWeekDueDate()
    ): Result<CreateSubscriptionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateSubscriptionRequest(
                customerId = customerId,
                leaderWalletId = leaderWalletId,
                nextDueDate = nextDueDate,
                value = 25.00,
                cycle = "WEEKLY",
                billingType = "PIX"
            )
            val response = apiService.createSubscription(request)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                _eventFlow.emit("Assinatura Semanal criada com sucesso! Split Líder: 40%")
                Result.success(data)
            } else {
                val errorMsg = "Erro ao criar assinatura: HTTP ${response.code()}"
                _eventFlow.emit(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao criar assinatura no FastAPI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Simula localmente e na VPS o recebimento de Webhook Asaas (ex: para testes de campo)
     */
    suspend fun simulateWebhookPayment(
        eventType: String, // "PAYMENT_RECEIVED" ou "PAYMENT_OVERDUE"
        customerId: String = _licenseState.value.customerId
    ) = withContext(Dispatchers.IO) {
        try {
            val payload = AsaasWebhookPayload(
                event = eventType,
                payment = AsaasPaymentItem(
                    id = "pay_${System.currentTimeMillis() % 100000}",
                    customer = customerId,
                    value = 25.00,
                    billingType = "PIX",
                    status = if (eventType == "PAYMENT_RECEIVED") "RECEIVED" else "OVERDUE"
                )
            )

            // Tenta enviar para a VPS
            try {
                apiService.sendAsaasWebhook(payload)
            } catch (ignored: Exception) {
                Log.d(TAG, "VPS webhook request fallback to local execution: ${ignored.message}")
            }

            // Executa no motor local do app
            withContext(Dispatchers.Main) {
                if (eventType == "PAYMENT_RECEIVED") {
                    SubscriptionManager.activateWeeklySubscription()
                    _licenseState.value = _licenseState.value.copy(
                        status = "ACTIVE",
                        isLicenseActive = true,
                        errorMessage = null,
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                } else {
                    SubscriptionManager.resetToFree()
                    _licenseState.value = _licenseState.value.copy(
                        status = "OVERDUE",
                        isLicenseActive = false,
                        errorMessage = null,
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                }
            }
            _eventFlow.emit("Webhook $eventType processado com sucesso!")
        } catch (e: Exception) {
            Log.e(TAG, "Erro simulando webhook: ${e.message}", e)
        }
    }

    private fun handleSyncFailure(errorMessage: String) {
        val localSub = SubscriptionManager.subscriptionState.value
        _licenseState.value = _licenseState.value.copy(
            isLicenseActive = localSub.isActive,
            status = if (localSub.isActive) "ACTIVE (LOCAL)" else "OVERDUE (LOCAL)",
            isLoading = false,
            errorMessage = errorMessage,
            isLocalFallback = true,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun calculateRemainingDays(expiresAtStr: String?): Int {
        if (expiresAtStr.isNullOrEmpty()) return 0
        return try {
            // Suporta formatos ISO ou SQLite YYYY-MM-DD HH:mm:ss
            val cleaned = expiresAtStr.replace("T", " ")
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            val expiryDate = format.parse(cleaned) ?: return 7
            val diffMs = expiryDate.time - System.currentTimeMillis()
            (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            7 // Fallback padrão de 7 dias
        }
    }

    private fun getNextWeekDueDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return format.format(calendar.time)
    }
}
