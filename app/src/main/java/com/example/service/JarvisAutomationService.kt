package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class JarvisAutomationService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAutomation"
        var isRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Jarvis Automation Service Conectado e Ativo!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Aqui o Jarvis "lê" a tela. 
        // Podemos identificar botões de "Aceitar" pelo texto ou ID.
        val rootNode = rootInActiveWindow ?: return
        
        // Exemplo: Procurar por "Aceitar" ou "Confirmar"
        findAndClickNode(rootNode, "Aceitar")
        findAndClickNode(rootNode, "Confirmar")
        findAndClickNode(rootNode, "OK")
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo, text: String) {
        val nodes = node.findAccessibilityNodeInfosByText(text)
        for (foundNode in nodes) {
            if (foundNode.isClickable) {
                foundNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Jarvis executou clique automático em: $text")
            }
        }
    }

    // Função para clicar em coordenadas específicas (O "clicar em todo o telefone")
    fun clickAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(builder.build(), null, null)
    }

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
