package com.example.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * SubscriptionViewModel
 *
 * ViewModel responsável por expor o status da licença (ativa/bloqueada), gerenciar
 * comandos de renovação semanal Asaas e fornecer observação em tempo real para as telas do Compose.
 */
class SubscriptionViewModel(
    application: Application,
    private val repository: SubscriptionRepository = SubscriptionRepository.getInstance(application)
) : AndroidViewModel(application) {

    // Estado da licença observado pelas telas em tempo real
    val licenseState: StateFlow<LicenseStatusState> = repository.licenseState

    // Notificações e mensagens de evento transitórias para Toasts ou Snackbars
    val eventFlow: SharedFlow<String> = repository.eventFlow

    init {
        // Carrega o status ao instanciar o ViewModel
        repository.refreshLicenseStatus()
    }

    /**
     * Força a sincronização do status com o servidor FastAPI
     */
    fun refreshStatus() {
        repository.refreshLicenseStatus()
    }

    /**
     * Atualiza o ID do cliente Asaas (identificador do entregador)
     */
    fun setCustomerId(customerId: String) {
        repository.updateCustomerId(customerId)
    }

    /**
     * Dispara a criação/ativação de uma nova assinatura semanal
     */
    fun subscribeWeekly(
        leaderWalletId: String = "wal_general_praca_sp_01",
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.createWeeklySubscription(leaderWalletId = leaderWalletId)
            result.onSuccess { response ->
                onComplete(true, "Assinatura Semanal (R$ 25,00) criada! Split Líder: R$ 10,00")
                refreshStatus()
            }.onFailure { error ->
                onComplete(false, error.localizedMessage ?: "Erro ao criar assinatura")
            }
        }
    }

    /**
     * Simula recebimento de pagamento via PIX para liberar imediatamente a licença de 7 dias
     */
    fun simulatePaymentReceived() {
        viewModelScope.launch {
            repository.simulateWebhookPayment("PAYMENT_RECEIVED")
        }
    }

    /**
     * Simula inadimplência (PAYMENT_OVERDUE) para bloquear o acesso PRO no app
     */
    fun simulatePaymentOverdue() {
        viewModelScope.launch {
            repository.simulateWebhookPayment("PAYMENT_OVERDUE")
        }
    }
}
