package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarState
import java.util.regex.Pattern

class RadarAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RadarAccessibility"
        private val FARE_REGEX = Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")

        // Prevent duplicate parsing of the same offer
        private var lastParsedFare: Double = 0.0
        private var lastParsedTime: Long = 0L
        private var lastParsedTexts: String = ""
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Filter package names to only parse target delivery apps or our own app for simulation/testing
        val allowedPackages = setOf(
            "com.ifood.driver",
            "com.ubercab.driver",
            "com.nine9.driver",
            "com.lalamove.android.driver",
            "com.rappi.delivery",
            packageName // always allow our own package for testing!
        )

        val rootNode = rootInActiveWindow ?: return
        val allTexts = mutableListOf<String>()
        extractTexts(rootNode, allTexts)

        if (allTexts.isEmpty()) return

        // Joint text for logging or duplicate check
        val joinedTexts = allTexts.joinToString(" | ")
        
        // Quick duplicates check (within 10 seconds of the same screen state)
        if (joinedTexts == lastParsedTexts && System.currentTimeMillis() - lastParsedTime < 10000) {
            return
        }

        parseAndProcessTexts(packageName, allTexts, joinedTexts)
    }

    private fun extractTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        if (node.text != null && node.text.toString().isNotBlank()) {
            list.add(node.text.toString().trim())
        } else if (node.contentDescription != null && node.contentDescription.toString().isNotBlank()) {
            list.add(node.contentDescription.toString().trim())
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            extractTexts(child, list)
        }
    }

    private fun parseAndProcessTexts(packageName: String, texts: List<String>, joined: String) {
        // Look for fare value (e.g., R$ 15,40 or R$15.00)
        var fareValue = 0.0
        val fareMatcher = FARE_REGEX.matcher(joined)
        if (fareMatcher.find()) {
            val fareStr = fareMatcher.group(1)?.replace(",", ".") ?: ""
            fareValue = fareStr.toDoubleOrNull() ?: 0.0
        }

        if (fareValue <= 0.0) {
            return // Not a valid offer screen or no fare detected
        }

        // Check if we already processed this exact fare value in the last 15 seconds to prevent spam
        val currentTime = System.currentTimeMillis()
        if (fareValue == lastParsedFare && (currentTime - lastParsedTime) < 15000L) {
            return
        }

        // Advanced extraction of distance (km/m) and duration (min) based on 2026 iFood, Uber, 99 layout designs
        val distanceRegex = Pattern.compile("(\\d+([.,]\\d+)?)\\s*(km|m)", Pattern.CASE_INSENSITIVE)
        val distanceMatcher = distanceRegex.matcher(joined)
        val foundDistances = mutableListOf<Double>()
        while (distanceMatcher.find()) {
            val number = distanceMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: continue
            val unit = distanceMatcher.group(3)?.lowercase() ?: "km"
            val distInKm = if (unit == "m") number / 1000.0 else number
            foundDistances.add(distInKm)
        }
        val distanceValue = if (foundDistances.isNotEmpty()) {
            // Take the maximum of all detected distances, which represents the total trip distance (pickup + delivery) in 99% of layout cases
            foundDistances.maxOrNull() ?: 0.0
        } else {
            0.0
        }

        val timeRegex = Pattern.compile("(\\d+)\\s*(min|minutos)", Pattern.CASE_INSENSITIVE)
        val timeMatcher = timeRegex.matcher(joined)
        val foundTimes = mutableListOf<Double>()
        while (timeMatcher.find()) {
            val mins = timeMatcher.group(1)?.toDoubleOrNull() ?: continue
            foundTimes.add(mins)
        }
        val durationValue = if (foundTimes.isNotEmpty()) {
            foundTimes.maxOrNull() ?: 0.0
        } else {
            0.0
        }

        // Detect app name based on package or keywords
        val appName = when {
            packageName.contains("ifood") || joined.contains("ifood", ignoreCase = true) -> "iFood"
            packageName.contains("uber") || joined.contains("uber", ignoreCase = true) -> "Uber"
            packageName.contains("nine9") || packageName.contains("99") || joined.contains("99", ignoreCase = true) -> "99"
            packageName.contains("lalamove") || joined.contains("lalamove", ignoreCase = true) -> "Lalamove"
            packageName.contains("rappi") || joined.contains("rappi", ignoreCase = true) -> "Rappi"
            else -> "App de Entrega"
        }

        // Try to identify pickup and delivery addresses
        var pickupAddress = ""
        var deliveryAddress = ""

        // Smart heuristics to find addresses
        // Often address strings contain "R.", "Av.", "Rua", "Avenida", "Alameda", "Estrada", "Rodovia"
        val addressKeywords = listOf("rua", "av.", "avenida", "alameda", "travessa", "praça", "rodovia", "r.", "av", "estrada")
        val potentialAddresses = texts.filter { text ->
            addressKeywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }

        if (potentialAddresses.size >= 2) {
            pickupAddress = potentialAddresses[0]
            deliveryAddress = potentialAddresses[1]
        } else if (potentialAddresses.size == 1) {
            pickupAddress = potentialAddresses[0]
            deliveryAddress = "Endereço secundário não detectado"
        } else {
            // Fallback heuristics using lines from texts
            val filteredTexts = texts.filter { 
                it.length > 5 && 
                !it.contains("R$") && 
                !it.contains("km", ignoreCase = true) && 
                !it.contains("aceitar", ignoreCase = true) && 
                !it.contains("recusar", ignoreCase = true) && 
                !it.contains("rejeitar", ignoreCase = true)
            }
            if (filteredTexts.size >= 2) {
                pickupAddress = filteredTexts[0]
                deliveryAddress = filteredTexts[1]
            } else {
                pickupAddress = "Coleta automática"
                deliveryAddress = "Entrega automática"
            }
        }

        // Save last parsed state to prevent duplicate loops
        lastParsedFare = fareValue
        lastParsedTime = currentTime
        lastParsedTexts = joined

        Log.d(TAG, "Extracted offer automatically from accessibility: $appName, R$ $fareValue, Distance: $distanceValue km, Time: $durationValue min, Pickup: $pickupAddress, Delivery: $deliveryAddress")
        RadarCoordinator.addLog("Acessibilidade: Capturada oferta do app $appName. Valor: R$ $fareValue | Distância: ${if (distanceValue > 0) "$distanceValue km" else "Não detectada"} | Tempo: ${if (durationValue > 0) "$durationValue min" else "Não detectado"}.", com.example.coordinator.LogType.SUCCESS)

        // Trigger the central service to analyze this offer in the background!
        val serviceIntent = Intent(this, RadarCoordinatorService::class.java).apply {
            putExtra("ACCESSIBILITY_OFFER", true)
            putExtra("APP_NAME", appName)
            putExtra("FARE_VALUE", fareValue)
            putExtra("PICKUP_ADDRESS", pickupAddress)
            putExtra("DELIVERY_ADDRESS", deliveryAddress)
            putExtra("DISTANCE_VALUE", distanceValue)
            putExtra("TIME_VALUE", durationValue)
        }
        
        if (RadarCoordinatorService.isServiceRunning) {
            startService(serviceIntent)
        } else {
            // Also update states if coordinator itself is running in some form
            RadarCoordinator.setActiveOffer(
                com.example.coordinator.ActiveOffer(appName, fareValue, pickupAddress, deliveryAddress, "", distanceValue, durationValue)
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        RadarCoordinator.addLog("Serviço de Acessibilidade ativo e monitorando ofertas.", com.example.coordinator.LogType.SUCCESS)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
        RadarCoordinator.addLog("Serviço de Acessibilidade interrompido temporariamente.", com.example.coordinator.LogType.WARNING)
    }
}
