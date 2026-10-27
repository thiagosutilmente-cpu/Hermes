package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * KitchenWaitTimerEngine
 *
 * Motor de estimativa inteligente de tempo de preparo de cozinha e cronômetro
 * regressivo em tempo real para pedidos mesclados (iFood + 99 + Rappi).
 *
 * Evita o principal problema da vida real: retirar um lanche rápido que esfria
 * enquanto o motoboy espera 15 minutos pelo segundo pedido de restaurante lento.
 */
object KitchenWaitTimerEngine {

    private const val TAG = "KitchenWaitTimer"

    enum class KitchenCategory(val avgPrepMinutes: Int, val label: String) {
        FAST_FOOD(4, "Fast Food (Balcão Rápido)"),
        CONVENIENCE_DRINKS(2, "Bebidas / Conveniência"),
        PIZZERIA(12, "Pizzaria / Forno"),
        CASUAL_DINING(14, "Restaurante Casual / Massas"),
        STEAKHOUSE_GOURMET(18, "Gourmet / Carnes / Grelhados")
    }

    enum class OrderKitchenStatus {
        PREPARING,
        ALMOST_READY,
        READY_AT_COUNTER,
        PICKED_UP
    }

    data class KitchenOrder(
        val orderId: String,
        val appName: String,
        val restaurantName: String,
        val category: KitchenCategory,
        val initialSeconds: Int,
        val remainingSeconds: Int,
        val status: OrderKitchenStatus
    )

    data class KitchenStackState(
        val orders: List<KitchenOrder> = emptyList(),
        val recommendedPickupSequence: String = "",
        val tacticalAdvice: String = "",
        val isTimerRunning: Boolean = false
    )

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _stackState = MutableStateFlow(KitchenStackState())
    val stackState: StateFlow<KitchenStackState> = _stackState.asStateFlow()

    init {
        // Inicializa com um cenário realista padrão para demonstração
        loadDefaultScenario()
    }

    fun loadDefaultScenario() {
        val orderA = KitchenOrder(
            orderId = "ord_ifood_outback",
            appName = "iFood",
            restaurantName = "Outback Steakhouse Paulista",
            category = KitchenCategory.STEAKHOUSE_GOURMET,
            initialSeconds = 14 * 60,
            remainingSeconds = 5 * 60 + 20, // 5 min e 20s restantes
            status = OrderKitchenStatus.PREPARING
        )

        val orderB = KitchenOrder(
            orderId = "ord_99_bk",
            appName = "99 Food",
            restaurantName = "Burger King Paulista",
            category = KitchenCategory.FAST_FOOD,
            initialSeconds = 4 * 60,
            remainingSeconds = 45, // 45 segundos restantes
            status = OrderKitchenStatus.ALMOST_READY
        )

        updateState(listOf(orderA, orderB))
    }

    fun startTimerLoop(context: Context) {
        if (_stackState.value.isTimerRunning) return

        _stackState.value = _stackState.value.copy(isTimerRunning = true)
        scope.launch {
            while (isActive && _stackState.value.isTimerRunning) {
                delay(1000L)
                val currentOrders = _stackState.value.orders.map { order ->
                    if (order.status == OrderKitchenStatus.PICKED_UP || order.remainingSeconds <= 0) {
                        order.copy(remainingSeconds = 0, status = OrderKitchenStatus.READY_AT_COUNTER)
                    } else {
                        val newSec = order.remainingSeconds - 1
                        val newStatus = when {
                            newSec <= 0 -> OrderKitchenStatus.READY_AT_COUNTER
                            newSec <= 60 -> OrderKitchenStatus.ALMOST_READY
                            else -> OrderKitchenStatus.PREPARING
                        }
                        order.copy(remainingSeconds = newSec, status = newStatus)
                    }
                }
                updateState(currentOrders)
            }
        }
    }

    fun markOrderPickedUp(orderId: String) {
        val updated = _stackState.value.orders.map {
            if (it.orderId == orderId) it.copy(status = OrderKitchenStatus.PICKED_UP, remainingSeconds = 0)
            else it
        }
        updateState(updated)
    }

    fun resetScenario(fastFoodFirst: Boolean) {
        if (fastFoodFirst) {
            loadDefaultScenario()
        } else {
            val orderA = KitchenOrder(
                orderId = "ord_rappi_pizzaria",
                appName = "Rappi",
                restaurantName = "Pizza Hut Consolação",
                category = KitchenCategory.PIZZERIA,
                initialSeconds = 12 * 60,
                remainingSeconds = 8 * 60,
                status = OrderKitchenStatus.PREPARING
            )
            val orderB = KitchenOrder(
                orderId = "ord_ze_bebidas",
                appName = "Zé Delivery",
                restaurantName = "Adega Paulista Express",
                category = KitchenCategory.CONVENIENCE_DRINKS,
                initialSeconds = 2 * 60,
                remainingSeconds = 0,
                status = OrderKitchenStatus.READY_AT_COUNTER
            )
            updateState(listOf(orderA, orderB))
        }
    }

    private fun updateState(orders: List<KitchenOrder>) {
        val readyCount = orders.count { it.status == OrderKitchenStatus.READY_AT_COUNTER }
        val slowOrder = orders.maxByOrNull { it.remainingSeconds }
        val fastOrder = orders.minByOrNull { it.remainingSeconds }

        val sequence = if (slowOrder != null && fastOrder != null && slowOrder.orderId != fastOrder.orderId) {
            "1º Aguardar ${slowOrder.restaurantName.take(16)} -> 2º Retirar ${fastOrder.restaurantName.take(16)}"
        } else {
            "Retirada simultânea liberada nos balcões"
        }

        val advice = if (fastOrder != null && fastOrder.status == OrderKitchenStatus.READY_AT_COUNTER && slowOrder != null && slowOrder.remainingSeconds > 120) {
            "⚠️ ATENÇÃO: O pedido ${fastOrder.restaurantName} já está pronto. Evite retirá-lo agora para não esfriar na bag enquanto aguarda o preparo do ${slowOrder.restaurantName} (${formatTime(slowOrder.remainingSeconds)} restantes)."
        } else {
            "✅ Ordem sincronizada: Ambos os pedidos estarão prontos com janela menor que 2 minutos."
        }

        _stackState.value = _stackState.value.copy(
            orders = orders,
            recommendedPickupSequence = sequence,
            tacticalAdvice = advice
        )
    }

    fun formatTime(seconds: Int): String {
        val nonNegative = seconds.coerceAtLeast(0)
        val m = nonNegative / 60
        val s = nonNegative % 60
        return String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }
}
