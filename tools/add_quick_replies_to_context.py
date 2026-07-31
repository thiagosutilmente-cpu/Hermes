import re

file_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'(val contextInfo = if \(offer != null\) \{[\s\S]*?\} else \{[\s\S]*?\})'

repl = """val offerText = if (offer != null) {
                        "Oferta ativa na tela: R$ ${offer.fareValue} por ${offer.totalDistance}km. Coleta: ${offer.pickupAddress}, Entrega: ${offer.deliveryAddress}. Velocidade atual: ${RadarCoordinator.currentSpeedKmh.value} km/h (Limite de segurança: $currentLimit km/h)."
                    } else {
                        "Nenhuma oferta na tela no momento. Velocidade atual: ${RadarCoordinator.currentSpeedKmh.value} km/h."
                    }
                    
                    val settings = RadarCoordinator.settings.value
                    val quickReplies = "Respostas Rápidas de Chat disponíveis: 1) '${settings.quickReply1Cmd}' -> '${settings.quickReply1Text}', 2) '${settings.quickReply2Cmd}' -> '${settings.quickReply2Text}', 3) '${settings.quickReply3Cmd}' -> '${settings.quickReply3Text}'"
                    val contextInfo = "$offerText\\n$quickReplies" """

new_content = re.sub(pattern, repl, content)

with open(file_path, 'w') as f:
    f.write(new_content)

