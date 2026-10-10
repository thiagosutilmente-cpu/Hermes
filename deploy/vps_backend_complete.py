# ==============================================================================
# JARVIS NEURAL COCKPIT - ASAAS WEBHOOK ENGINE & EVOLUTION API INTEGRATION
# ==============================================================================
# Este script roda na sua VPS Hostinger (Linux Ubuntu/Debian).
# Ele recebe webhooks do Asaas em tempo real e dispara mensagens automáticas
# no WhatsApp dos entregadores usando a Evolution API ou Z-API.
#
# INSTRUÇÕES DE INSTALAÇÃO RÁPIDA NA VPS:
# 1. Conecte na sua VPS via SSH:
#    ssh root@IP_DA_SUA_VPS
# 2. Instale os pacotes necessários:
#    pip install fastapi uvicorn requests
# 3. Crie o arquivo:
#    nano main.py
# 4. Cole este código completo, configure suas chaves abaixo e salve (Ctrl+O, Enter, Ctrl+X).
# 5. Inicie o servidor em segundo plano:
#    nohup uvicorn main:app --host 0.0.0.0 --port 8000 > jarvis_server.log 2>&1 &
# 6. Configure no painel do Asaas (Configurações > Integrações > Webhooks):
#    URL: http://IP_DA_SUA_VPS:8000/webhook/asaas
#    Eventos selecionados: PAYMENT_RECEIVED, PAYMENT_OVERDUE, PAYMENT_CREATED
# ==============================================================================

import sqlite3
import requests
import uvicorn
from fastapi import FastAPI, Request, HTTPException

# ---------------------------------------------------------
# CONFIGURAÇÕES DA EVOLUTION API (WHATSAPP)
# ---------------------------------------------------------
EVOLUTION_API_URL = "http://SEU_IP_EVOLUTION:8080" # ou https://sua-evolution.com
EVOLUTION_API_KEY = "SUA_CHAVE_GLOBAL_EVOLUTION"
INSTANCE_NAME = "jarvis_pro_bot"
APK_DOWNLOAD_URL = "https://ais-dev-pg6paszmsamfrpcpczq7n7-321882160298.us-west2.run.app"

app = FastAPI(
    title="Jarvis Neural Cockpit - Asaas Webhook Engine",
    description="Backend oficial de automação de pagamentos Asaas e disparos WhatsApp via Evolution API"
)

def init_db():
    conn = sqlite3.connect("jarvis_core.db")
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id TEXT UNIQUE,
            phone TEXT,
            status TEXT DEFAULT 'PENDING',
            plan_type TEXT,
            activation_key TEXT,
            last_payment_date TEXT
        )
    ''')
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS webhook_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event TEXT,
            customer_id TEXT,
            value REAL,
            raw_payload TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    conn.commit()
    conn.close()

init_db()

def send_evolution_whatsapp(phone: str, message: str) -> bool:
    """Dispara a mensagem formatada para o entregador via Evolution API v1/v2."""
    if not phone:
        return False

    clean_phone = "".join(filter(str.isdigit, phone))
    # Garante formato internacional DDI 55
    if len(clean_phone) in (10, 11):
        clean_phone = f"55{clean_phone}"

    endpoint = f"{EVOLUTION_API_URL}/message/sendText/{INSTANCE_NAME}"
    headers = {
        "apikey": EVOLUTION_API_KEY,
        "Content-Type": "application/json"
    }
    payload = {
        "number": clean_phone,
        "options": {
            "delay": 1200,
            "presence": "composing"
        },
        "textMessage": {
            "text": message
        }
    }

    try:
        response = requests.post(endpoint, json=payload, headers=headers, timeout=10)
        print(f"[WHATSAPP ENVIADO] Status: {response.status_code} para {clean_phone}")
        return response.status_code in (200, 201)
    except Exception as e:
        print(f"[ERRO WHATSAPP] Falha ao enviar para {clean_phone}: {e}")
        return False

@app.get("/")
def health_check():
    return {
        "status": "online",
        "system": "Jarvis Neural Cockpit Asaas Engine",
        "version": "2.4.0"
    }

@app.post("/webhook/asaas")
async def asaas_webhook(request: Request):
    """
    Endpoint principal para recepção de notificações HTTP do Asaas.
    """
    try:
        payload = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON payload")

    event = payload.get("event")
    payment = payload.get("payment", {})
    customer_id = payment.get("customer")
    value = payment.get("value", 0.0)
    customer_phone = payment.get("mobilePhone") or payment.get("phone")

    conn = sqlite3.connect("jarvis_core.db")
    cursor = conn.cursor()

    # Log do evento bruto
    cursor.execute(
        "INSERT INTO webhook_logs (event, customer_id, value, raw_payload) VALUES (?, ?, ?, ?)",
        (event, customer_id, value, str(payload))
    )

    if event == "PAYMENT_RECEIVED":
        # Ativa o cliente e gera chave única de liberação
        activation_key = f"JARVIS-PRO-{abs(hash(customer_id)) % 10000:04d}"
        cursor.execute('''
            INSERT INTO users (customer_id, phone, status, plan_type, activation_key, last_payment_date)
            VALUES (?, ?, 'ACTIVE', 'PLANO_PRO', ?, datetime('now'))
            ON CONFLICT(customer_id) DO UPDATE SET
                status = 'ACTIVE',
                activation_key = excluded.activation_key,
                last_payment_date = datetime('now')
        ''', (customer_id, customer_phone, activation_key))
        conn.commit()

        print(f"[ASAAS] Pagamento Confirmado: R$ {value} para cliente {customer_id}")

        # Mensagem Tática de Boas-Vindas & Liberação do APK
        welcome_message = (
            f"🚀 *PAGAMENTO CONFIRMADO - JARVIS NEURAL PRO* 🚀\n\n"
            f"Seu pagamento de *R$ {value:.2f}* foi processado com sucesso pelo Asaas!\n\n"
            f"🔑 *Sua Chave de Acesso Exclusiva:*\n"
            f"` {activation_key} `\n\n"
            f"📲 *Baixe ou atualize seu APK aqui:*\n"
            f"{APK_DOWNLOAD_URL}\n\n"
            f"🛡️ *Recursos Imediatos Ativados:*\n"
            f"• Análise neural e rejeição automática de corridas lixo\n"
            f"• Cálculo de lucro real e R$/KM no HUD flutuante\n"
            f"• Relatórios de combustível economizado em tempo real\n\n"
            f"Bora faturar alto com segurança!"
        )
        if customer_phone:
            send_evolution_whatsapp(customer_phone, welcome_message)

    elif event == "PAYMENT_OVERDUE":
        # Marca como vencido e aciona régua de retenção
        cursor.execute("UPDATE users SET status = 'OVERDUE' WHERE customer_id = ?", (customer_id,))
        conn.commit()

        print(f"[ASAAS] Assinatura Vencida: Cliente {customer_id}")

        retention_message = (
            f"⚠️ *AVISO IMPORTANTE - SEU ACESSO AO JARVIS PRO EXPIROU* ⚠️\n\n"
            f"Identificamos que a renovação da sua assinatura de *R$ {value:.2f}* não foi concluída.\n\n"
            f"🚨 *Atenção ao rodar sem o Jarvis:*\n"
            f"• Você voltará a aceitar corridas com taxa abaixo de R$ 2,50/KM sem perceber.\n"
            f"• O custo desnecessário de combustível pode chegar a R$ 45,00 por dia.\n"
            f"• A bolha flutuante inteligente ficará bloqueada.\n\n"
            f"💡 *Renove agora mesmo no link oficial do Asaas:*\n"
            f"https://www.asaas.com/c/{customer_id}\n\n"
            f"Assim que o PIX for pago, seu acesso volta automaticamente em menos de 1 minuto!"
        )
        if customer_phone:
            send_evolution_whatsapp(customer_phone, retention_message)

    conn.close()
    return {"status": "success", "event": event, "customer": customer_id}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
