package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Representa uma janela ativa detectada na tela dividida (Split-Screen)
 */
data class SplitWindowTelemetry(
    val position: WindowVerticalPosition,
    val packageName: String,
    val appName: String,
    val bounds: Rect,
    val latestOffer: ScreenPoolOffer?,
    val isFocused: Boolean,
    val lastUpdateMillis: Long = System.currentTimeMillis()
)

enum class WindowVerticalPosition {
    TOP,
    BOTTOM,
    FULL_SCREEN,
    FLOATING
}

/**
 * Par pré-configurado de aplicativos para lançamento conjunto em tela dividida.
 */
data class SplitScreenAppPair(
    val id: String,
    val title: String,
    val topPackage: String,
    val topAppName: String,
    val bottomPackage: String,
    val bottomAppName: String,
    val description: String
)

/**
 * SplitScreenDualRadarCoordinator
 *
 * Coordenador tático para o Modo Dividido Vertical (Multi-Window / Split-Screen).
 *
 * Responsável por:
 * 1. Identificar em tempo real todas as janelas interativas na tela ([AccessibilityService.getWindows]).
 * 2. Classificar as janelas por coordenada Y da tela (Top Window vs Bottom Window).
 * 3. Varrer simultaneamente ambas as metades sem necessidade de alternância de foco.
 * 4. Alimentar o [DeepOrderPoolMergingEngine] instantaneamente quando novas corridas surgirem em qualquer metade.
 * 5. Lançar pares de aplicativos diretamente em tela dividida.
 */
class SplitScreenDualRadarCoordinator private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SplitScreenCoordinator"

        @Volatile
        private var INSTANCE: SplitScreenDualRadarCoordinator? = null

        fun getInstance(context: Context): SplitScreenDualRadarCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SplitScreenDualRadarCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Pares de aplicativos homologados para motoristas
        val PRECONFIGURED_PAIRS = listOf(
            SplitScreenAppPair(
                id = "ifood_99",
                title = "iFood (Topo) + 99Food (Base)",
                topPackage = "com.ifood.entregador",
                topAppName = "iFood Entregador",
                bottomPackage = "com.didiglobal.driver",
                bottomAppName = "99 Motorista / 99Food",
                description = "Par clássico de alta densidade de pedidos em capitais e regiões centrais."
            ),
            SplitScreenAppPair(
                id = "ifood_rappi",
                title = "iFood (Topo) + Rappi (Base)",
                topPackage = "com.ifood.entregador",
                topAppName = "iFood Entregador",
                bottomPackage = "com.grability.rappi.entregador",
                bottomAppName = "Rappi Entregador",
                description = "Ideal para zonas nobres e polos gastronômicos com alta concentração de hubs."
            ),
            SplitScreenAppPair(
                id = "noventa_rappi",
                title = "99Food (Topo) + Rappi (Base)",
                topPackage = "com.didiglobal.driver",
                topAppName = "99 Motorista",
                bottomPackage = "com.grability.rappi.entregador",
                bottomAppName = "Rappi Entregador",
                description = "Excelente alternativa para alternância de horários de pico e dias de chuva."
            )
        )
    }

    private val _isSplitScreenActive = MutableStateFlow(false)
    val isSplitScreenActive: StateFlow<Boolean> = _isSplitScreenActive.asStateFlow()

    private val _topWindowTelemetry = MutableStateFlow<SplitWindowTelemetry?>(null)
    val topWindowTelemetry: StateFlow<SplitWindowTelemetry?> = _topWindowTelemetry.asStateFlow()

    private val _bottomWindowTelemetry = MutableStateFlow<SplitWindowTelemetry?>(null)
    val bottomWindowTelemetry: StateFlow<SplitWindowTelemetry?> = _bottomWindowTelemetry.asStateFlow()

    private val _selectedPair = MutableStateFlow(PRECONFIGURED_PAIRS[0])
    val selectedPair: StateFlow<SplitScreenAppPair> = _selectedPair.asStateFlow()

    fun selectPair(pair: SplitScreenAppPair) {
        _selectedPair.value = pair
    }

    /**
     * Varrer todas as janelas do sistema em tempo real capturadas pelo serviço de acessibilidade.
     */
    fun processWindows(accessibilityService: AccessibilityService, windows: List<AccessibilityWindowInfo>?) {
        if (windows.isNullOrEmpty()) {
            _isSplitScreenActive.value = false
            return
        }

        // Filtra apenas janelas de aplicativos interativos
        val appWindows = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.root != null
        }

        // Se houver pelo menos 2 janelas de aplicativos visíveis simultaneamente, o modo dividido está ativo!
        val isDualActive = appWindows.size >= 2
        _isSplitScreenActive.value = isDualActive

        val screenBounds = Rect()
        val windowList = mutableListOf<SplitWindowTelemetry>()

        for (window in appWindows) {
            val root = window.root ?: continue
            val bounds = Rect()
            window.getBoundsInScreen(bounds)

            val pkg = root.packageName?.toString() ?: ""
            val appName = resolveAppName(pkg)

            // Classifica pela posição vertical (Y central)
            val centerY = bounds.centerY()
            val position = when {
                !isDualActive -> WindowVerticalPosition.FULL_SCREEN
                centerY < 1000 -> WindowVerticalPosition.TOP
                else -> WindowVerticalPosition.BOTTOM
            }

            // Extrai a oferta visível nesta janela
            val offer = extractOfferFromWindowNode(pkg, appName, root)
            @Suppress("DEPRECATION")
            root.recycle()

            windowList.add(
                SplitWindowTelemetry(
                    position = position,
                    packageName = pkg,
                    appName = appName,
                    bounds = bounds,
                    latestOffer = offer,
                    isFocused = window.isFocused
                )
            )
        }

        val top = windowList.firstOrNull { it.position == WindowVerticalPosition.TOP }
        val bottom = windowList.firstOrNull { it.position == WindowVerticalPosition.BOTTOM }

        _topWindowTelemetry.value = top
        _bottomWindowTelemetry.value = bottom

        // Se ambas as metades possuem ofertas capturadas, alimenta o DeepOrderPoolMergingEngine imediatamente
        if (top?.latestOffer != null && bottom?.latestOffer != null) {
            val poolEngine = DeepOrderPoolMergingEngine.getInstance(context)
            poolEngine.ingestPoolOffers(top.appName, listOf(top.latestOffer))
            poolEngine.ingestPoolOffers(bottom.appName, listOf(bottom.latestOffer))
            Log.i(TAG, "⚡ Sucesso na leitura simultânea: Topo=${top.appName} R$ ${top.latestOffer.value} | Base=${bottom.appName} R$ ${bottom.latestOffer.value}")
        }
    }

    /**
     * Extrai a melhor oferta visível dentro do nó de uma janela específica
     */
    private fun extractOfferFromWindowNode(
        packageName: String,
        appName: String,
        rootNode: AccessibilityNodeInfo
    ): ScreenPoolOffer? {
        val texts = mutableListOf<String>()
        collectTexts(rootNode, texts)
        val blob = texts.joinToString(" ")

        val valueRegex = Regex("""R\$\s*(\d+[.,]\d{2})""")
        val distanceRegex = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)

        val matchVal = valueRegex.find(blob)
        val matchDist = distanceRegex.find(blob)

        if (matchVal != null && matchDist != null) {
            val v = matchVal.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            val d = matchDist.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0

            if (v > 0.0 && d > 0.0) {
                val rest = texts.firstOrNull {
                    it.contains("Restaurante", true) ||
                    it.contains("Shopping", true) ||
                    it.contains("Habib", true) ||
                    it.contains("Burger", true) ||
                    it.contains("Outback", true) ||
                    it.contains("Pizzaria", true)
                } ?: "Restaurante $appName"

                val drop = texts.firstOrNull {
                    it.contains("Rua", true) ||
                    it.contains("Av", true) ||
                    it.contains("Alameda", true)
                } ?: "Destino na Cidade"

                return ScreenPoolOffer(
                    appName = appName,
                    packageName = packageName,
                    restaurantName = rest,
                    pickupAddress = rest,
                    dropoffAddress = drop,
                    value = v,
                    distanceKm = d
                )
            }
        }
        return null
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val t = node.text?.toString()?.trim()
        val d = node.contentDescription?.toString()?.trim()
        if (!t.isNullOrEmpty()) list.add(t)
        else if (!d.isNullOrEmpty()) list.add(d)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectTexts(child, list)
            @Suppress("DEPRECATION")
            child?.recycle()
        }
    }

    /**
     * Dispara a ação global do Android para abrir o modo de tela dividida.
     */
    fun triggerSplitScreenMode(accessibilityService: AccessibilityService?): Boolean {
        if (accessibilityService == null) {
            Log.w(TAG, "JarvisAccessibilityService não está ativo.")
            return false
        }

        try {
            val success = accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            Log.i(TAG, "Ação GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN disparada. Sucesso: $success")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao acionar tela dividida via AccessibilityService", e)
            return false
        }
    }

    /**
     * Lança o par configurado de aplicativos.
     * Primeiro abre o app superior, aguarda brevemente e aciona a divisão de tela para escolher o inferior.
     */
    fun launchSplitScreenPair(context: Context, pair: SplitScreenAppPair) {
        try {
            val pm = context.packageManager
            val topIntent = pm.getLaunchIntentForPackage(pair.topPackage)
            if (topIntent != null) {
                topIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(topIntent)
            }

            // Solicita a divisão via serviço de acessibilidade se ativo
            JarvisAccessibilityService.instance?.let { service ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            }

            TextToSpeechManager.getInstance(context).speakVoiceAlert(
                "Iniciando modo dividido: ${pair.topAppName} no topo e ${pair.bottomAppName} na base. Leitura simultânea ativada."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao lançar par de tela dividida: ${e.message}")
        }
    }

    private fun resolveAppName(packageName: String): String {
        return when {
            packageName.contains("ifood", ignoreCase = true) -> "iFood"
            packageName.contains("didiglobal", ignoreCase = true) || packageName.contains("99", ignoreCase = true) -> "99Food"
            packageName.contains("rappi", ignoreCase = true) -> "Rappi"
            packageName.contains("uber", ignoreCase = true) -> "Uber Driver"
            packageName.contains("lalamove", ignoreCase = true) -> "Lalamove"
            else -> packageName.substringAfterLast('.')
        }
    }

    /**
     * Permite simular telemetria simultânea em ambiente de teste/emulador.
     */
    fun simulateDualSplitWindowTelemetry() {
        val topOffer = ScreenPoolOffer(
            appName = "iFood",
            packageName = "com.ifood.entregador",
            restaurantName = "Fogo de Chão - Jardins",
            pickupAddress = "Rua Augusta, 2077",
            dropoffAddress = "Av. Rebouças, 1800 - Pinheiros",
            value = 22.50,
            distanceKm = 3.4,
            pickupLat = -23.5590,
            pickupLng = -46.6620,
            dropoffLat = -23.5680,
            dropoffLng = -46.6850
        )

        val bottomOffer = ScreenPoolOffer(
            appName = "99Food",
            packageName = "com.didiglobal.driver",
            restaurantName = "Bullguer Jardins",
            pickupAddress = "Alameda Lorena, 1430",
            dropoffAddress = "Rua dos Pinheiros, 600 - Pinheiros",
            value = 18.00,
            distanceKm = 2.9,
            pickupLat = -23.5630,
            pickupLng = -46.6650,
            dropoffLat = -23.5670,
            dropoffLng = -46.6880
        )

        _isSplitScreenActive.value = true
        _topWindowTelemetry.value = SplitWindowTelemetry(
            position = WindowVerticalPosition.TOP,
            packageName = topOffer.packageName,
            appName = topOffer.appName,
            bounds = Rect(0, 0, 1080, 1150),
            latestOffer = topOffer,
            isFocused = true
        )

        _bottomWindowTelemetry.value = SplitWindowTelemetry(
            position = WindowVerticalPosition.BOTTOM,
            packageName = bottomOffer.packageName,
            appName = bottomOffer.appName,
            bounds = Rect(0, 1150, 1080, 2300),
            latestOffer = bottomOffer,
            isFocused = false
        )

        val poolEngine = DeepOrderPoolMergingEngine.getInstance(context)
        poolEngine.ingestPoolOffers("iFood", listOf(topOffer))
        poolEngine.ingestPoolOffers("99Food", listOf(bottomOffer))
    }
}
