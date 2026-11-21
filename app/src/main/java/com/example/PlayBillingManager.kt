package com.example

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Representa os estados de conexão com o serviço Google Play Billing.
 */
enum class PlayBillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    UNAVAILABLE,
    ERROR
}

/**
 * Representa os estados do fluxo de compra / checkout de assinatura.
 */
sealed class PlayBillingPurchaseState {
    object Idle : PlayBillingPurchaseState()
    object Loading : PlayBillingPurchaseState()
    data class Success(val orderId: String, val productId: String, val message: String) : PlayBillingPurchaseState()
    data class Pending(val message: String) : PlayBillingPurchaseState()
    data class Cancelled(val message: String = "Operação cancelada pelo usuário") : PlayBillingPurchaseState()
    data class Error(val errorMessage: String) : PlayBillingPurchaseState()
}

/**
 * Detalhes de um plano de assinatura do Google Play.
 */
data class PlaySubscriptionProduct(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriod: String,
    val freeTrialPeriodDays: Int = 0
)

/**
 * Gerenciador oficial da Biblioteca de Faturamento do Google Play (Google Play Billing v9.1.0).
 *
 * Responsável por:
 * 1. Estabelecer e manter conexão real com o Google Play Store via [BillingClient].
 * 2. Consultar catálogo de assinaturas configuradas no Google Play Console ([ProductDetails]).
 * 3. Iniciar o fluxo nativo de pagamento / assinatura ([BillingClient.launchBillingFlow]).
 * 4. Processar confirmações e confirmação de recebimento (Acknowledge) obrigatória em até 3 dias.
 * 5. Restaurar compras anteriores ativas do condutor via [BillingClient.queryPurchasesAsync].
 * 6. Desbloquear os recursos Premium no [SubscriptionManager] somente após confirmação real.
 */
object PlayBillingManager {

    private const val TAG = "PlayBillingManager"
    private const val PREFS_BILLING = "radar_play_billing_prefs"
    private const val KEY_LAST_PURCHASE_TOKEN = "last_play_purchase_token"
    private const val KEY_LAST_ORDER_ID = "last_play_order_id"
    private const val KEY_LAST_PRODUCT_ID = "last_play_product_id"
    private const val KEY_PURCHASE_TIMESTAMP = "last_play_timestamp"

    // IDs de produtos de assinatura configurados no Google Play Console
    const val SUBSCRIPTION_ID_MONTHLY = "radar_pro_mensal"
    const val SUBSCRIPTION_ID_ANNUAL = "radar_pro_anual"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private var billingClient: BillingClient? = null
    private val billingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val cachedProductDetails = mutableMapOf<String, ProductDetails>()
    private var pendingPurchaseCallback: ((Boolean, String?) -> Unit)? = null

    private val _connectionState = MutableStateFlow(PlayBillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PlayBillingConnectionState> = _connectionState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PlayBillingPurchaseState>(PlayBillingPurchaseState.Idle)
    val purchaseState: StateFlow<PlayBillingPurchaseState> = _purchaseState.asStateFlow()

    private val _availableSubscriptions = MutableStateFlow<List<PlaySubscriptionProduct>>(emptyList())
    val availableSubscriptions: StateFlow<List<PlaySubscriptionProduct>> = _availableSubscriptions.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdated(billingResult, purchases)
    }

    /**
     * Inicializa o BillingClient oficial com PendingPurchases e reconexão automática.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_BILLING, Context.MODE_PRIVATE)

        loadDefaultSubscriptionProducts()

        if (billingClient == null) {
            val pendingParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()

            billingClient = BillingClient.newBuilder(appContext)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(pendingParams)
                .enableAutoServiceReconnection()
                .build()
        }

        startBillingConnection()
    }

    /**
     * Metadados base enquanto o catálogo oficial do Google Play Console carrega ou não está disponível offline.
     */
    private fun loadDefaultSubscriptionProducts() {
        if (_availableSubscriptions.value.isNotEmpty()) return
        _availableSubscriptions.value = listOf(
            PlaySubscriptionProduct(
                productId = SUBSCRIPTION_ID_MONTHLY,
                title = "Radar Pro Mensal",
                description = "Varredura ilimitada, comandos de voz no capacete e filtro anti-prejuízo",
                formattedPrice = "R$ 99,90",
                billingPeriod = "Mensal",
                freeTrialPeriodDays = 7
            ),
            PlaySubscriptionProduct(
                productId = SUBSCRIPTION_ID_ANNUAL,
                title = "Radar Pro Anual (Desconto 45%)",
                description = "Economize mais de R$ 548 no ano com todos os recursos Pro liberados",
                formattedPrice = "R$ 650,00",
                billingPeriod = "Anual (R$ 54,16/mês)",
                freeTrialPeriodDays = 7
            )
        )
    }

    /**
     * Conecta ao serviço oficial Google Play Billing.
     */
    fun startBillingConnection() {
        val client = billingClient ?: return
        if (client.isReady) {
            _connectionState.value = PlayBillingConnectionState.CONNECTED
            billingScope.launch {
                refreshSubscriptionCatalog()
                queryExistingPurchasesInternal()
            }
            return
        }
        if (_connectionState.value == PlayBillingConnectionState.CONNECTING) return

        _connectionState.value = PlayBillingConnectionState.CONNECTING
        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            _connectionState.value = PlayBillingConnectionState.CONNECTED
                            Log.d(TAG, "Google Play Billing v9 conectado com sucesso.")
                            billingScope.launch {
                                refreshSubscriptionCatalog()
                                queryExistingPurchasesInternal()
                            }
                        }
                        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                            _connectionState.value = PlayBillingConnectionState.UNAVAILABLE
                            Log.w(TAG, "Google Play Billing indisponível neste dispositivo: ${billingResult.debugMessage}")
                        }
                        else -> {
                            _connectionState.value = PlayBillingConnectionState.ERROR
                            Log.e(TAG, "Erro ao configurar BillingClient (${billingResult.responseCode}): ${billingResult.debugMessage}")
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _connectionState.value = PlayBillingConnectionState.DISCONNECTED
                    Log.w(TAG, "Serviço Google Play Billing desconectado; reconexão automática acionada quando necessário.")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao iniciar conexão com Play Billing: ${e.message}", e)
            _connectionState.value = PlayBillingConnectionState.ERROR
        }
    }

    /**
     * Consulta o catálogo real de assinaturas cadastradas no Google Play Console.
     */
    suspend fun refreshSubscriptionCatalog() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = withContext(Dispatchers.IO) {
            client.queryProductDetails(params)
        }

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val detailsList = result.productDetailsList.orEmpty()
            detailsList.forEach { details ->
                cachedProductDetails[details.productId] = details
            }
            if (detailsList.isNotEmpty()) {
                _availableSubscriptions.value = detailsList.map { details ->
                    val firstPhase = details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.lastOrNull()
                    PlaySubscriptionProduct(
                        productId = details.productId,
                        title = details.title,
                        description = details.description,
                        formattedPrice = firstPhase?.formattedPrice ?: "",
                        billingPeriod = if (details.productId == SUBSCRIPTION_ID_ANNUAL) "Anual" else "Mensal",
                        freeTrialPeriodDays = 7
                    )
                }
            }
            Log.d(TAG, "Catálogo Google Play carregado: ${detailsList.size} assinatura(s).")
        } else {
            Log.w(TAG, "Não foi possível consultar produtos no Play Console (${result.billingResult.responseCode}): ${result.billingResult.debugMessage}")
        }
    }

    /**
     * Inicia o fluxo oficial de assinatura do Google Play (BottomSheet nativo do Google Play).
     */
    fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val client = billingClient
        if (client == null || !client.isReady) {
            startBillingConnection()
            val msg = "Google Play Billing desconectado. Tente novamente em alguns segundos."
            _purchaseState.value = PlayBillingPurchaseState.Error(msg)
            onComplete(false, msg)
            return
        }

        pendingPurchaseCallback = onComplete
        _purchaseState.value = PlayBillingPurchaseState.Loading

        billingScope.launch {
            var details = cachedProductDetails[productId]
            if (details == null) {
                refreshSubscriptionCatalog()
                details = cachedProductDetails[productId]
            }

            if (details == null) {
                val msg = "Plano '$productId' ainda não publicado no Google Play Console para este app."
                _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                pendingPurchaseCallback?.invoke(false, msg)
                pendingPurchaseCallback = null
                return@launch
            }

            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                val msg = "Nenhuma oferta ativa encontrada no Play Console para '$productId'."
                _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                pendingPurchaseCallback?.invoke(false, msg)
                pendingPurchaseCallback = null
                return@launch
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            val launchResult = client.launchBillingFlow(activity, billingFlowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                val msg = "Não foi possível abrir o Google Play (${launchResult.responseCode}): ${launchResult.debugMessage}"
                _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                pendingPurchaseCallback?.invoke(false, msg)
                pendingPurchaseCallback = null
            }
        }
    }

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    val msg = "Nenhuma compra retornada pelo Google Play."
                    _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                    pendingPurchaseCallback?.invoke(false, msg)
                    pendingPurchaseCallback = null
                    return
                }
                billingScope.launch {
                    purchases.forEach { purchase ->
                        processPurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                val msg = "Assinatura cancelada pelo usuário."
                _purchaseState.value = PlayBillingPurchaseState.Cancelled(msg)
                pendingPurchaseCallback?.invoke(false, msg)
                pendingPurchaseCallback = null
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                billingScope.launch {
                    queryExistingPurchasesInternal()
                }
                val msg = "Você já possui esta assinatura ativa na conta Google."
                pendingPurchaseCallback?.invoke(true, msg)
                pendingPurchaseCallback = null
            }
            else -> {
                val msg = "Erro no Google Play (${billingResult.responseCode}): ${billingResult.debugMessage}"
                _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                pendingPurchaseCallback?.invoke(false, msg)
                pendingPurchaseCallback = null
            }
        }
    }

    private suspend fun processPurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val ackResult = withContext(Dispatchers.IO) {
                        billingClient?.acknowledgePurchase(ackParams)
                    }
                    if (ackResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                        val msg = "Falha ao confirmar compra no Google Play (${ackResult?.responseCode})."
                        _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                        pendingPurchaseCallback?.invoke(false, msg)
                        pendingPurchaseCallback = null
                        return
                    }
                }

                val productId = purchase.products.firstOrNull() ?: SUBSCRIPTION_ID_MONTHLY
                val orderId = purchase.orderId ?: "GPA.VERIFIED"
                val isAnnual = productId == SUBSCRIPTION_ID_ANNUAL

                prefs.edit()
                    .putString(KEY_LAST_PURCHASE_TOKEN, purchase.purchaseToken)
                    .putString(KEY_LAST_ORDER_ID, orderId)
                    .putString(KEY_LAST_PRODUCT_ID, productId)
                    .putLong(KEY_PURCHASE_TIMESTAMP, purchase.purchaseTime)
                    .apply()

                SubscriptionManager.activateProSubscription(annual = isAnnual)

                FirebaseAnalyticsManager.logPurchaseSuccess(
                    transactionId = orderId,
                    planId = productId,
                    value = if (isAnnual) 650.0 else 99.90,
                    paymentMethod = "google_play"
                )

                val successMsg = "Assinatura confirmada pelo Google Play! Recursos Pro liberados."
                _purchaseState.value = PlayBillingPurchaseState.Success(
                    orderId = orderId,
                    productId = productId,
                    message = successMsg
                )
                pendingPurchaseCallback?.invoke(true, successMsg)
                pendingPurchaseCallback = null
            }
            Purchase.PurchaseState.PENDING -> {
                val pendingMsg = "Pagamento pendente de confirmação no Google Play. Assim que compensar, seu acesso será liberado."
                _purchaseState.value = PlayBillingPurchaseState.Pending(pendingMsg)
                pendingPurchaseCallback?.invoke(false, pendingMsg)
                pendingPurchaseCallback = null
            }
            else -> {
                val msg = "Estado de compra não concluído."
                _purchaseState.value = PlayBillingPurchaseState.Error(msg)
                pendingPurchaseCallback?.invoke(false, msg)
                pendingPurchaseCallback = null
            }
        }
    }

    /**
     * Restaura compras anteriores ativas na conta Google Play do condutor.
     */
    fun restorePurchases(onResult: (Int, String) -> Unit) {
        val client = billingClient
        if (client == null || !client.isReady) {
            startBillingConnection()
            onResult(0, "Google Play Billing indisponível no momento.")
            return
        }

        _purchaseState.value = PlayBillingPurchaseState.Loading
        billingScope.launch {
            val activeCount = queryExistingPurchasesInternal()
            if (activeCount > 0) {
                FirebaseAnalyticsManager.logSubscriptionRestored(activeCount)
                onResult(activeCount, "$activeCount assinatura(s) ativa(s) restaurada(s) do Google Play!")
            } else {
                FirebaseAnalyticsManager.logSubscriptionRestored(0)
                _purchaseState.value = PlayBillingPurchaseState.Idle
                onResult(0, "Nenhuma assinatura ativa encontrada nesta conta Google.")
            }
        }
    }

    /**
     * Consulta compras de assinaturas ativas via Google Play Billing.
     */
    private suspend fun queryExistingPurchasesInternal(): Int {
        val client = billingClient ?: return 0
        if (!client.isReady) return 0

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = withContext(Dispatchers.IO) {
            client.queryPurchasesAsync(params)
        }

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return 0
        }

        val activePurchases = result.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        activePurchases.forEach { purchase ->
            processPurchase(purchase)
        }
        return activePurchases.size
    }

    /**
     * Reseta o estado da compra para Idle.
     */
    fun resetPurchaseState() {
        _purchaseState.value = PlayBillingPurchaseState.Idle
    }
}
