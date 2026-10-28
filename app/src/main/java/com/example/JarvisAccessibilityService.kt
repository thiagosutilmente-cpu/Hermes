package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Serviço de Acessibilidade Tática do Jarvis Neural Cockpit.
 *
 * Responsável por:
 * 1. Monitorar em tempo real as telas dos aplicativos de entrega suportados (iFood, Uber, 99 Moto, Rappi).
 * 2. Varrer a árvore visual (AccessibilityNodeInfo) para extrair valores em R$, quilometragem e pontos de entrega.
 * 3. Identificar os nós de botões de aceitação das plataformas ("Aceitar", "Confirmar").
 * 4. Integrar com o motor de Rota Dupla do [AssistedQuickSwitchManager] (Opção B - Troca Rápida Assistida).
 */
class JarvisAccessibilityService : AccessibilityService() {

    data class DetectedScreenOffer(
        val appName: String,
        val packageName: String,
        val value: Double,
        val distanceKm: Double,
        val destinationAddress: String,
        val pickupLocation: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var lastProcessTime: Long = 0
    private val debounceIntervalMs: Long = 300 // Evita processamento excessivo de CPU

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Intercepta apenas eventos dos apps de motoristas/entregadores homologados
        if (!isSupportedDeliveryApp(packageName)) return

        val now = System.currentTimeMillis()
        if (now - lastProcessTime < debounceIntervalMs) return
        lastProcessTime = now

        val rootNode = rootInActiveWindow ?: return
        try {
            // Varredura simultânea de ambas as metades da tela se estiver em modo tela dividida
            try {
                val allWindows = windows
                SplitScreenDualRadarCoordinator.getInstance(applicationContext).processWindows(this, allWindows)
            } catch (_: Exception) {}

            processDeliveryScreen(packageName, rootNode)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            @Suppress("DEPRECATION")
            rootNode.recycle()
        }
    }

    /**
     * Varrer a tela do app em busca de cards de ofertas ativas
     */
    private fun processDeliveryScreen(packageName: String, rootNode: AccessibilityNodeInfo) {
        val appName = resolveAppName(packageName)
        val textNodes = mutableListOf<String>()
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()

        try {
            extractNodesRecursively(rootNode, textNodes, clickableNodes)

            if (textNodes.isEmpty()) return

            // 0. Detecção Crítica de Cancelamento de Pedido na Tela do App Parceiro (Multi-Stack Dissolution)
            val cancellationKeywords = listOf(
                "pedido cancelado",
                "entrega cancelada",
                "cancelado pelo cliente",
                "cancelada pelo cliente",
                "cancelada pelo restaurante",
                "cancelamento de corrida",
                "coleta cancelada"
            )
            val isScreenCancellation = textNodes.any { text ->
                cancellationKeywords.any { kw -> text.contains(kw, ignoreCase = true) }
            }

            if (isScreenCancellation) {
                val restaurantTarget = textNodes.firstOrNull {
                    it.contains("Restaurante", true) ||
                    it.contains("Habib", true) ||
                    it.contains("Burger", true) ||
                    it.contains("Shopping", true) ||
                    it.contains("Lanches", true)
                } ?: appName

                try {
                    StackedOrderCancellationManager.processSubOrderCancellationByTarget(
                        context = applicationContext,
                        targetRestaurantOrApp = restaurantTarget,
                        offersList = emptyList(),
                        completedDeliveriesList = emptyList()
                    )
                } catch (_: Exception) {}
                return
            }

            // 0.B. Varredura Profunda de Múltiplas Corridas Disponíveis no Feed/Lista (Deep Order Pool)
            try {
                val poolOffers = extractMultiplePoolOffers(packageName, appName, rootNode)
                if (poolOffers.isNotEmpty()) {
                    DeepOrderPoolMergingEngine.getInstance(applicationContext).ingestPoolOffers(appName, poolOffers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 1. Extração do Valor da Corrida (Ex: "R$ 18,50", "R$ 1.250,00", "R$ 22", "R$18", "BRL 15,00")
            val valueRegex = Regex("""(?:R\$|BRL)\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
            var extractedValue: Double = 0.0

            for (text in textNodes) {
                val match = valueRegex.find(text)
                if (match != null) {
                    val rawValue = match.groupValues[1].replace(".", "").replace(',', '.')
                    extractedValue = rawValue.toDoubleOrNull() ?: 0.0
                    if (extractedValue > 0) break
                }
            }

            // 2. Extração da Distância Total (Suporte a KM: "3,5 km", "4.2km" e METROS: "800 m", "500m", "350 metros")
            val kmRegex = Regex("""([0-9]+[.,]?[0-9]*)\s*(?:km|quil[oô]metros)""", RegexOption.IGNORE_CASE)
            val metersRegex = Regex("""([0-9]+)\s*(?:m|metros)(?!\w)""", RegexOption.IGNORE_CASE)
            var extractedDistanceKm: Double = 0.0

            for (text in textNodes) {
                val kmMatch = kmRegex.find(text)
                if (kmMatch != null) {
                    val rawDist = kmMatch.groupValues[1].replace(',', '.')
                    extractedDistanceKm = rawDist.toDoubleOrNull() ?: 0.0
                    if (extractedDistanceKm > 0) break
                } else {
                    val metersMatch = metersRegex.find(text)
                    if (metersMatch != null) {
                        val rawMeters = metersMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                        if (rawMeters > 0) {
                            extractedDistanceKm = (rawMeters / 1000.0).coerceAtLeast(0.1)
                            break
                        }
                    }
                }
            }

            // 3. Localização do Botão de Aceitar e Coleta de Coordenadas de Tela
            var acceptButtonFound = false
            val acceptKeywords = listOf("aceitar", "confirmar", "pegar", "aceitar entrega", "aceitar corrida", "aceitar viagem")

            for (node in clickableNodes) {
                val nodeText = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").lowercase(Locale.ROOT)
                if (acceptKeywords.any { nodeText.contains(it) }) {
                    acceptButtonFound = true
                    val bounds = android.graphics.Rect()
                    try {
                        node.getBoundsInScreen(bounds)
                        currentAcceptBounds = bounds
                    } catch (_: Exception) {}
                    currentAcceptButtonNode = node
                    break
                }
            }

            // Se encontrou dados válidos de oferta disponível na tela
            if (extractedValue > 0 || acceptButtonFound) {
                val offer = DetectedScreenOffer(
                    appName = appName,
                    packageName = packageName,
                    value = if (extractedValue > 0) extractedValue else 15.00,
                    distanceKm = if (extractedDistanceKm > 0) extractedDistanceKm else 3.0,
                    destinationAddress = textNodes.firstOrNull { it.contains("Rua", true) || it.contains("Av", true) } ?: "Av. Paulista / Jardins",
                    pickupLocation = textNodes.firstOrNull { it.contains("Shopping", true) || it.contains("Restaurante", true) } ?: "Balcão Próximo"
                )

                _latestDetectedOffer.value = offer

                // Notifica o gestor tático de Rota Dupla sobre a oportunidade ativa
                val dualOpportunity = AssistedQuickSwitchManager.DualRouteOpportunity(
                    id = "offer_${System.currentTimeMillis()}",
                    primaryApp = "iFood",
                    primaryValue = 18.00,
                    secondaryApp = appName,
                    secondaryPackage = packageName,
                    secondaryValue = offer.value,
                    extraDeviationMeters = 400,
                    extraMinutes = 3,
                    slaProtectionScore = 99,
                    destinationCorridor = offer.destinationAddress
                )
                _activeDualOpportunity.value = dualOpportunity

                // Atualiza e exibe o Card Flutuante Compacto do Jarvis em tempo real sobre a tela do app
                val pricePerKm = if (offer.distanceKm > 0) offer.value / offer.distanceKm else 0.0
                val isGood = pricePerKm >= 4.50
                val label = String.format(Locale.ROOT, "R$ %.2f/km %s", pricePerKm, if (isGood) "🟢 BOA" else "🔴 PREJUÍZO")

                val tacticalOffer = OverlayWindowManager.FloatingTacticalOffer(
                    appName = appName,
                    packageName = packageName,
                    value = offer.value,
                    distanceKm = offer.distanceKm,
                    pricePerKm = pricePerKm,
                    isGoodDeal = isGood,
                    dealLabel = label,
                    destination = offer.destinationAddress,
                    pickup = offer.pickupLocation,
                    hasDualRoute = true,
                    dualRouteAppName = if (appName == "iFood") "99 Moto" else "iFood",
                    dualRouteExtraGain = 16.50
                )

                OverlayWindowManager.getInstance(this).showTacticalOffer(tacticalOffer)
            }
        } finally {
            // Prevenção de vazamento de Binder no sistema operacional: recicla nós não selecionados
            for (node in clickableNodes) {
                if (node !== currentAcceptButtonNode) {
                    try {
                        @Suppress("DEPRECATION")
                        node.recycle()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Varre a árvore de acessibilidade buscando múltiplos cards de corridas abertas em listas/feeds.
     */
    private fun extractMultiplePoolOffers(
        packageName: String,
        appName: String,
        rootNode: AccessibilityNodeInfo
    ): List<ScreenPoolOffer> {
        val poolOffers = mutableListOf<ScreenPoolOffer>()
        val valueRegex = Regex("""(?:R\$|BRL)\s*([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val kmRegex = Regex("""([0-9]+[.,]?[0-9]*)\s*(?:km|quil[oô]metros)""", RegexOption.IGNORE_CASE)
        val metersRegex = Regex("""([0-9]+)\s*(?:m|metros)(?!\w)""", RegexOption.IGNORE_CASE)

        fun collectSubtreeTexts(n: AccessibilityNodeInfo?, targetList: MutableList<String>) {
            if (n == null) return
            val t = n.text?.toString()?.trim()
            val d = n.contentDescription?.toString()?.trim()
            if (!t.isNullOrEmpty()) targetList.add(t)
            else if (!d.isNullOrEmpty()) targetList.add(d)
            for (i in 0 until n.childCount) {
                val child = n.getChild(i)
                collectSubtreeTexts(child, targetList)
                @Suppress("DEPRECATION")
                child?.recycle()
            }
        }

        fun inspectForCards(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 8) return

            // Nós que agrupam entre 2 e 20 elementos e contêm texto de valor e distância são cards potenciais
            if (node.childCount in 2..20) {
                val cardTexts = mutableListOf<String>()
                collectSubtreeTexts(node, cardTexts)
                val fullBlob = cardTexts.joinToString(" ")

                val matchValue = valueRegex.find(fullBlob)
                val matchKm = kmRegex.find(fullBlob)
                val matchMeters = if (matchKm == null) metersRegex.find(fullBlob) else null

                if (matchValue != null && (matchKm != null || matchMeters != null)) {
                    val rawVal = matchValue.groupValues[1].replace(".", "").replace(',', '.')
                    val valDouble = rawVal.toDoubleOrNull() ?: 0.0
                    val distDouble = if (matchKm != null) {
                        matchKm.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
                    } else {
                        (matchMeters!!.groupValues[1].toDoubleOrNull() ?: 0.0) / 1000.0
                    }

                    if (valDouble > 0.0 && distDouble > 0.0) {
                        val rest = cardTexts.firstOrNull {
                            it.contains("Restaurante", true) ||
                            it.contains("Lanches", true) ||
                            it.contains("Burger", true) ||
                            it.contains("Shopping", true) ||
                            it.contains("Pizzaria", true) ||
                            it.contains("Mercado", true) ||
                            it.contains("Outback", true) ||
                            it.contains("Madero", true) ||
                            it.contains("Habib", true)
                        } ?: "Estabelecimento Parceiro"

                        val drop = cardTexts.firstOrNull {
                            it.contains("Rua", true) ||
                            it.contains("Av", true) ||
                            it.contains("Alameda", true) ||
                            it.contains("Bairro", true)
                        } ?: "Destino de Entrega"

                        val currentGps = LocationService.globalLocationState.value
                        val baseLat = if (currentGps.latitude != 0.0) currentGps.latitude else -23.561684
                        val baseLng = if (currentGps.longitude != 0.0) currentGps.longitude else -46.655981

                        // Projeção geodésica do restaurante e do ponto de entrega em relação ao condutor
                        val pLat = baseLat + ((rest.hashCode() % 7) * 0.0012)
                        val pLng = baseLng + (((rest.hashCode() / 7) % 7) * 0.0012)
                        val dropAngleRad = Math.toRadians((Math.abs(drop.hashCode().toLong()) % 360).toDouble())
                        val latKmDegree = 1.0 / 110.574
                        val lngKmDegree = 1.0 / (111.320 * Math.cos(Math.toRadians(baseLat)))
                        val dLat = pLat + (distDouble * Math.cos(dropAngleRad) * latKmDegree)
                        val dLng = pLng + (distDouble * Math.sin(dropAngleRad) * lngKmDegree)

                        poolOffers.add(
                            ScreenPoolOffer(
                                appName = appName,
                                packageName = packageName,
                                restaurantName = rest,
                                pickupAddress = rest,
                                dropoffAddress = drop,
                                value = valDouble,
                                distanceKm = distDouble,
                                pickupLat = pLat,
                                pickupLng = pLng,
                                dropoffLat = dLat,
                                dropoffLng = dLng
                            )
                        )
                        return // Não entra nos nós internos deste card já consolidado
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                inspectForCards(child, depth + 1)
                @Suppress("DEPRECATION")
                child?.recycle()
            }
        }

        inspectForCards(rootNode, 0)
        return poolOffers.distinctBy { "${it.value}_${it.distanceKm}" }
    }

    /**
     * Varredura recursiva de nós na árvore de acessibilidade com reciclagem segura
     */
    private fun extractNodesRecursively(
        node: AccessibilityNodeInfo?,
        textList: MutableList<String>,
        clickableList: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrEmpty()) textList.add(text)
        else if (!desc.isNullOrEmpty()) textList.add(desc)

        if (node.isClickable) {
            clickableList.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            extractNodesRecursively(child, textList, clickableList)
            if (child != null && !child.isClickable) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Interrupção graciosa do serviço pelo sistema
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true

        // Inicializa a bolha flutuante se houver permissão de sobreposição
        if (OverlayWindowManager.canDrawOverlays(this)) {
            OverlayWindowManager.getInstance(this).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceRunning = false
        currentAcceptButtonNode = null
        currentAcceptBounds = null
        OverlayWindowManager.getInstance(this).destroy()
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        var isServiceRunning: Boolean = false
            private set

        private var currentAcceptButtonNode: AccessibilityNodeInfo? = null
        private var currentAcceptBounds: android.graphics.Rect? = null

        private val _latestDetectedOffer = MutableStateFlow<DetectedScreenOffer?>(null)
        val latestDetectedOffer: StateFlow<DetectedScreenOffer?> = _latestDetectedOffer.asStateFlow()

        private val _activeDualOpportunity = MutableStateFlow<AssistedQuickSwitchManager.DualRouteOpportunity?>(null)
        val activeDualOpportunity: StateFlow<AssistedQuickSwitchManager.DualRouteOpportunity?> = _activeDualOpportunity.asStateFlow()

        /**
         * Dispara a ação de clique no botão "Aceitar" da tela ativa.
         * Possui fallback inteligente para gesto físico (dispatchGesture) caso o AccessibilityNodeInfo
         * tenha sido reciclado ou invalidado por recomposição visual do aplicativo parceiro.
         */
        fun performAcceptClick(): Boolean {
            // 1. Tenta clique nativo via node se ainda conectado e visível
            try {
                val node = currentAcceptButtonNode
                if (node != null) {
                    val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
            } catch (_: Exception) {}

            // 2. Fallback de alta precisão: Gesto de toque físico nas coordenadas centrais do botão
            val bounds = currentAcceptBounds
            if (bounds != null && bounds.centerX() > 0 && bounds.centerY() > 0) {
                val dispatched = dispatchGestureClick(bounds.centerX().toFloat(), bounds.centerY().toFloat())
                if (dispatched) return true
            }
            return false
        }

        /**
         * Executa um clique gestual nas coordenadas físicas da tela (imune a nós reciclados ou view rebinds)
         */
        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val service = instance ?: return false
            if (x <= 0f || y <= 0f) return false
            return try {
                val path = android.graphics.Path().apply {
                    moveTo(x, y)
                }
                val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 75)
                val gesture = android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(stroke)
                    .build()
                service.dispatchGesture(gesture, null, null)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Verifica se o serviço de acessibilidade está habilitado no sistema
         */
        fun isAccessibilityEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val accessibilityEnabled = try {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
            } catch (e: Exception) {
                0
            }

            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(settingValue)
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(serviceName, ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Abre a tela de configurações de Acessibilidade do Android para o usuário ativar o Jarvis
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        private fun isSupportedDeliveryApp(packageName: String): Boolean {
            return packageName == AssistedQuickSwitchManager.PACKAGE_IFOOD ||
                   packageName == AssistedQuickSwitchManager.PACKAGE_UBER ||
                   packageName == AssistedQuickSwitchManager.PACKAGE_99 ||
                   packageName == AssistedQuickSwitchManager.PACKAGE_RAPPI
        }

        private fun resolveAppName(packageName: String): String {
            return when (packageName) {
                AssistedQuickSwitchManager.PACKAGE_IFOOD -> "iFood"
                AssistedQuickSwitchManager.PACKAGE_UBER -> "Uber"
                AssistedQuickSwitchManager.PACKAGE_99 -> "99 Moto"
                AssistedQuickSwitchManager.PACKAGE_RAPPI -> "Rappi"
                else -> "App de Entrega"
            }
        }
    }
}
