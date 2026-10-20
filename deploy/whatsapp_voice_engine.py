# ==============================================================================
# JARVIS COCKPIT - MOTOR DE ÁUDIOS HUMANIZADOS NO WHATSAPP (VOICE ENGINE)
# ==============================================================================
# Envia notas de voz com simulação de "gravando áudio..." antes do disparo.
# Aumenta absurdamente a confiança de motoboys e entregadores.
# ==============================================================================

import os
import json
import logging
from typing import Dict, Any

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("VoiceEngine")

VOICE_SCRIPTS = {
    "apresentacao": {
        "title": "Apresentação do Radar com Tela Apagada",
        "duration_seconds": 18,
        "transcript": "Fala meu parceiro, tudo certo? Vi que você curte rodar protegido. O Radar avisa comando e blitz no fone de ouvido bluetooth com o celular no bolso e tela 100% apagada, sem esquentar o aparelho. Dá uma olhada no link que te mandei!"
    },
    "quebra_preco": {
        "title": "Comparativo Multa vs Assinatura",
        "duration_seconds": 22,
        "transcript": "Irmão, pensa comigo: uma blitz apreende sua moto ou te dá R$ 293 de multa gravíssima mais 7 pontos na carteira. O app custa R$ 19,90 por mês, dá menos de 70 centavos por dia. Na primeira blitz que te livrar, já pagou um ano inteiro de acesso!"
    },
    "boas_vindas": {
        "title": "Comemoração de Ativação VIP",
        "duration_seconds": 15,
        "transcript": "Pagamento confirmado com sucesso, parceiro! Sua conta VIP já tá liberada no sistema da VPS. Baixa o aplicativo no link abaixo e já pode botar pra rodar no plantão de hoje!"
    }
}

class WhatsAppVoiceEngine:
    """Simula gravação e envio de notas de voz no WhatsApp via Evolution API."""

    @staticmethod
    def get_scripts() -> Dict[str, Any]:
        return VOICE_SCRIPTS

    @staticmethod
    def simulate_voice_send(phone: str, script_key: str = "apresentacao") -> Dict[str, Any]:
        clean_phone = "".join(filter(str.isdigit, phone)) or "5511999999999"
        script = VOICE_SCRIPTS.get(script_key, VOICE_SCRIPTS["apresentacao"])
        
        logger.info(f"Simulando presença 'gravando áudio...' para {clean_phone} por {script['duration_seconds']}s")
        return {
            "success": True,
            "target_phone": clean_phone,
            "presence_simulation": "recording (Gravando áudio...)",
            "audio_format": "audio/ogg; codecs=opus (PTT WhatsApp Nota de Voz)",
            "script_title": script["title"],
            "duration": f"{script['duration_seconds']}s",
            "transcript": script["transcript"],
            "status": "ENVIADO_COMO_NOTA_DE_VOZ"
        }
