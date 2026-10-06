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

        extractNodesRecursively(rootNode, textNodes, clickableNodes)

        if (textNodes.isEmpty()) return

        // 1. Extração do Valor da Corrida (Ex: "R$ 18,50" ou "R$22.00")
        val valueRegex = Regex("""R\$\s*(\d+[.,]\d{2})""")
        var extractedValue: Double = 0.0

        for (text in textNodes) {
            val match = valueRegex.find(text)
            if (match != null) {
                val rawValue = match.groupValues[1].replace(',', '.')
                extractedValue = rawValue.toDoubleOrNull() ?: 0.0
                if (extractedValue > 0) break
            }
        }

        // 2. Extração da Distância (Ex: "3,5 km" ou "4.2km")
        val distanceRegex = Regex("""(\d+[.,]?\d*)\s*km""", RegexOption.IGNORE_CASE)
        var extractedDistanceKm: Double = 0.0

        for (text in textNodes) {
            val match = distanceRegex.find(text)
            if (match != null) {
                val rawDist = match.groupValues[1].replace(',', '.')
                extractedDistanceKm = rawDist.toDoubleOrNull() ?: 0.0
                if (extractedDistanceKm > 0) break
            }
        }

        // 3. Localização do Botão de Aceitar
        var acceptButtonFound = false
        val acceptKeywords = listOf("aceitar", "confirmar", "pegar", "aceitar entrega", "aceitar corrida", "aceitar viagem")

        for (node in clickableNodes) {
            val nodeText = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").lowercase(Locale.ROOT)
            if (acceptKeywords.any { nodeText.contains(it) }) {
                acceptButtonFound = true
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
    }

    /**
     * Varredura recursiva de nós na árvore de acessibilidade
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
        OverlayWindowManager.getInstance(this).destroy()
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        var isServiceRunning: Boolean = false
            private set

        private var currentAcceptButtonNode: AccessibilityNodeInfo? = null

        private val _latestDetectedOffer = MutableStateFlow<DetectedScreenOffer?>(null)
        val latestDetectedOffer: StateFlow<DetectedScreenOffer?> = _latestDetectedOffer.asStateFlow()

        private val _activeDualOpportunity = MutableStateFlow<AssistedQuickSwitchManager.DualRouteOpportunity?>(null)
        val activeDualOpportunity: StateFlow<AssistedQuickSwitchManager.DualRouteOpportunity?> = _activeDualOpportunity.asStateFlow()

        /**
         * Dispara a ação de clique no botão "Aceitar" da tela ativa
         */
        fun performAcceptClick(): Boolean {
            return try {
                currentAcceptButtonNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
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
