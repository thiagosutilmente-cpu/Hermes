import re

file_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'(val contextInfo = if \(offer != null\) \{[\s\S]*?\} else \{[\s\S]*?\})\s*(val result = com\.example\.voice\.JarvisPersonaEngine\.processCommand\(command, contextInfo\))'

interceptor = """val contextInfo = if (offer != null) {
                        "Oferta ativa na tela: R$ ${offer.fareValue} por ${offer.totalDistance}km. Coleta: ${offer.pickupAddress}, Entrega: ${offer.deliveryAddress}. Velocidade atual: ${RadarCoordinator.currentSpeedKmh.value} km/h (Limite de segurança: $currentLimit km/h)."
                    } else {
                        "Nenhuma oferta na tela no momento. Velocidade atual: ${RadarCoordinator.currentSpeedKmh.value} km/h."
                    }
                    
                    if (command.startsWith("quick_reply_")) {
                        val replyText = when (command) {
                            "quick_reply_1" -> RadarCoordinator.settings.value.quickReply1Text
                            "quick_reply_2" -> RadarCoordinator.settings.value.quickReply2Text
                            "quick_reply_3" -> RadarCoordinator.settings.value.quickReply3Text
                            else -> ""
                        }
                        if (replyText.isNotEmpty()) {
                            val intent = android.content.Intent("com.example.ACTION_AUTOFILL_CHAT")
                            intent.putExtra("MESSAGE_TEXT", replyText)
                            intent.setPackage(packageName)
                            sendBroadcast(intent)
                            speakText("Enviando resposta rápida.")
                            RadarCoordinator.addLog("Voz: Resposta rápida enviada no chat - $replyText", com.example.coordinator.LogType.SUCCESS)
                        }
                        return@launch
                    }
                    
                    val result = com.example.voice.JarvisPersonaEngine.processCommand(command, contextInfo)"""

def repl(m):
    return interceptor

new_content = re.sub(pattern, repl, content)

if new_content == content:
    print("Failed to replace!")
else:
    print("Replaced!")
    with open(file_path, 'w') as f:
        f.write(new_content)

