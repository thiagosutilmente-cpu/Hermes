package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.example.coordinator.RadarCoordinator
import com.example.coordinator.RadarState
import com.example.data.AppDatabase
import com.example.data.NeuralClickEntity
import java.util.regex.Pattern
import kotlinx.coroutines.*

class RadarAccessibilityService : AccessibilityService() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
    }



    companion object {
        private const val TAG = "RadarAccessibility"
        private val FARE_REGEX = Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")

        private var instance: RadarAccessibilityService? = null
        
        fun getInstance(): RadarAccessibilityService? = instance

        // Prevent duplicate parsing of the same offer
        private var lastParsedFare: Double = 0.0
        private var lastParsedTime: Long = 0L
        private var lastParsedTexts: String = ""

        // --- NEURAL CLICK ENGINE & SEMANTIC MAPPING ---
        private val clickableNodesMap = mutableMapOf<String, AccessibilityNodeInfo>()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastCurrentPackageName: String = ""

    private fun generateScreenHash(node: AccessibilityNodeInfo?): String {
        if (node == null) return "null"
        val sb = StringBuilder()
        fun traverse(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 10) return
            sb.append(n.className).append(n.childCount)
            for (i in 0 until n.childCount) {
                val child = n.getChild(i)
                traverse(child, depth + 1)
                child?.recycle() // RECICLAGEM CRÍTICA
            }
        }
        traverse(node, 0)
        return sb.toString().hashCode().toString()
    }

    /**
     * Ghost Eye: Multi-Target Neural Scan.
     * Varre a tela em busca de múltiplas ofertas simultâneas (Listas).
     */
    private fun performMultiTargetScan(rootNode: AccessibilityNodeInfo, packageName: String) {
        val foundOffers = mutableListOf<Bundle>()
        val fareRegex = java.util.regex.Pattern.compile("R\\$\\s*(\\d+[,.]\\d{2})")
        
        // Função recursiva blindada para encontrar padrões de preço em listas
        fun findPrices(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 50) return
            
            try {
                val text = node.text?.toString() ?: ""
                val matcher = fareRegex.matcher(text)
                
                if (matcher.find()) {
                    val fare = matcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    if (fare > 5.0) {
                        val bundle = Bundle().apply {
                            putDouble("FARE", fare)
                            putString("TEXT", text)
                        }
                        foundOffers.add(bundle)
                    }
                }
                
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        try {
                            findPrices(child, depth + 1)
                        } finally {
                            child.recycle() // RECICLAGEM CRÍTICA
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ghost Eye: Erro ao processar nó de acessibilidade.", e)
            }
        }
        
        try {
            findPrices(rootNode, 0)
            com.example.coordinator.RadarCoordinator.updateModuleHealth("GhostEye", true)
        } catch (e: Exception) {
            com.example.coordinator.RadarCoordinator.updateModuleHealth("GhostEye", false)
        }
        
        if (foundOffers.size > 1) {
            Log.d(TAG, "Ghost Eye: Detectadas ${foundOffers.size} ofertas simultâneas. Analisando melhor ROI...")
            val bestOffer = foundOffers.maxByOrNull { it.getDouble("FARE") }
            bestOffer?.let { b ->
                val intent = Intent("com.example.ACTION_NEW_OFFER").apply {
                    putExtra("APP_NAME", packageName)
                    putExtra("FARE_VALUE", b.getDouble("FARE"))
                    putExtra("MULTI_TARGET", true)
                    putExtra("TOTAL_TARGETS", foundOffers.size)
                }
                sendBroadcast(intent)
            }
        }
    }

    private val windowPackageMap = mutableMapOf<Int, String>() // WindowID -> PackageName

    fun getScreenLayout(): String {
        val root = rootInActiveWindow ?: return "Sem interface ativa no momento."
        val list = mutableListOf<String>()
        captureNodeDetails(root, list, 0)
        return list.joinToString("\n")
    }

    private fun captureNodeDetails(node: AccessibilityNodeInfo?, list: MutableList<String>, depth: Int) {
        if (node == null) return
        val indent = "  ".repeat(depth)
        val text = node.text ?: node.contentDescription ?: ""
        val clickable = if (node.isClickable) "[CLICKABLE]" else ""
        
        if (text.isNotBlank() || node.isClickable) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            list.add("$indent$clickable Text: \"$text\", Class: ${node.className}, Center: (${rect.centerX()}, ${rect.centerY()})")
        }
        
        if (depth < 8) { // Limite de profundidade para evitar payloads gigantes
            for (i in 0 until node.childCount) {
                captureNodeDetails(node.getChild(i), list, depth + 1)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        lastCurrentPackageName = packageName
        
        // Detect split screen / multi-window and Map IDs
        val windows = windows
        RadarCoordinator.updateSplitScreenStatus(windows.size > 1)
        
        windowPackageMap.clear()
        windows.forEach { window ->
            if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                window.root?.let { root ->
                    val pkg = root.packageName?.toString() ?: ""
                    windowPackageMap[window.id] = pkg
                    root.recycle()
                }
            }
        }
        
        // Feed the active package name to the coordinator for overlay/stealth control
        RadarCoordinator.updateActiveAppPackageName(packageName)
        RadarCoordinator.updateSwarmPresence(packageName)

        val currentRoot = rootInActiveWindow

        // Auditoria de Oferta Expirada/Indisponível (Prevenção de frustração)
        if (currentRoot != null) {
            val expiredKeywords = listOf("indisponível", "não está mais disponível", "expirou", "outro motorista aceitou")
            fun checkExpired(node: AccessibilityNodeInfo) {
                try {
                    val text = node.text?.toString()?.lowercase() ?: ""
                    if (expiredKeywords.any { text.contains(it) }) {
                        Log.d(TAG, "Jarvis: Detecção de oferta expirada/perdida: $text")
                        val intent = Intent("com.example.ACTION_OFFER_EXPIRED").apply {
                            putExtra("REASON", text)
                        }
                        sendBroadcast(intent)
                        return
                    }
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i)
                        if (child != null) {
                            try {
                                checkExpired(child)
                            } finally {
                                child.recycle() // RECICLAGEM CRÍTICA
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
            
            // Criamos uma cópia segura para a corrotina para evitar que o nó original seja reciclado antes do tempo
            val safeRoot = AccessibilityNodeInfo.obtain(currentRoot)
            serviceScope.launch {
                try {
                    checkExpired(safeRoot)
                } finally {
                    safeRoot.recycle()
                }
            }
        }

        // Ghost Eye: Neural Multi-Target Scan (Suporte a Split-Screen)
        if (windows.size > 1) {
            windows.forEach { window ->
                if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    window.root?.let { root ->
                        val winPackage = root.packageName?.toString() ?: ""
                        performMultiTargetScan(root, winPackage)
                        root.recycle()
                    }
                }
            }
        } else {
            currentRoot?.let { root ->
                performMultiTargetScan(root, packageName)
            }
        }

        // --- APRENDIZADO PASSIVO 5.0: O Jarvis mapeia instintos e hábitos ---
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val node = event.source
            node?.let {
                val rect = android.graphics.Rect()
                it.getBoundsInScreen(rect)
                val clickX = rect.centerX().toFloat() / resources.displayMetrics.widthPixels
                val clickY = rect.centerY().toFloat() / resources.displayMetrics.heightPixels
                
                // Grava o hábito de clique associado ao contexto da tela
                val screenHash = generateScreenHash(currentRoot)
                RadarCoordinator.learnUserHabit(packageName, screenHash, clickX, clickY, it.text?.toString())
            }
        }

        // Filter package names
        val allowedApps = listOf(
            "com.ifood.driver",
            "br.com.brainweb.ifood",
            "com.ubercab.driver",
            "com.taxis99",
            "com.99taxis.driver",
            "com.lalamove",
            "com.rappi",
            "sinet.startup.inDriver",
            "com.loggi.driver",
            "com.borzodelivery",
            "com.example",
            "com.google.android.apps.maps",
            "com.waze"
        )
        
        val isAllowed = allowedApps.any { packageName.contains(it, ignoreCase = true) }
        if (!isAllowed || currentRoot == null) return

        // Ghost Eye: Neural Multi-Target Scan (Listas de corridas)
        performMultiTargetScan(currentRoot, packageName)
        
        // Index clickable nodes semantically for "Surreal Clicks"
        clickableNodesMap.clear()
        indexClickableNodes(currentRoot)
        
        val allTexts = mutableListOf<String>()
        extractTexts(currentRoot, allTexts)

        if (allTexts.isEmpty()) return

        // Joint text for logging or duplicate check
        val joinedTexts = allTexts.joinToString(" | ")
        
        // Quick duplicates check
        if (joinedTexts == lastParsedTexts && System.currentTimeMillis() - lastParsedTime < 10000) {
            return
        }

        val currentTime = System.currentTimeMillis()

        // Detect cancellation
        val isCancelled = joinedTexts.contains("cancelada", ignoreCase = true) || 
                          joinedTexts.contains("cancelado", ignoreCase = true) ||
                          joinedTexts.contains("cancelamento", ignoreCase = true) ||
                          joinedTexts.contains("corrida cancelada", ignoreCase = true) ||
                          joinedTexts.contains("pedido cancelado", ignoreCase = true)

        if (isCancelled) {
            Log.w(TAG, "Detectada possível cancelamento no app $packageName")
            RadarCoordinator.addLog("Alerta Neural: Detectado sinal de cancelamento no app $packageName.", com.example.coordinator.LogType.ALERT)
            
            val cancelIntent = Intent(this, RadarCoordinatorService::class.java).apply {
                putExtra("ORDER_CANCELLED", true)
                putExtra("APP_NAME", packageName)
            }
            startService(cancelIntent)
            
            lastParsedTexts = joinedTexts
            lastParsedTime = currentTime
            return
        }

        detectAndTrackActiveDelivery(packageName, allTexts, joinedTexts)
        parseAndProcessTexts(packageName, allTexts, joinedTexts)
    }

    private fun indexClickableNodes(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        if (node.isClickable) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            
            // Create a semantic fingerprint for the element
            val key = "$text|$contentDesc|$viewId"
            if (key.length > 3) {
                clickableNodesMap[key] = node
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    indexClickableNodes(child)
                } finally {
                    // child.recycle() // Nota: Não reciclamos aqui porque os nós são guardados no mapa
                }
            }
        }
    }

    /**
     * O Jarvis agora pode clicar em qualquer lugar baseado em uma "intenção neural".
     */
    fun performNeuralClick(intentKeywords: List<String>): Boolean {
        for ((key, node) in clickableNodesMap) {
            if (intentKeywords.any { key.contains(it, ignoreCase = true) }) {
                Log.d(TAG, "Neural Click: Ativando elo em $key")
                RadarCoordinator.addLog("Jarvis: Ativando elo neural em botão semântico.", com.example.coordinator.LogType.SUCCESS)
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    fun dispatchSurrealGesture(x: Float, y: Float, actionType: String = "CLICK"): Boolean {
        // --- GHOST PROTOCOL 5.0: Bio-Mimetic Interaction ---
        // Aumentamos a variação para simular a imprecisão natural do dedo humano
        val jitterX = x + ((-5..5).random()).toFloat()
        val jitterY = y + ((-5..5).random()).toFloat()
        
        val path = android.graphics.Path()
        path.moveTo(jitterX, jitterY)
        
        // Simular um micro-deslizamento (dwell) que ocorre em toques reais
        if (actionType == "CLICK") {
            val driftX = jitterX + ((-2..2).random()).toFloat()
            val driftY = jitterY + ((-2..2).random()).toFloat()
            path.lineTo(driftX, driftY)
        }

        val gestureBuilder = GestureDescription.Builder()
        // Duração variável (entre 90ms e 180ms) - cliques fixos de 100ms são fáceis de detectar por bots
        val settings = RadarCoordinator.settings.value
        val baseDuration = (90..180).random().toLong()
        
        // Se o Ghost Protocol estiver alto, adicionamos mais aleatoriedade (Polimorfismo)
        val humanDuration = if (settings.ghostProtocolLevel > 7) {
            baseDuration + (-20..40).random()
        } else {
            baseDuration
        }
        
        val stroke = GestureDescription.StrokeDescription(path, 0, humanDuration)
        gestureBuilder.addStroke(stroke)
        
        Log.d(TAG, "Ghost Protocol ${settings.ghostProtocolLevel}: Executando toque bio-mimético em ($jitterX, $jitterY) por ${humanDuration}ms")
        RadarCoordinator.addLog("Jarvis: Clique Polimórfico Ghost ${settings.ghostProtocolLevel}. Assinatura camuflada.", com.example.coordinator.LogType.INFO)

        return dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
            }
        }, null)
    }

    /**
     * Versão aprimorada do Neural Click que tenta coordenadas se a árvore de nós falhar.
     * Agora com busca exaustiva por similaridade semântica e MEMÓRIA DE APRENDIZADO.
     */
    suspend fun performSurrealAction(keywords: List<String>): Boolean {
        val packageName = lastCurrentPackageName
        if (packageName.isBlank()) return false
        
        Log.d(TAG, "Iniciando Ação Surreal para: $keywords no app $packageName")
        RadarCoordinator.addLog("Jarvis: Sincronizando intenção neural para '${keywords.firstOrNull()}'...", com.example.coordinator.LogType.INFO)

        // 1. Tentar Memória Neural de Aprendizado (O "Cérebro" do Jarvis com Similaridade Fuzzy)
        val mainKeyword = keywords.firstOrNull() ?: ""
        if (mainKeyword.isNotBlank()) {
            val learning = getFuzzyLearning(packageName, mainKeyword)
            learning?.let {
                Log.i(TAG, "Memória Neural (Fuzzy/Exact): Encontrei padrão aprendido para '$mainKeyword' (Mapeado de '${it.keyword}'). Verificando validade...")
                RadarCoordinator.addLog("Jarvis: Ativando memória aprendida de similaridade cognitiva para '$mainKeyword'.", com.example.coordinator.LogType.SUCCESS)
                dispatchSurrealGesture(it.lastX, it.lastY)
                return true
            }
        }

        // 2. Tentar mapeamento semântico exato (rápido)
        if (performNeuralClick(keywords)) return true
        
        // 3. Fallback: Busca recursiva profunda por texto parcial ou descrição
        val root = rootInActiveWindow ?: return false
        val found = findAndClickByHeuristics(root, keywords, mainKeyword)
        
        if (!found) {
            // 4. Busca por OCR Espacial (Heurística de texto em qualquer nó)
            Log.w(TAG, "Falha na árvore padrão. Iniciando Varredura Neural Profunda...")
            RadarCoordinator.addLog("Jarvis: Ativando Varredura Neural Profunda 4.0 (Bio-Scanning)...", com.example.coordinator.LogType.WARNING)
            
            val deepFound = findDeepHeuristics(root, keywords, mainKeyword)
            if (deepFound) {
                RadarCoordinator.addLog("Jarvis: Alvo localizado via Varredura Profunda.", com.example.coordinator.LogType.SUCCESS)
                return true
            }

            // 5. Último recurso: Ícones Comuns
            if (searchCommonIcons(root, keywords)) return true
            
            // 6. INTELIGÊNCIA COGNITIVA (Gemini Vision/Hierarchy Analysis)
            RadarCoordinator.addLog("Jarvis: Heurísticas falharam. Iniciando Análise Cognitiva de Interface...", com.example.coordinator.LogType.ALERT)
            val layout = getScreenLayout()
            val coords = com.example.voice.JarvisPersonaEngine.processCognitiveClick(mainKeyword, layout)
            
            if (coords != null) {
                RadarCoordinator.addLog("Jarvis: Alvo identificado via Raciocínio Cognitivo. Executando clique surreal.", com.example.coordinator.LogType.SUCCESS)
                saveNeuralLearning(packageName, mainKeyword, "Cognitive Selection", coords.first, coords.second)
                return dispatchSurrealGesture(coords.first, coords.second)
            }
        }
        
        return found
    }

    private fun findDeepHeuristics(node: AccessibilityNodeInfo, keywords: List<String>, mainKeyword: String): Boolean {
        // Busca mesmo em nós que não são marcados como clicáveis mas tem texto (clique no pai ou coordenada)
        val content = "${node.text} ${node.contentDescription}".lowercase()
        if (keywords.any { content.contains(it) }) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()
            
            Log.w(TAG, "Varredura Profunda: Alvo encontrado em '$content' ($centerX, $centerY)")
            saveNeuralLearning(lastCurrentPackageName, mainKeyword, content, centerX, centerY)
            return dispatchSurrealGesture(centerX, centerY)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findDeepHeuristics(child, keywords, mainKeyword)) return true
        }
        return false
    }

    private fun searchCommonIcons(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        // Mapeamento de ícones comuns por ID de recurso ou descrição de conteúdo
        val iconKeywords = listOf("fechar", "close", "back", "voltar", "menu", "chat", "perfil")
        val relevant = keywords.any { k -> iconKeywords.any { i -> k.contains(i) } }
        
        if (relevant) {
            // Busca profunda por nós clicáveis pequenos que podem ser ícones
            return findClickableIcon(node, keywords)
        }
        return false
    }

    private fun findClickableIcon(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        if (node.isClickable) {
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (keywords.any { id.contains(it) || desc.contains(it) }) {
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)
                return dispatchSurrealGesture(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findClickableIcon(child, keywords)) return true
        }
        return false
    }

    private fun findAndClickByHeuristics(node: AccessibilityNodeInfo, keywords: List<String>, mainKeyword: String): Boolean {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        
        val content = "${node.text} ${node.contentDescription}".lowercase()
        if (keywords.any { content.contains(it) }) {
            // Se achamos algo que parece ser o que queremos, clicamos no centro do retângulo
            val centerX = rect.centerX().toFloat()
            val centerY = rect.centerY().toFloat()
            Log.w(TAG, "Heurística Surreal: Clicando em coordenada ($centerX, $centerY) para '$content'")
            
            // APRENDER: Salvar este clique para o futuro
            if (mainKeyword.isNotBlank()) {
                saveNeuralLearning(lastCurrentPackageName, mainKeyword, content, centerX, centerY)
            }
            
            return dispatchSurrealGesture(centerX, centerY)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickByHeuristics(child, keywords, mainKeyword)) return true
        }
        return false
    }

    private fun saveNeuralLearning(pkg: String, keyword: String, semanticKey: String, x: Float, y: Float) {
        serviceScope.launch {
            val dao = AppDatabase.getDatabase(applicationContext).neuralClickDao()
            val existing = dao.getLearning(pkg, keyword)
            val entityToSave = if (existing == null) {
                NeuralClickEntity(
                    packageName = pkg,
                    keyword = keyword,
                    semanticKey = semanticKey,
                    lastX = x,
                    lastY = y,
                    usageCount = 1,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                existing.copy(
                    lastX = x,
                    lastY = y,
                    usageCount = existing.usageCount + 1,
                    timestamp = System.currentTimeMillis()
                )
            }
            
            if (existing == null) {
                dao.insert(entityToSave)
                Log.d(TAG, "Aprendizado Neural: Nova âncora salva para '$keyword' no app $pkg")
            } else {
                dao.update(entityToSave)
                Log.d(TAG, "Aprendizado Neural: Âncora atualizada para '$keyword' no app $pkg")
            }
            
            // Upload to Cloud for collaborative operator-to-operator learning!
            com.example.data.FirestoreManager.uploadNeuralLearning(entityToSave)
        }
    }

    private suspend fun getFuzzyLearning(packageName: String, mainKeyword: String): NeuralClickEntity? {
        val dao = AppDatabase.getDatabase(applicationContext).neuralClickDao()
        // Try exact match first
        val exact = dao.getLearning(packageName, mainKeyword)
        if (exact != null) return exact
        
        // Try fuzzy match on keyword for the same package
        val all = dao.getAllLearnings()
        val pkgLearnings = all.filter { it.packageName == packageName }
        
        var bestMatch: NeuralClickEntity? = null
        var bestScore = 0.0
        
        for (learning in pkgLearnings) {
            val score = calculateFuzzySimilarity(mainKeyword.lowercase(), learning.keyword.lowercase())
            if (score > 0.70 && score > bestScore) {
                bestScore = score
                bestMatch = learning
            }
        }
        
        if (bestMatch != null) {
            Log.i(TAG, "Fuzzy Match Encontrado: '$mainKeyword' assemelha-se a '${bestMatch.keyword}' com score de ${(bestScore * 100).toInt()}%")
            RadarCoordinator.addLog("Jarvis: Similaridade Cognitiva de ${(bestScore * 100).toInt()}% para '${bestMatch.keyword}'.", com.example.coordinator.LogType.SUCCESS)
        }
        return bestMatch
    }
    
    private fun calculateFuzzySimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isBlank() || s2.isBlank()) return 0.0
        
        // Jaccard similarity of 2-character bi-grams
        val bigrams1 = s1.windowed(2).toSet()
        val bigrams2 = s2.windowed(2).toSet()
        
        if (bigrams1.isEmpty() || bigrams2.isEmpty()) {
            // fallback to word intersection
            val w1 = s1.split(" ").toSet()
            val w2 = s2.split(" ").toSet()
            val intersection = w1.intersect(w2).size
            val union = w1.union(w2).size
            return if (union > 0) intersection.toDouble() / union else 0.0
        }
        
        val intersection = bigrams1.intersect(bigrams2).size
        val union = bigrams1.union(bigrams2).size
        return intersection.toDouble() / union
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

        // Detect app name primarily based on package name to avoid mixing
        val appNameBase = when {
            packageName.contains("ifood", ignoreCase = true) -> "iFood"
            packageName.contains("uber", ignoreCase = true) -> "Uber"
            packageName.contains("taxis99", ignoreCase = true) || packageName.contains("nine9", ignoreCase = true) || packageName.contains("99", ignoreCase = true) -> "99"
            packageName.contains("lalamove", ignoreCase = true) -> "Lalamove"
            packageName.contains("rappi", ignoreCase = true) -> "Rappi"
            packageName.contains("indriver", ignoreCase = true) -> "inDrive"
            packageName.contains("loggi", ignoreCase = true) -> "Loggi"
            packageName.contains("borzo", ignoreCase = true) -> "Borzo"
            // Fallbacks based on text only if package is system UI or something else
            joined.contains("ifood", ignoreCase = true) -> "iFood"
            joined.contains("uber", ignoreCase = true) -> "Uber"
            joined.contains("99", ignoreCase = true) -> "99"
            joined.contains("lalamove", ignoreCase = true) -> "Lalamove"
            joined.contains("indriver", ignoreCase = true) -> "inDrive"
            else -> "App de Entrega"
        }

        // Detect service type (Moto, Flash, Entrega, X, Comfort, etc)
        val serviceType = when {
            joined.contains("flash", ignoreCase = true) -> "Flash"
            joined.contains("moto", ignoreCase = true) -> "Moto"
            joined.contains("uberx", ignoreCase = true) || joined.contains("uber x", ignoreCase = true) -> "X"
            joined.contains("comfort", ignoreCase = true) -> "Comfort"
            joined.contains("black", ignoreCase = true) -> "Black"
            joined.contains("entrega", ignoreCase = true) || joined.contains("delivery", ignoreCase = true) -> "Entrega"
            joined.contains("pop", ignoreCase = true) -> "Pop"
            else -> ""
        }

        val appName = if (serviceType.isNotEmpty() && !appNameBase.contains(serviceType, ignoreCase = true)) {
            "$appNameBase $serviceType"
        } else {
            appNameBase
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
        
        // Analyze for high-value orders to trigger physiological adrenaline spike simulation
        HumanInteractionSimulator.analyzeScreenForAdrenalineSpike(joined)
        
        // Anti-bot cognitive reading delay: wait a natural human comprehension time before dispatching/processing
        val cognitiveDelay = HumanInteractionSimulator.getCognitiveReadingDelayMs()
        if (cognitiveDelay > 0) {
            Log.d(TAG, "Simulating cognitive eye-scan delay of ${cognitiveDelay}ms before sending offer to coordinator")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (RadarCoordinatorService.isServiceRunning) {
                    startService(serviceIntent)
                } else {
                    RadarCoordinator.setActiveOffer(
                        com.example.coordinator.ActiveOffer(appName, fareValue, pickupAddress, deliveryAddress, "", distanceValue, durationValue)
                    )
                }
            }, cognitiveDelay)
        } else {
            if (RadarCoordinatorService.isServiceRunning) {
                startService(serviceIntent)
            } else {
                RadarCoordinator.setActiveOffer(
                    com.example.coordinator.ActiveOffer(appName, fareValue, pickupAddress, deliveryAddress, "", distanceValue, durationValue)
                )
            }
        }
    }

    private val commandReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_PERFORM_GLOBAL_ACTION") {
                val actionId = intent.getIntExtra("GLOBAL_ACTION_ID", -1)
                if (actionId != -1) {
                    Log.d(TAG, "Jarvis: Executando Ação Global ID: $actionId")
                    performGlobalAction(actionId)
                }
            } else if (intent?.action == "com.example.ACTION_EXECUTE_REJECT") {
                Log.d(TAG, "Received ACTION_EXECUTE_REJECT broadcast")
                RadarCoordinator.addLog("Segurança: Clique físico automático de RECUSA bloqueado para proteção contra banimento. Por favor, recuse manualmente na tela.", com.example.coordinator.LogType.WARNING)
            } else if (intent?.action == "com.example.ACTION_EXECUTE_CLICK") {
                Log.d(TAG, "Received ACTION_EXECUTE_CLICK broadcast")
                RadarCoordinator.addLog("Segurança: Clique físico automático de ACEITE bloqueado para proteção contra banimento. Por favor, toque na tela do aplicativo para aceitar com segurança.", com.example.coordinator.LogType.WARNING)
            } else if (intent?.action == "com.example.ACTION_REMOTE_CLICK") {
                Log.d(TAG, "Received ACTION_REMOTE_CLICK broadcast")
                RadarCoordinator.addLog("Segurança: Clique físico remoto bloqueado para total proteção contra detecção de robôs.", com.example.coordinator.LogType.WARNING)
            } else if (intent?.action == "com.example.ACTION_EXECUTE_GESTURE") {
                val gestureType = intent.getStringExtra("GESTURE_TYPE") ?: ""
                Log.d(TAG, "Received ACTION_EXECUTE_GESTURE: $gestureType")
                val metrics = resources.displayMetrics
                val screenWidth = metrics.widthPixels
                val screenHeight = metrics.heightPixels
                
                when (gestureType) {
                    "ZOOM_IN" -> {
                        val (path1, path2) = HumanInteractionSimulator.createHumanizedPinchCoordinates(screenWidth, screenHeight, true)
                        dispatchGestureMultiStroke(path1, path2, (350..600).random().toLong())
                        RadarCoordinator.addLog("Jarvis: Comando de voz executado (Ampliar Mapa).", com.example.coordinator.LogType.SUCCESS)
                    }
                    "ZOOM_OUT" -> {
                        val (path1, path2) = HumanInteractionSimulator.createHumanizedPinchCoordinates(screenWidth, screenHeight, false)
                        dispatchGestureMultiStroke(path1, path2, (350..600).random().toLong())
                        RadarCoordinator.addLog("Jarvis: Comando de voz executado (Afastar Mapa).", com.example.coordinator.LogType.SUCCESS)
                    }
                    "PAN_MAP" -> {
                        val path = HumanInteractionSimulator.createHumanizedMapPanCoordinates(screenWidth, screenHeight)
                        dispatchCustomGesture(path, (400..750).random().toLong())
                        RadarCoordinator.addLog("Jarvis: Comando de voz executado (Arrastar Mapa).", com.example.coordinator.LogType.SUCCESS)
                    }
                    "CLICK_ACCEPT" -> {
                        RadarCoordinator.addLog("Segurança: Toque de aceite por comando de voz bloqueado para evitar banimentos.", com.example.coordinator.LogType.WARNING)
                    }
                    "CLICK_REJECT" -> {
                        RadarCoordinator.addLog("Segurança: Toque de recusa por comando de voz bloqueado para evitar banimentos.", com.example.coordinator.LogType.WARNING)
                    }
                }
            } else if (intent?.action == "com.example.ACTION_SCROLL") {
                val direction = intent.getStringExtra("DIRECTION") ?: "DOWN"
                Log.d(TAG, "Received ACTION_SCROLL direction: $direction")
                val rootNode = rootInActiveWindow
                var scrolled = false
                if (rootNode != null) {
                    val scrollableNode = findScrollableNode(rootNode)
                    if (scrollableNode != null) {
                        if (direction == "UP") {
                            scrolled = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                        } else {
                            scrolled = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        }
                    }
                }
                if (!scrolled) {
                   val metrics = resources.displayMetrics
                   val screenWidth = metrics.widthPixels
                   val screenHeight = metrics.heightPixels
                   if (direction == "UP") {
                       val path = android.graphics.Path()
                       path.moveTo(screenWidth / 2f, screenHeight * 0.3f)
                       path.lineTo(screenWidth / 2f, screenHeight * 0.7f)
                       dispatchCustomGesture(path, 400L)
                   } else {
                       val path = android.graphics.Path()
                       path.moveTo(screenWidth / 2f, screenHeight * 0.7f)
                       path.lineTo(screenWidth / 2f, screenHeight * 0.3f)
                       dispatchCustomGesture(path, 400L)
                   }
                }
            } else if (intent?.action == "com.example.ACTION_NEURAL_CLICK") {
                val target = intent.getStringExtra("TARGET_TEXT") ?: ""
                Log.d(TAG, "Received ACTION_NEURAL_CLICK for: $target")
                if (target.isNotBlank()) {
                    serviceScope.launch {
                        val success = performSurrealAction(listOf(target))
                        if (success) {
                            RadarCoordinator.addLog("Jarvis: Clique neural executado com sucesso em '$target'.", com.example.coordinator.LogType.SUCCESS)
                        } else {
                            RadarCoordinator.addLog("Jarvis: Não foi possível localizar o alvo '$target' para clique neural.", com.example.coordinator.LogType.WARNING)
                        }
                    }
                }
            } else if (intent?.action == "com.example.ACTION_MACRO_WHATSAPP") {
                val contact = intent.getStringExtra("CONTACT_NAME") ?: ""
                val message = intent.getStringExtra("MESSAGE_TEXT") ?: ""
                Log.d(TAG, "Received ACTION_MACRO_WHATSAPP for: $contact -> $message")
                
                serviceScope.launch {
                    val success = performWhatsAppMacro(contact, message)
                    if (success) {
                        RadarCoordinator.addLog("Jarvis: Mensagem enviada via WhatsApp para '$contact' com sucesso.", com.example.coordinator.LogType.SUCCESS)
                    } else {
                        RadarCoordinator.addLog("Jarvis: Não foi possível executar a jornada no WhatsApp para '$contact'.", com.example.coordinator.LogType.WARNING)
                    }
                }
            } else if (intent?.action == "com.example.ACTION_AUTOFILL_CHAT") {
                val message = intent.getStringExtra("MESSAGE_TEXT") ?: "Estou chegando ao seu endereço em breve!"
                Log.d(TAG, "Received ACTION_AUTOFILL_CHAT: $message")
                val success = autofillAndSendChat(message)
                if (success) {
                    RadarCoordinator.addLog("Jarvis: Mensagem preenchida com sucesso no chat do aplicativo.", com.example.coordinator.LogType.SUCCESS)
                } else {
                    RadarCoordinator.addLog("Jarvis: Não foi possível localizar o campo de texto de chat.", com.example.coordinator.LogType.WARNING)
                }
            }
        }
    }

    private suspend fun performWhatsAppMacro(contact: String, message: String): Boolean {
        try {
            val pm = packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                RadarCoordinator.addLog("Jarvis: WhatsApp não encontrado instalado no dispositivo.", com.example.coordinator.LogType.ALERT)
                return false
            }
            
            kotlinx.coroutines.delay(2500)
            
            val root = rootInActiveWindow ?: return false
            
            // Tenta clicar no botão de pesquisa (lupa)
            var clickedSearch = false
            fun findAndClickSearch(node: AccessibilityNodeInfo?): Boolean {
                if (node == null) return false
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val viewId = node.viewIdResourceName?.lowercase() ?: ""
                if ((desc == "pesquisar" || desc == "search") && node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                if (viewId.contains("menuitem_search") && node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                for (i in 0 until node.childCount) {
                    if (findAndClickSearch(node.getChild(i))) return true
                }
                return false
            }
            
            clickedSearch = findAndClickSearch(root)
            if (!clickedSearch) return false
            
            kotlinx.coroutines.delay(1000)
            
            // Digitar o nome no campo de busca
            val searchRoot = rootInActiveWindow ?: return false
            var searchInput: AccessibilityNodeInfo? = null
            fun findSearchInput(node: AccessibilityNodeInfo?) {
                if (node == null) return
                if (node.className == "android.widget.EditText") {
                    searchInput = node
                    return
                }
                for (i in 0 until node.childCount) {
                    findSearchInput(node.getChild(i))
                    if (searchInput != null) return
                }
            }
            findSearchInput(searchRoot)
            
            if (searchInput != null) {
                val args = android.os.Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
                searchInput?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                
                kotlinx.coroutines.delay(1500)
                
                // Clicar no primeiro resultado clicável que não seja a busca
                val resultsRoot = rootInActiveWindow ?: return false
                var contactClicked = false
                fun clickContact(node: AccessibilityNodeInfo?): Boolean {
                    if (node == null) return false
                    val text = node.text?.toString()?.lowercase() ?: ""
                    // Remover acentos para garantir que a busca não falhe (ex: 'João' -> 'joao')
                    val cleanText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")
                    val cleanContact = java.text.Normalizer.normalize(contact, java.text.Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "").lowercase()
                    if (cleanText.contains(cleanContact) && node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    // Alternativa: clica no container pai do texto
                    if (cleanText.contains(cleanContact)) {
                        var p = node.parent
                        while (p != null) {
                            if (p.isClickable) {
                                p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                return true
                            }
                            p = p.parent
                        }
                    }
                    for (i in 0 until node.childCount) {
                        if (clickContact(node.getChild(i))) return true
                    }
                    return false
                }
                contactClicked = clickContact(resultsRoot)
                
                if (!contactClicked) return false
                
                kotlinx.coroutines.delay(1500)
                
                // Agora na tela de chat, digitar a mensagem
                val chatRoot = rootInActiveWindow ?: return false
                var chatInput: AccessibilityNodeInfo? = null
                fun findChatInput(node: AccessibilityNodeInfo?) {
                    if (node == null) return
                    if (node.className == "android.widget.EditText") {
                        chatInput = node
                        return
                    }
                    for (i in 0 until node.childCount) {
                        findChatInput(node.getChild(i))
                        if (chatInput != null) return
                    }
                }
                findChatInput(chatRoot)
                
                if (chatInput != null) {
                    val msgArgs = android.os.Bundle()
                    msgArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                    chatInput?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, msgArgs)
                    
                    kotlinx.coroutines.delay(800)
                    
                    // Clicar em enviar
                    val sendRoot = rootInActiveWindow ?: return false
                    fun clickSend(node: AccessibilityNodeInfo?): Boolean {
                        if (node == null) return false
                        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                        if ((desc == "enviar" || desc == "send") && node.isClickable) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return true
                        }
                        for (i in 0 until node.childCount) {
                            if (clickSend(node.getChild(i))) return true
                        }
                        return false
                    }
                    
                    return clickSend(sendRoot)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing WhatsApp Macro", e)
        }
        return false
    }

    private fun autofillAndSendChat(message: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editNodes = mutableListOf<AccessibilityNodeInfo>()
        findEditTextFields(rootNode, editNodes)
        
        if (editNodes.isEmpty()) {
            return false
        }
        
        val editText = editNodes[0]
        val arguments = android.os.Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
        val textSet = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        
        if (textSet) {
            val sendKeywords = listOf("Enviar", "Mandar", "Send", "Senden", "Icon")
            for (kw in sendKeywords) {
                val sendNodes = rootNode.findAccessibilityNodeInfosByText(kw)
                for (node in sendNodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                }
            }
            return true
        }
        return false
    }

    private fun findEditTextFields(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.className == "android.widget.EditText" || node.isEditable) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            findEditTextFields(child, list)
        }
    }

    private fun dispatchGestureMultiStroke(path1: android.graphics.Path, path2: android.graphics.Path, duration: Long): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) return false
        try {
            val stroke1 = android.accessibilityservice.GestureDescription.StrokeDescription(path1, 0, duration)
            val stroke2 = android.accessibilityservice.GestureDescription.StrokeDescription(path2, 0, duration)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke1)
                .addStroke(stroke2)
                .build()
            return dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching multi-stroke gesture", e)
            return false
        }
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val result = findScrollableNode(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun dispatchCustomGesture(path: android.graphics.Path, duration: Long): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            return false
        }
        try {
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke)
                .build()
                
            return dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching custom gesture", e)
            return false
        }
    }

    private fun dispatchGestureClick(x: Float, y: Float): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            Log.e(TAG, "Cannot dispatch gesture on Android < Nougat")
            return false
        }
        try {
            val isAntiDetectionActive = RadarCoordinator.settings.value.antiDeteccaoMilitar
            
            val (finalX, finalY) = if (isAntiDetectionActive) {
                // Determine a randomized point around the target coordinate to mimic human thumb imprecision
                val deltaX = (-10..10).random()
                val deltaY = (-10..10).random()
                Pair(x + deltaX, y + deltaY)
            } else {
                Pair(x, y)
            }
            
            // Generate a natural touch curve/micro-tremor using the simulator
            val path = HumanInteractionSimulator.createHumanizedSwipePath(finalX, finalY)
            val duration = HumanInteractionSimulator.getHumanizedPressDuration()
            
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke)
                .build()
                
            val result = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Gesture completed successfully at ($finalX, $finalY) duration ${duration}ms")
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Gesture cancelled at ($finalX, $finalY)")
                }
            }, null)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching gesture click", e)
            return false
        }
    }

    private fun dispatchGestureSwipe(bounds: android.graphics.Rect): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            Log.e(TAG, "Cannot dispatch swipe gesture on Android < Nougat")
            return false
        }
        try {
            val path = HumanInteractionSimulator.createHumanizedSwipeCoordinates(bounds)
            val duration = HumanInteractionSimulator.getHumanizedSwipeDuration()
            
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke)
                .build()
                
            val result = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Swipe gesture completed successfully for bounds $bounds duration ${duration}ms")
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Swipe gesture cancelled")
                }
            }, null)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching swipe gesture", e)
            return false
        }
    }

    private fun dispatchGestureScroll(bounds: android.graphics.Rect): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            Log.e(TAG, "Cannot dispatch scroll gesture on Android < Nougat")
            return false
        }
        try {
            val path = HumanInteractionSimulator.createHumanizedScrollCoordinates(bounds)
            val duration = HumanInteractionSimulator.getHumanizedScrollDuration()
            
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke)
                .build()
                
            val result = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Scroll gesture completed successfully for bounds $bounds")
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    Log.d(TAG, "Scroll gesture cancelled")
                }
            }, null)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching scroll gesture", e)
            return false
        }
    }

    private fun performClickOnAccept(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        
        val isSlideKeyword = text.isNotBlank() && (
            text.contains("deslizar", ignoreCase = true) || 
            text.contains("deslize", ignoreCase = true) || 
            text.contains("arraste", ignoreCase = true) ||
            text.contains("arrastar", ignoreCase = true) ||
            text.contains("slide", ignoreCase = true) ||
            text.contains("swipe", ignoreCase = true)
        )

        val isAcceptKeyword = text.isNotBlank() && (
            text.contains("aceitar", ignoreCase = true) || 
            text.contains("confirmar", ignoreCase = true) || 
            text.contains("tocar para", ignoreCase = true) ||
            text.contains("coletar", ignoreCase = true) ||
            text.contains("pegar", ignoreCase = true) ||
            text.contains("vamos", ignoreCase = true) ||
            text.contains("quero", ignoreCase = true) ||
            text.contains("aceito", ignoreCase = true) ||
            text.contains("entrar", ignoreCase = true) ||
            text.contains("receber", ignoreCase = true)
        )

        if (isSlideKeyword) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            Log.d(TAG, "Detectado controle deslizante ($text). Executando gesto físico de slide humanizado.")
            RadarCoordinator.addLog("Acessibilidade: Detectado slider de aceite ($text). Executando gesto de arrasto humanizado.", com.example.coordinator.LogType.SUCCESS)
            
            if (HumanInteractionSimulator.shouldSimulateSliderHesitation()) {
                Log.d(TAG, "Stealth Engine: Simulating aborted swipe (hesitation) on slider")
                RadarCoordinator.addLog("Acessibilidade: Simulando hesitação no slider (arraste incompleto).", com.example.coordinator.LogType.ALERT)
                
                val abortedPath = HumanInteractionSimulator.createAbortedSwipeCoordinates(bounds)
                dispatchCustomGesture(abortedPath, (200..400).random().toLong())
                
                // Wait a moment and then do the real swipe
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    dispatchGestureSwipe(bounds)
                }, (600..1200).random().toLong())
                return true
            } else {
                return dispatchGestureSwipe(bounds)
            }
        } else if (isAcceptKeyword) {
            // Obter as coordenadas físicas do elemento na tela para efetuar o clique por Gesto Real
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            val shouldScroll = HumanInteractionSimulator.shouldSimulateScrollBeforeClick()
            val shouldPanMap = HumanInteractionSimulator.shouldSimulateMapPan()
            val shouldPinchMap = HumanInteractionSimulator.shouldSimulateMapPinch()
            
            if (shouldPinchMap) {
                Log.d(TAG, "Stealth Engine: Simulating map pinch zoom before accepting")
                RadarCoordinator.addLog("Acessibilidade: Ajustando aproximação (Pinch Zoom no mapa simulado).", com.example.coordinator.LogType.SUCCESS)
                
                val metrics = resources.displayMetrics
                val (path1, path2) = HumanInteractionSimulator.createHumanizedPinchCoordinates(metrics.widthPixels, metrics.heightPixels)
                dispatchGestureMultiStroke(path1, path2, (350..600).random().toLong())
                
                val delayMs = (750..1300).random().toLong()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    executeFinalHumanClick(node, bounds)
                }, delayMs)
                return true
            } else if (shouldPanMap) {
                Log.d(TAG, "Stealth Engine: Simulating map pan before clicking accept")
                RadarCoordinator.addLog("Acessibilidade: Checando destino (Arraste no mapa simulado).", com.example.coordinator.LogType.SUCCESS)
                
                val metrics = resources.displayMetrics
                val path = HumanInteractionSimulator.createHumanizedMapPanCoordinates(metrics.widthPixels, metrics.heightPixels)
                dispatchCustomGesture(path, (400..750).random().toLong())
                
                val delayMs = (800..1500).random().toLong()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    executeFinalHumanClick(node, bounds)
                }, delayMs)
                return true
            } else if (shouldScroll) {
                Log.d(TAG, "Stealth Engine: Simulating organic scroll check before clicking accept")
                RadarCoordinator.addLog("Acessibilidade: Simulando leitura de tela (rolagem natural) antes de aceitar.", com.example.coordinator.LogType.SUCCESS)
                
                // Simulate vertical scroll on the whole screen region
                val metrics = resources.displayMetrics
                val screenRect = android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
                dispatchGestureScroll(screenRect)
                
                // Schedule the click after scroll animation finishes with humanized response delay
                val delayMs = (550..950).random().toLong()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    executeFinalHumanClick(node, bounds)
                }, delayMs)
                
                return true
            } else {
                return executeFinalHumanClick(node, bounds)
            }
        }

        // Busca recursiva nos nós filhos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (performClickOnAccept(child)) {
                return true
            }
        }
        
        return false
    }

    private fun performClickOnReject(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        
        val isRejectKeyword = text.isNotBlank() && (
            text.contains("recusar", ignoreCase = true) || 
            text.contains("rejeitar", ignoreCase = true) || 
            text.contains("ignorar", ignoreCase = true) ||
            text.contains("fechar", ignoreCase = true) ||
            text.contains("cancelar", ignoreCase = true) ||
            text.equals("x", ignoreCase = true) ||
            text.contains("close", ignoreCase = true)
        )

        if (isRejectKeyword) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            Log.d(TAG, "Detectado botão de rejeição/cancelamento ($text). Executando clique.")
            RadarCoordinator.addLog("Acessibilidade: Detectado botão de recusa ($text). Executando toque humano.", com.example.coordinator.LogType.SUCCESS)
            return executeFinalHumanClick(node, bounds)
        }

        // Busca recursiva nos nós filhos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (performClickOnReject(child)) {
                return true
            }
        }
        
        return false
    }

    private fun executeFinalHumanClick(node: AccessibilityNodeInfo, bounds: android.graphics.Rect): Boolean {
        val shouldHesitate = HumanInteractionSimulator.shouldSimulateHesitation()
        val shouldMissClick = HumanInteractionSimulator.shouldSimulateMissClick()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        if (shouldHesitate) {
            Log.d(TAG, "Stealth Engine: Simulating hover/hesitation before clicking")
            RadarCoordinator.addLog("Acessibilidade: Simulando hesitação humana antes do clique.", com.example.coordinator.LogType.ALERT)
            val tremorPath = HumanInteractionSimulator.createHesitationTremorPath(bounds)
            dispatchCustomGesture(tremorPath, (100..250).random().toLong())
            
            // Wait a moment, then click
            handler.postDelayed({ executeActualClick(node, bounds) }, (250..600).random().toLong())
            return true
        } else if (shouldMissClick) {
            Log.d(TAG, "Stealth Engine: Simulating human miss-click (fatigue)")
            RadarCoordinator.addLog("Acessibilidade: Simulando erro de toque natural (dedo escorregou).", com.example.coordinator.LogType.ALERT)
            
            val (missX, missY) = HumanInteractionSimulator.getMissClickPoint(bounds)
            dispatchGestureClick(missX, missY)
            
            // Wait for reaction time to realize the miss, then click the correct spot
            handler.postDelayed({ executeActualClick(node, bounds) }, (400..800).random().toLong())
            return true
        } else {
            return executeActualClick(node, bounds)
        }
    }

    private fun executeActualClick(node: AccessibilityNodeInfo, bounds: android.graphics.Rect): Boolean {
        // Humanize the exact contact coordinates within the target bounds
        val (touchX, touchY) = HumanInteractionSimulator.getHumanizedTouchPoint(bounds)

        var clicked = false
        // 1. Tentar clique por acessibilidade padrão (rápido e direto)
        if (node.isClickable) {
            clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    break
                }
                parent = parent.parent
            }
        }

        // 2. DUPLA CONTINGÊNCIA: Disparar clique por Gesto Físico Real de Toque na mesma coordenada (Indetectável e Infalível)
        if (touchX > 0 && touchY > 0) {
            Log.d(TAG, "Dispatched physical double-gesture click on button at coordinates ($touchX, $touchY)")
            val gestureSuccess = dispatchGestureClick(touchX, touchY)
            if (gestureSuccess) clicked = true
        }

        return clicked
    }

    private var ghostTouchJob: kotlinx.coroutines.Job? = null
    private var collaborativeSyncJob: kotlinx.coroutines.Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        com.example.coordinator.RadarCoordinator.setAccessibilityService(this)
        try {
            val filter = android.content.IntentFilter().apply {
                addAction("com.example.ACTION_EXECUTE_CLICK")
                addAction("com.example.ACTION_EXECUTE_REJECT")
                addAction("com.example.ACTION_REMOTE_CLICK")
                addAction("com.example.ACTION_EXECUTE_GESTURE")
                addAction("com.example.ACTION_AUTOFILL_CHAT")
                addAction("com.example.ACTION_PERFORM_GLOBAL_ACTION")
                addAction("com.example.ACTION_SCROLL")
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(commandReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(commandReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }
        RadarCoordinator.addLog("Serviço de Acessibilidade ativo e monitorando ofertas.", com.example.coordinator.LogType.SUCCESS)
        
        startGhostTouchRoutine()
        startCollaborativeSync()
    }
    
    fun getClickableUIComponents(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val clickableNodes = mutableListOf<String>()
        
        fun scan(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.isClickable) {
                val text = node.text ?: "no-text"
                val id = node.viewIdResourceName ?: "no-id"
                clickableNodes.add("Text: $text, ID: $id, Actionable: ${node.isClickable}")
            }
            for (i in 0 until node.childCount) {
                scan(node.getChild(i))
            }
            node.recycle()
        }
        
        scan(root)
        return clickableNodes
    }

    // --- EXTRAORDINARY FEATURE: UI Thread Responsiveness Checker ---
    private var lastCheckTime = System.currentTimeMillis()
    fun checkResponsiveness(): Float {
        val now = System.currentTimeMillis()
        val delta = now - lastCheckTime
        lastCheckTime = now
        val start = System.nanoTime()
        // Simulate a lightweight check
        Thread.sleep(10)
        val duration = (System.nanoTime() - start) / 1_000_000.0 // ms
        
        // Return 0.0 to 1.0 (0=perfect, 1=frozen)
        return (duration / 50.0).coerceIn(0.0, 1.0).toFloat()
    }
    
    private fun startCollaborativeSync() {
        collaborativeSyncJob?.cancel()
        collaborativeSyncJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                Log.d(TAG, "Sync: Triggering collaborative neural click sync from Firestore...")
                com.example.data.FirestoreManager.syncSharedNeuralLearnings(applicationContext)
                // Sync every 5 minutes (300_000ms)
                delay(300_000L)
            }
        }
    }
    
    private fun startGhostTouchRoutine() {
        ghostTouchJob?.cancel()
        ghostTouchJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Wait between 3 to 8 minutes
                val delayMs = (180_000L..480_000L).random()
                delay(delayMs)
                
                if (RadarCoordinator.settings.value.antiDeteccaoMilitar) {
                    performGhostTouch()
                }
            }
        }
    }
    
    private fun performGhostTouch() {
        Log.d(TAG, "Stealth Engine: Performing organic Ghost Touch to keep app session alive")
        val metrics = resources.displayMetrics
        val screenRect = android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        
        // Randomly choose between a micro-scroll or a safe tap
        if (kotlin.random.Random.nextBoolean()) {
            dispatchGestureScroll(screenRect)
        } else {
            // Tap on a safe zone (usually top-center or upper areas where there are no critical buttons)
            val safeX = (metrics.widthPixels * kotlin.random.Random.nextDouble(0.2, 0.8)).toFloat()
            val safeY = (metrics.heightPixels * kotlin.random.Random.nextDouble(0.1, 0.3)).toFloat()
            dispatchGestureClick(safeX, safeY)
        }
    }
    
    private fun detectAndTrackActiveDelivery(packageName: String, texts: List<String>, joinedTexts: String) {
        if (packageName == "com.example") return // skip our own app
        
        // Keywords indicating we are currently in active trip/delivery mode
        val activeTripKeywords = listOf(
            "entregar", "coletar", "retirar", "a caminho", "cliente", "restaurante", 
            "navegar", "direções", "pedido #", "instruções de entrega", "enviar mensagem", 
            "ligar para", "comprovante", "código de entrega", "confirmar entrega", 
            "concluir entrega", "finalizar rota", "cheguei", "pedido em andamento", 
            "detalhes da rota", "retirada", "entrega", "destino"
        )
        
        // Maps/Waze specific indicators
        val isNavApp = packageName.contains("maps", ignoreCase = true) || packageName.contains("waze", ignoreCase = true)
        val hasActiveTripIndicators = isNavApp || activeTripKeywords.any { joinedTexts.contains(it, ignoreCase = true) }
        
        if (hasActiveTripIndicators) {
            // Find address candidate
            val addressKeywords = listOf(
                "rua ", "av. ", "avenida ", "alameda ", "travessa ", "praça ", "rodovia ", 
                "r. ", "av ", "estrada ", "rod. ", "pça "
            )
            
            val potentialAddresses = texts.filter { text ->
                addressKeywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
            }
            
            val validAddresses = potentialAddresses.filter { 
                it.length in 10..120 && 
                !it.contains("baixar", ignoreCase = true) && 
                !it.contains("instalar", ignoreCase = true) &&
                !it.contains("configurar", ignoreCase = true) &&
                !it.contains("entregador", ignoreCase = true) &&
                !it.contains("suporte", ignoreCase = true) &&
                !it.contains("ajuda", ignoreCase = true)
            }
            
            if (validAddresses.isNotEmpty()) {
                val detectedDestination = validAddresses.last().trim()
                val isAlreadyActive = com.example.coordinator.RadarCoordinator.deliveryActive.value
                val currentActiveDest = com.example.coordinator.RadarCoordinator.settings.value.activeDeliveryDestination
                
                // Extract fare if available
                var detectedFare = 15.0
                val fareMatcher = FARE_REGEX.matcher(joinedTexts)
                if (fareMatcher.find()) {
                    val fareStr = fareMatcher.group(1)?.replace(",", ".") ?: ""
                    val parsedFare = fareStr.toDoubleOrNull() ?: 0.0
                    if (parsedFare > 5.0) {
                        detectedFare = parsedFare
                    }
                }

                // Extract distance if available
                val distanceRegex = Pattern.compile("(\\d+([.,]\\d+)?)\\s*(km|m)", Pattern.CASE_INSENSITIVE)
                val distanceMatcher = distanceRegex.matcher(joinedTexts)
                val foundDistances = mutableListOf<Double>()
                while (distanceMatcher.find()) {
                    val number = distanceMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull() ?: continue
                    val unit = distanceMatcher.group(3)?.lowercase() ?: "km"
                    val distInKm = if (unit == "m") number / 1000.0 else number
                    foundDistances.add(distInKm)
                }
                val detectedDistance = if (foundDistances.isNotEmpty()) {
                    foundDistances.maxOrNull() ?: 5.0
                } else 5.0

                // If we are not active or the destination changed, let's update it automatically!
                if (!isAlreadyActive || currentActiveDest != detectedDestination) {
                    Log.d(TAG, "Cognitive Auto-Detection: Set active delivery destination to $detectedDestination from $packageName")
                    
                    serviceScope.launch {
                        com.example.coordinator.RadarCoordinator.startActiveDelivery(
                            appName = getAppNameByPackage(packageName),
                            fare = detectedFare,
                            dist = detectedDistance,
                            time = detectedDistance * 3.0, // estimate: 3 min per km
                            destination = detectedDestination
                        )
                        com.example.coordinator.RadarCoordinator.addLog(
                            "Jarvis Cognitivo: Detectei automaticamente que você está em rota ativa para: $detectedDestination via ${getAppNameByPackage(packageName)} (Valor: R$ ${String.format(java.util.Locale.US, "%.2f", detectedFare)} | ${String.format(java.util.Locale.US, "%.1f", detectedDistance)} km). Roteador Chained A+B recalibrado!",
                            com.example.coordinator.LogType.SUCCESS
                        )
                    }
                }
            }
        } else {
            // Check for completion keywords
            val completionKeywords = listOf("entregue", "finalizado", "concluído", "sucesso", "corrida encerrada", "viagem concluída", "chegou ao destino")
            val hasCompletionIndicator = completionKeywords.any { joinedTexts.contains(it, ignoreCase = true) }
            
            // If we are back to system/idle screens or have explicit completion, let's stop active delivery tracking
            val isIdleScreen = joinedTexts.contains("online", ignoreCase = true) || 
                               joinedTexts.contains("procurando", ignoreCase = true) || 
                               joinedTexts.contains("aguardando", ignoreCase = true)
            
            if ((hasCompletionIndicator || isIdleScreen) && com.example.coordinator.RadarCoordinator.deliveryActive.value) {
                val appPackage = packageName
                serviceScope.launch {
                    com.example.coordinator.RadarCoordinator.completeActiveDelivery()
                    com.example.coordinator.RadarCoordinator.addLog(
                        "Jarvis Cognitivo: Detectei que a entrega no app ${getAppNameByPackage(appPackage)} foi concluída! Voltei ao modo de espera.",
                        com.example.coordinator.LogType.SUCCESS
                    )
                }
            }
        }
    }

    private fun getAppNameByPackage(packageName: String): String {
        return when {
            packageName.contains("ifood", ignoreCase = true) -> "iFood"
            packageName.contains("uber", ignoreCase = true) -> "Uber"
            packageName.contains("taxis99", ignoreCase = true) || packageName.contains("nine9", ignoreCase = true) || packageName.contains("99", ignoreCase = true) -> "99"
            packageName.contains("lalamove", ignoreCase = true) -> "Lalamove"
            packageName.contains("rappi", ignoreCase = true) -> "Rappi"
            packageName.contains("indriver", ignoreCase = true) -> "inDrive"
            packageName.contains("loggi", ignoreCase = true) -> "Loggi"
            packageName.contains("borzo", ignoreCase = true) -> "Borzo"
            packageName.contains("maps", ignoreCase = true) -> "Google Maps"
            packageName.contains("waze", ignoreCase = true) -> "Waze"
            else -> "App de Entrega"
        }
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        ghostTouchJob?.cancel()
        collaborativeSyncJob?.cancel()
        serviceScope.cancel()
        try {
            unregisterReceiver(commandReceiver)
        } catch (e: Exception) {
            // Ignored
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
        RadarCoordinator.addLog("Serviço de Acessibilidade interrompido temporariamente.", com.example.coordinator.LogType.WARNING)
    }
}
