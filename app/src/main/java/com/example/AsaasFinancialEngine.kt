package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parâmetros de Regra de Negócio do Motor Financeiro Asaas (Passo 1):
 * 1. Assinatura Semanal: R$ 25,00 via PIX Recorrente
 * 2. Split Automático:
 *    - 40% (R$ 10,00) fixo para a conta Asaas do Líder de Praça (walletId do General)
 *    - 60% (R$ 15,00) líquido retido na Conta Master PJ do App
 * 3. Webhooks:
 *    - PAYMENT_RECEIVED: Ativa licença PRO por 7 dias
 *    - PAYMENT_OVERDUE: Bloqueia acesso PRO no app imediatamente
 */
object AsaasFinancialEngine {

    private const val TAG = "AsaasFinancialEngine"

    // Parâmetros Oficiais de Precificação e Split
    const val WEEKLY_SUBSCRIPTION_PRICE = 25.00 // R$ 25,00
    const val LEADER_SPLIT_PERCENTAGE = 40.0    // 40%
    const val LEADER_FIXED_VALUE = 10.00        // R$ 10,00 fixo
    const val MASTER_PJ_PERCENTAGE = 60.0       // 60%
    const val MASTER_PJ_FIXED_VALUE = 15.00     // R$ 15,00 fixo
    const val CYCLE_TYPE = "WEEKLY"
    const val BILLING_TYPE = "PIX"

    // Wallet ID padrão do General / Líder de Praça (configurável por praça/região)
    const val DEFAULT_GENERAL_WALLET_ID = "wal_general_praca_sp_01"

    private val _engineState = MutableStateFlow(
        AsaasEngineState(
            subscriptionPrice = WEEKLY_SUBSCRIPTION_PRICE,
            leaderSplitValue = LEADER_FIXED_VALUE,
            masterPjSplitValue = MASTER_PJ_FIXED_VALUE,
            generalWalletId = DEFAULT_GENERAL_WALLET_ID,
            isSplitValid = true
        )
    )
    val engineState: StateFlow<AsaasEngineState> = _engineState.asStateFlow()

    data class AsaasEngineState(
        val subscriptionPrice: Double,
        val leaderSplitValue: Double,
        val masterPjSplitValue: Double,
        val generalWalletId: String,
        val isSplitValid: Boolean,
        val lastWebhookProcessed: String? = null
    )

    /**
     * Valida matematicamente a conformidade da regra de split.
     */
    fun validateSplit(
        totalPrice: Double = WEEKLY_SUBSCRIPTION_PRICE,
        leaderValue: Double = LEADER_FIXED_VALUE,
        masterValue: Double = MASTER_PJ_FIXED_VALUE
    ): Boolean {
        val sum = leaderValue + masterValue
        val isSumEqual = Math.abs(sum - totalPrice) < 0.001
        val leaderRatio = leaderValue / totalPrice
        val masterRatio = masterValue / totalPrice
        val isLeader40 = Math.abs(leaderRatio - 0.40) < 0.001
        val isMaster60 = Math.abs(masterRatio - 0.60) < 0.001
        return isSumEqual && isLeader40 && isMaster60
    }

    /**
     * Gera o Payload JSON oficial de criação de Assinatura Semanal com Split no Asaas API v3.
     */
    fun buildAsaasSubscriptionPayload(
        customerAsaasId: String,
        customerName: String,
        generalWalletId: String = _engineState.value.generalWalletId,
        nextDueDate: String // Formato YYYY-MM-DD
    ): JSONObject {
        val payload = JSONObject()
        payload.put("customer", customerAsaasId)
        payload.put("billingType", BILLING_TYPE)
        payload.put("value", WEEKLY_SUBSCRIPTION_PRICE)
        payload.put("nextDueDate", nextDueDate)
        payload.put("cycle", CYCLE_TYPE)
        payload.put("description", "Assinatura Semanal JARVIS NEURAL COCKPIT PRO (PIX Recorrente)")

        // Configuração de Split Automático conforme documentação oficial do Asaas
        val splitArray = JSONArray()
        val leaderSplit = JSONObject()
        leaderSplit.put("walletId", generalWalletId)
        leaderSplit.put("fixedValue", LEADER_FIXED_VALUE) // R$ 10,00 fixo (40%)
        leaderSplit.put("description", "Comissão Líder de Praça (40%)")
        splitArray.put(leaderSplit)

        payload.put("split", splitArray)
        return payload
    }

    /**
     * Valida o token de assinatura Pro autenticado via HTTPS diretamente com o servidor VPS.
     * Substitui o processamento local de webhooks (processWebhookEvent), delegando
     * a validação de pagamento e status ao backend seguro do servidor.
     */
    suspend fun verifySubscriptionTokenWithServer(
        context: Context,
        token: String? = null
    ): Boolean {
        if (!token.isNullOrBlank()) {
            SubscriptionManager.setAuthToken(token)
        }
        val isVerified = SubscriptionManager.verifySubscriptionWithServer(context)
        _engineState.value = _engineState.value.copy(
            lastWebhookProcessed = if (isVerified) {
                "HTTPS_TOKEN_VERIFIED: Licença PRO validada pelo servidor"
            } else {
                "HTTPS_TOKEN_REJECTED: Licença inativa ou expirada no servidor"
            }
        )
        return isVerified
    }

    /**
     * @deprecated Webhooks do Asaas são agora recebidos e validados com exclusividade pelo servidor backend VPS.
     * Esta função foi substituída por [verifySubscriptionTokenWithServer] via HTTPS autenticado.
     */
    @Deprecated("Substituído por verificação de token autenticada via HTTPS no servidor")
    fun processWebhookEvent(
        context: Context,
        eventType: String,
        customerId: String,
        value: Double,
        webhookSecretToken: String? = null
    ): Boolean {
        Log.w(TAG, "Aviso: processWebhookEvent local foi substituído. Webhooks são recebidos no servidor VPS.")
        Log.i(TAG, "Acionando sincronização de token autenticada via HTTPS para o cliente: $customerId")
        CoroutineScope(Dispatchers.IO).launch {
            verifySubscriptionTokenWithServer(context, customerId)
        }
        return true
    }
}
