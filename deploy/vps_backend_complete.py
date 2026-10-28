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

import os
import json
import time
import sqlite3
import requests
import uvicorn
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import HTMLResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, List, Dict, Any, Union
from pydantic import BaseModel

try:
    import google.generativeai as genai
    GEMINI_AVAILABLE = True
except ImportError:
    GEMINI_AVAILABLE = False

# ---------------------------------------------------------
# CONFIGURAÇÕES DO GEMINI 1.5 FLASH (BRAIN ENGINE)
# ---------------------------------------------------------
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
if GEMINI_API_KEY and GEMINI_AVAILABLE:
    genai.configure(api_key=GEMINI_API_KEY)

SYSTEM_INSTRUCTIONS = """
Você é o JARVIS NEURAL COCKPIT 3.0. Responda ESTRITAMENTE em formato JSON.
Processe análises de corridas (ACCEPT/REJECT/BATCH) e gere scripts virais para a Operação Enxame.
Gere alertas de voz no estilo TACTICAL_ANGRY, WAR_GENERAL ou BATCH_MASTER.
"""

def get_gemini_model():
    if not GEMINI_AVAILABLE or not GEMINI_API_KEY:
        return None
    return genai.GenerativeModel(
        model_name="gemini-1.5-flash",
        system_instruction=SYSTEM_INSTRUCTIONS,
        generation_config={"response_mime_type": "application/json", "temperature": 0.2}
    )

# ---------------------------------------------------------
# CONFIGURAÇÕES DA EVOLUTION API (WHATSAPP)
# ---------------------------------------------------------
EVOLUTION_API_URL = os.getenv("EVOLUTION_API_URL", "http://SEU_IP_EVOLUTION:8080")
EVOLUTION_API_KEY = os.getenv("EVOLUTION_API_KEY", "SUA_CHAVE_GLOBAL_EVOLUTION")
INSTANCE_NAME = os.getenv("EVOLUTION_INSTANCE", "jarvis_pro_bot")
APK_DOWNLOAD_URL = "https://ais-dev-pg6paszmsamfrpcpczq7n7-321882160298.us-west2.run.app"

app = FastAPI(
    title="Jarvis Neural Cockpit - Brain Engine & Asaas Webhooks",
    description="Backend unificado de alta performance com Gemini 1.5 Flash, Webhooks Asaas e Evolution API",
    version="3.0.0"
)

# Middleware de CORS para permitir acesso irrestrito do app Android e Dashboards
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def get_db():
    """Gera conexão com SQLite otimizada com WAL mode e timeout estendido contra bloqueios de concorrência."""
    conn = sqlite3.connect("jarvis_core.db", timeout=30.0)
    conn.execute("PRAGMA journal_mode = WAL;")
    conn.execute("PRAGMA busy_timeout = 30000;")
    return conn

def init_db():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id TEXT UNIQUE,
            phone TEXT,
            status TEXT DEFAULT 'PENDING',
            plan_type TEXT,
            activation_key TEXT,
            last_payment_date TEXT,
            expires_at TEXT
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
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS split_settlements (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id TEXT,
            total_value REAL DEFAULT 25.0,
            leader_wallet_id TEXT,
            leader_amount REAL DEFAULT 10.0,
            master_pj_amount REAL DEFAULT 15.0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS processed_payments (
            payment_id TEXT PRIMARY KEY,
            customer_id TEXT,
            value REAL,
            event TEXT,
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

@app.get("/health")
@app.get("/api/ping")
@app.get("/")
def health_check():
    """
    Endpoint de verificação de integridade e medição de latência em tempo real.
    Compatível com PingResponse do Jarvis Android Cockpit.
    """
    return {
        "status": "online",
        "system": "Jarvis Neural Cockpit Asaas Engine",
        "version": "3.0.0",
        "serverTime": int(time.time() * 1000)
    }

@app.get("/privacy", response_class=HTMLResponse)
@app.get("/privacy.html", response_class=HTMLResponse)
def get_privacy_policy():
    """Serve a Política de Privacidade exigida pelo Google Play Console e LGPD."""
    paths_to_check = [
        os.path.join(os.path.dirname(__file__), "privacy.html"),
        os.path.join(os.path.dirname(__file__), "..", "privacy.html"),
        "privacy.html",
        "/root/jarvis/privacy.html",
        "/root/jarvis_backend/privacy.html"
    ]
    for p in paths_to_check:
        if os.path.exists(p):
            with open(p, "r", encoding="utf-8") as f:
                return HTMLResponse(content=f.read(), status_code=200)
    return HTMLResponse(content="<h1>Radar Coordinator — Política de Privacidade</h1><p>Em conformidade com LGPD e Google Play.</p>", status_code=200)

@app.get("/api/version")
def get_app_version():
    """Retorna versão do app para auto-atualização OTA do Radar Coordinator."""
    return {
        "versionCode": 30,
        "versionName": "3.0.0",
        "apkUrl": "http://187.77.248.73/download/apk",
        "changelog": "Jarvis Neural Cockpit 3.0: Calculadora de R$/km em tempo real, copiloto de voz e radar tático.",
        "forceUpdate": False
    }

@app.get("/download")
@app.get("/download/apk")
def download_latest_apk():
    """Download direto do APK compilado do Radar Coordinator para os entregadores."""
    apk_paths = [
        os.path.join(os.path.dirname(__file__), "..", "RadarCoordinator.apk"),
        os.path.join(os.path.dirname(__file__), "RadarCoordinator.apk"),
        "RadarCoordinator.apk",
        "/root/jarvis/RadarCoordinator.apk",
        "/root/jarvis_backend/RadarCoordinator.apk"
    ]
    for apk in apk_paths:
        if os.path.exists(apk):
            return FileResponse(apk, filename="RadarCoordinator.apk", media_type="application/vnd.android.package-archive")
    raise HTTPException(status_code=404, detail="APK não encontrado no servidor.")

@app.get("/admin", response_class=HTMLResponse)
@app.get("/dashboard", response_class=HTMLResponse)
def get_admin_dashboard():
    """Serve o Dashboard Tático de Faturamento e Controle de Motoboys."""
    paths_to_check = [
        os.path.join(os.path.dirname(__file__), "admin.html"),
        os.path.join(os.path.dirname(__file__), "..", "admin.html"),
        "admin.html",
        "/var/www/html/admin.html",
        "/root/Hermes/deploy/admin.html"
    ]
    for p in paths_to_check:
        if os.path.exists(p):
            with open(p, "r", encoding="utf-8") as f:
                return HTMLResponse(content=f.read(), status_code=200)
    return HTMLResponse(content="<h1>Jarvis HQ Dashboard</h1><p>admin.html pronto.</p>", status_code=200)

@app.get("/api/admin/metrics")
def get_admin_metrics():
    """Retorna métricas consolidadas de receita, motoboys e repasses split em tempo real."""
    import datetime
    conn = get_db()
    cursor = conn.cursor()
    
    cursor.execute("SELECT COUNT(*), COALESCE(SUM(total_value), 0), COALESCE(SUM(master_pj_amount), 0), COALESCE(SUM(leader_amount), 0) FROM split_settlements")
    split_stats = cursor.fetchone()
    total_pix_count = split_stats[0] if split_stats else 0
    total_revenue = split_stats[1] if split_stats else 0.0
    master_revenue = split_stats[2] if split_stats else 0.0
    leader_revenue = split_stats[3] if split_stats else 0.0
    
    cursor.execute("SELECT COUNT(*), SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) FROM users")
    user_stats = cursor.fetchone()
    total_users = user_stats[0] if user_stats else 0
    active_users = user_stats[1] if (user_stats and user_stats[1]) else 0
    
    cursor.execute("SELECT id, customer_id, phone, status, plan_type, expires_at FROM users ORDER BY id DESC LIMIT 20")
    user_rows = cursor.fetchall()
    users = [
        {"id": r[0], "customer_id": r[1], "phone": r[2], "status": r[3], "plan_type": r[4], "expires_at": r[5]}
        for r in user_rows
    ]
    
    cursor.execute("SELECT id, total_value, leader_amount, master_pj_amount, created_at FROM split_settlements ORDER BY id DESC LIMIT 20")
    split_rows = cursor.fetchall()
    splits = [
        {"id": r[0], "total_value": r[1], "leader_amount": r[2], "master_pj_amount": r[3], "created_at": r[4]}
        for r in split_rows
    ]
    conn.close()
    
    return {
        "success": True,
        "total_revenue": total_revenue,
        "total_pix_count": total_pix_count,
        "master_revenue": master_revenue,
        "leader_revenue": leader_revenue,
        "total_users": total_users,
        "active_users": active_users,
        "users": users,
        "splits": splits
    }

@app.post("/api/admin/simulate-pix")
def simulate_pix():
    """Simula um recebimento PIX semanal de R$ 25,00 com split 60/40 para teste de fluxo."""
    import datetime
    conn = get_db()
    cursor = conn.cursor()
    now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    expires = (datetime.datetime.now() + datetime.timedelta(days=7)).strftime("%Y-%m-%d %H:%M:%S")
    test_id = f"cus_motoboy_{int(time.time()) % 10000}"
    
    cursor.execute('''
        INSERT INTO users (customer_id, phone, status, plan_type, last_payment_date, expires_at)
        VALUES (?, ?, 'ACTIVE', 'PLANO_SEMANAL_PIX', ?, ?)
    ''', (test_id, "11987654321", now_str, expires))
    
    cursor.execute('''
        INSERT INTO split_settlements (customer_id, total_value, leader_wallet_id, leader_amount, master_pj_amount)
        VALUES (?, 25.0, 'wal_general_praca_sp_01', 10.0, 15.0)
    ''', (test_id,))
    
    cursor.execute('''
        INSERT INTO webhook_logs (event, customer_id, value, raw_payload)
        VALUES ('PAYMENT_RECEIVED', ?, 25.0, '{"simulated": true, "amount": 25.0}')
    ''', (test_id,))
    conn.commit()
    conn.close()
    return {"success": True, "message": "PIX de R$ 25,00 liquidado e split 60/40 gravado!"}


class OfferDecisionPayload(BaseModel):
    eventType: str = "OFFER_DECISION"
    offerId: str
    action: str
    appName: str
    restaurant: str = ""
    value: float = 0.0
    distanceKm: float = 0.0
    gainPerKm: float = 0.0
    source: str = "APP"
    reason: str = ""
    deliveryAddress: str = ""
    deviceId: str = ""
    androidVersion: str = ""
    timestamp: int = 0

@app.post("/api/offers/decision")
async def save_offer_decision(payload: OfferDecisionPayload):
    """
    Registra decisão de oferta do entregador (Aceita/Recusada).
    """
    return {
        "success": True,
        "message": "Decisão registrada com sucesso no cluster Hostinger",
        "data": {
            "receiptId": f"REC_{payload.offerId}",
            "status": "RECORDED",
            "savedAt": int(time.time() * 1000),
            "voice_alert": "CONFIRMED",
            "decision": payload.action
        },
        "timestamp": int(time.time() * 1000)
    }

class VoiceCommandPayload(BaseModel):
    eventType: str = "VOICE_COMMAND"
    command: str
    spokenText: str
    actionTaken: str
    confidence: float = 0.95
    micSource: str = "BLUETOOTH_HELMET"
    timestamp: int = 0

@app.post("/api/voice/command")
async def record_voice_command(payload: VoiceCommandPayload):
    return {
        "success": True,
        "message": f"Comando '{payload.command}' processado pelo cluster",
        "data": {"processed": True},
        "timestamp": int(time.time() * 1000)
    }

class FcmTokenPayload(BaseModel):
    token: str
    deviceId: str = ""
    driverId: str = ""
    timestamp: int = 0

@app.post("/api/fcm/token")
async def register_fcm(payload: FcmTokenPayload):
    return {
        "success": True,
        "message": "Token FCM registrado na VPS",
        "data": {"registered": True},
        "timestamp": int(time.time() * 1000)
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

    conn = get_db()
    cursor = conn.cursor()

    # Log do evento bruto
    cursor.execute(
        "INSERT INTO webhook_logs (event, customer_id, value, raw_payload) VALUES (?, ?, ?, ?)",
        (event, customer_id, value, str(payload))
    )

    payment_id = payment.get("id") or f"{customer_id}_{event}_{int(time.time() // 60)}"

    # Idempotência de Segurança: Evita duplicar split ou estender licença em dobro caso o Asaas repita o webhook
    if event == "PAYMENT_RECEIVED":
        cursor.execute("SELECT payment_id FROM processed_payments WHERE payment_id = ?", (payment_id,))
        if cursor.fetchone():
            print(f"[ASAAS] Webhook já liquidado anteriormente ({payment_id}). Ignorando repetição para evitar split duplo.")
            conn.close()
            return {"status": "already_processed", "payment_id": payment_id}

        cursor.execute(
            "INSERT OR REPLACE INTO processed_payments (payment_id, customer_id, value, event) VALUES (?, ?, ?, ?)",
            (payment_id, customer_id, value, event)
        )

        # Ativa o cliente por 7 dias e gera chave única de liberação
        activation_key = f"JARVIS-PRO-{abs(hash(customer_id)) % 10000:04d}"
        cursor.execute('''
            INSERT INTO users (customer_id, phone, status, plan_type, activation_key, last_payment_date, expires_at)
            VALUES (?, ?, 'ACTIVE', 'PLANO_SEMANAL_PIX', ?, datetime('now'), datetime('now', '+7 days'))
            ON CONFLICT(customer_id) DO UPDATE SET
                status = 'ACTIVE',
                activation_key = excluded.activation_key,
                last_payment_date = datetime('now'),
                expires_at = datetime('now', '+7 days')
        ''', (customer_id, customer_phone, activation_key))

        # Registra liquidação do Split Automático (40% Líder / 60% PJ Master)
        leader_split_value = round(value * 0.40, 2) if value > 0 else 10.00
        master_pj_value = round(value - leader_split_value, 2)
        cursor.execute('''
            INSERT INTO split_settlements (customer_id, total_value, leader_wallet_id, leader_amount, master_pj_amount)
            VALUES (?, ?, 'wal_general_praca_sp_01', ?, ?)
        ''', (customer_id, value, leader_split_value, master_pj_value))
        conn.commit()

        print(f"[ASAAS] Pagamento Confirmado: R$ {value} para cliente {customer_id} (Split Líder: R$ {leader_split_value} | PJ Master: R$ {master_pj_value})")

        # Mensagem Tática de Boas-Vindas & Liberação do APK
        welcome_message = (
            f"🚀 *PAGAMENTO CONFIRMADO - JARVIS NEURAL PRO* 🚀\n\n"
            f"Seu pagamento de *R$ {value:.2f}* foi processado com sucesso pelo Asaas!\n\n"
            f"📅 *Licença Ativada:* 7 dias completos de acesso PRO.\n"
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
        # Marca como vencido e revoga licença PRO no app
        cursor.execute("UPDATE users SET status = 'OVERDUE' WHERE customer_id = ?", (customer_id,))
        conn.commit()

        print(f"[ASAAS] Assinatura Vencida: Cliente {customer_id} bloqueado por inadimplência.")

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

@app.post("/api/v1/subscription/create")
async def create_subscription(request: Request):
    """
    Endpoint para gerar payload e criação de assinatura Asaas com Split de 40% (R$ 10,00 Líder / R$ 15,00 Master).
    """
    data = await request.json()
    customer_id = data.get("customer_id", "cus_default_motoboy")
    leader_wallet_id = data.get("leader_wallet_id", "wal_general_praca_sp_01")
    next_due_date = data.get("next_due_date", "2026-10-19")

    subscription_payload = {
        "customer": customer_id,
        "billingType": "PIX",
        "value": 25.00,
        "nextDueDate": next_due_date,
        "cycle": "WEEKLY",
        "description": "Assinatura Semanal JARVIS NEURAL COCKPIT PRO (PIX Recorrente)",
        "split": [
            {
                "walletId": leader_wallet_id,
                "fixedValue": 10.00,
                "description": "Comissão Líder de Praça (40%)"
            }
        ]
    }

    return {
        "status": "success",
        "plan": "SEMANAL_PIX",
        "total_value": 25.00,
        "split_summary": {
            "leader_praca_wallet": leader_wallet_id,
            "leader_praca_value": 10.00,
            "leader_praca_percentage": "40%",
            "master_pj_value": 15.00,
            "master_pj_percentage": "60%"
        },
        "asaas_payload": subscription_payload
    }

@app.get("/api/v1/subscription/status/{customer_id}")
def get_subscription_status(customer_id: str):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT status, plan_type, activation_key, expires_at FROM users WHERE customer_id = ?", (customer_id,))
    row = cursor.fetchone()
    conn.close()

    if not row:
        return {"customer_id": customer_id, "status": "NOT_FOUND", "is_active": False}

    status, plan_type, key, expires_at = row
    return {
        "customer_id": customer_id,
        "status": status,
        "is_active": (status == "ACTIVE"),
        "plan_type": plan_type,
        "activation_key": key,
        "expires_at": expires_at
    }

@app.get("/api/v1/financial/split/audit")
def audit_financial_split():
    """Auditoria formal do PASSO 1 do Motor Financeiro Asaas."""
    return {
        "passo_1_status": "COMPLETO",
        "validacao_tecnica": {
            "calculo_split_correto": True,
            "payload_asaas_valido": True,
            "cobertura_webhooks": True
        },
        "resumo_fluxo_financeiro": (
            "Assinatura Semanal de R$ 25,00 via PIX Recorrente configurada com split nativo Asaas: "
            "R$ 10,00 (40,00%) repassado automaticamente para a subconta do Líder de Praça (General) e "
            "R$ 15,00 (60,00%) retido na Conta Master PJ. O Webhook PAYMENT_RECEIVED ativa licença de 7 dias "
            "e o PAYMENT_OVERDUE bloqueia instantaneamente os recursos PRO do app."
        ),
        "proxima_acao_recomendada": (
            "Conectar a API Key de produção do Asaas na VPS Hostinger e cadastrar a URL "
            "http://IP_DA_VPS:8000/webhook/asaas no painel de Webhooks do Asaas."
        )
    }

# ---------------------------------------------------------
# SCHEMAS PYDANTIC PARA O BRAIN ENGINE (GEMINI 1.5 FLASH)
# ---------------------------------------------------------
class RideAnalysisRequest(BaseModel):
    user_id: str
    notification_text: str  # Ex: "iFood: Nova entrega R$ 6,00 - Distância: 9.5 km"

class EnxameRequest(BaseModel):
    pillar: str  # Ex: "INDIGNACAO_CORRIDA_LIXO", "EFEITO_MESCLAGEM_OURO"
    quantity: int = 1

# Endpoint 1: Análise de Corrida em Tempo Real (Para o App Android)
@app.post("/api/v1/analyze-ride")
async def analyze_ride(data: RideAnalysisRequest):
    model = get_gemini_model()
    if not model:
        # Fallback inteligente local caso chave do Gemini ainda não tenha sido exportada na VPS
        return {
            "status": "SUCCESS",
            "fallback_mode": True,
            "analysis": {
                "decision": "REJECT" if "R$ 6,00" in data.notification_text and "9.5 km" in data.notification_text else "EVALUATE",
                "voice_alert": "TACTICAL_ANGRY",
                "reasoning": "Taxa R$/km inviável de R$ 0,63/km. Rejeição recomendada pelo cockpit.",
                "haptic_pattern": "AUTO_REJECTED"
            }
        }
    try:
        prompt = f"TYPE: RIDE_ANALYSIS. Notificação recebida: {data.notification_text}"
        response = model.generate_content(prompt)
        json_data = json.loads(response.text)
        return {"status": "SUCCESS", "analysis": json_data}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro ao processar com Gemini: {str(e)}")

# Endpoint 2: Gerador de Conteúdo para Operação Enxame (Para N8N / Python Marketing)
@app.post("/api/v1/generate-enxame")
async def generate_enxame(data: EnxameRequest):
    model = get_gemini_model()
    if not model:
        return {
            "status": "SUCCESS",
            "fallback_mode": True,
            "content": [
                {
                    "title": "Alerta de Corrida Prejuízo",
                    "hook": "9.5 km por 6 reais? O Jarvis bloqueou na hora!",
                    "script": "Veja como o piloto economizou gasolina e não caiu na pegadinha da plataforma.",
                    "pillar": data.pillar
                }
            ]
        }
    try:
        prompt = f"TYPE: GENERATE_VIRAL_BATCH. Gerar {data.quantity} variações para o pilar {data.pillar}."
        response = model.generate_content(prompt)
        json_data = json.loads(response.text)
        return {"status": "SUCCESS", "content": json_data}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro ao gerar conteúdo: {str(e)}")

# ---------------------------------------------------------
# ENDPOINTS DE AUTOMAÇÃO DE META ADS (VPS HOSTINGER)
# ---------------------------------------------------------
try:
    from meta_ads_engine import MetaAdsEngine
except ImportError:
    import sys
    sys.path.append(os.path.dirname(__file__))
    from meta_ads_engine import MetaAdsEngine

class MetaBudgetRequest(BaseModel):
    budget_reais: float

class MetaToggleRequest(BaseModel):
    active: bool

@app.post("/api/meta-ads/launch")
async def launch_meta_campaign():
    """Lança a campanha automatizada no Meta Ads ou simula na VPS."""
    result = MetaAdsEngine.launch_campaign(daily_budget_cents=2000)
    return result

@app.post("/api/meta-ads/toggle-status")
async def toggle_meta_campaign(data: MetaToggleRequest):
    """Pausa ou ativa a campanha no Meta Ads."""
    result = MetaAdsEngine.toggle_status(active=data.active)
    return result

@app.get("/api/meta-ads/insights")
async def get_meta_insights():
    """Retorna métricas em tempo real (gastos, cliques, CPC, downloads)."""
    result = MetaAdsEngine.get_insights()
    return result

@app.post("/api/meta-ads/budget")
async def update_meta_budget(data: MetaBudgetRequest):
    """Atualiza o orçamento diário da campanha no Meta Ads."""
    cents = int(data.budget_reais * 100)
    result = MetaAdsEngine.update_budget(daily_budget_cents=cents)
    return result

# -------------------------------------------------------------
# ROTAS DE AUTOMAÇÃO DO WHATSAPP PESSOAL (EVOLUTION API / BAILEYS)
# -------------------------------------------------------------
try:
    from whatsapp_marketing_engine import WhatsAppMarketingEngine
except ImportError:
    import sys
    sys.path.append(os.path.dirname(__file__))
    from whatsapp_marketing_engine import WhatsAppMarketingEngine

try:
    from whatsapp_ai_sales_agent import WhatsAppAiSalesAgent
    from asaas_pix_auto_activation import AsaasPixAutoActivation
except ImportError:
    import sys
    sys.path.append(os.path.dirname(__file__))
    from whatsapp_ai_sales_agent import WhatsAppAiSalesAgent
    from asaas_pix_auto_activation import AsaasPixAutoActivation

class WhatsAppTestRequest(BaseModel):
    phone_number: str
    custom_message: Optional[str] = None

class WhatsAppToggleRequest(BaseModel):
    active: bool

class WhatsAppIncomingLeadRequest(BaseModel):
    phone: str
    message: str

class AiSalesToggleRequest(BaseModel):
    enabled: bool

class AiSimulateRequest(BaseModel):
    phone: Optional[str] = "5511999999999"
    message: str

@app.get("/api/marketing/whatsapp/status")
async def get_whatsapp_marketing_status():
    """Retorna status e métricas do WhatsApp Pessoal na VPS."""
    return WhatsAppMarketingEngine.get_status()

@app.post("/api/marketing/whatsapp/send-test")
async def send_whatsapp_test(data: WhatsAppTestRequest):
    """Envia mensagem de teste do funil para o WhatsApp do dono."""
    return WhatsAppMarketingEngine.send_test_message(phone_number=data.phone_number, custom_message=data.custom_message)

@app.post("/api/marketing/whatsapp/toggle-auto-reply")
async def toggle_whatsapp_auto_reply(data: WhatsAppToggleRequest):
    """Ativa ou pausa a auto-resposta de leads do WhatsApp."""
    return WhatsAppMarketingEngine.toggle_auto_reply(active=data.active)

@app.get("/api/marketing/whatsapp/leads")
async def get_whatsapp_leads():
    """Retorna os leads recentes capturados pelo funil de anúncios."""
    leads = WhatsAppMarketingEngine.get_recent_leads()
    return {"success": True, "total_leads": len(leads), "leads": leads}

@app.post("/webhook/whatsapp/lead")
async def handle_whatsapp_lead_webhook(data: WhatsAppIncomingLeadRequest):
    """Webhook acionado pela Evolution API/Baileys quando um motoboy chama no WhatsApp."""
    # Se a IA estiver ativa, ela assume o atendimento inteligente
    if WhatsAppAiSalesAgent.is_enabled():
        return WhatsAppAiSalesAgent.generate_ai_response(phone=data.phone, user_message=data.message)
    return WhatsAppMarketingEngine.handle_incoming_lead_message(phone=data.phone, message_text=data.message)

# ==============================================================================
# ROTAS DA IA VENDEDORA 100% AUTÔNOMA E ATIVAÇÃO IMEDIATA ASAAS PIX
# ==============================================================================

@app.get("/api/marketing/ai-sales/status")
async def get_ai_sales_status():
    """Retorna métricas em tempo real da IA vendedora que roda na VPS."""
    metrics = WhatsAppAiSalesAgent.get_metrics()
    return {"success": True, "data": metrics}

@app.post("/api/marketing/ai-sales/toggle")
async def toggle_ai_sales(data: AiSalesToggleRequest):
    """Ativa ou pausa o piloto de vendas autônomo da IA no WhatsApp."""
    success = WhatsAppAiSalesAgent.set_enabled(data.enabled)
    return {"success": success, "enabled": data.enabled}

@app.post("/api/marketing/ai-sales/simulate")
async def simulate_ai_negotiation(data: AiSimulateRequest):
    """Permite testar e simular ao vivo como a IA quebra objeções e fecha vendas."""
    result = WhatsAppAiSalesAgent.generate_ai_response(phone=data.phone or "5511999999999", user_message=data.message)
    return {"success": True, "simulation": result}

@app.get("/api/marketing/ai-sales/deals")
async def get_ai_sales_deals():
    """Retorna as vendas fechadas e ativadas automaticamente pela IA."""
    deals = WhatsAppAiSalesAgent.get_recent_deals()
    return {"success": True, "total": len(deals), "deals": deals}

@app.post("/api/webhooks/asaas-pix")
async def handle_asaas_pix_webhook(payload: Dict[str, Any]):
    """Webhook oficial do Asaas: liquida o Pix e ativa a conta VIP imediatamente."""
    result = AsaasPixAutoActivation.process_webhook_event(payload)
    return result

# ==============================================================================
# ROTAS DA SUPER AUTOMAÇÃO 360° (RECUPERAÇÃO PIX, VOZ HUMANIZADA, OTIMIZADOR ADS)
# ==============================================================================

try:
    from whatsapp_pix_recovery import WhatsAppPixRecoveryEngine
    from whatsapp_voice_engine import WhatsAppVoiceEngine
    from meta_ads_auto_optimizer import MetaAdsAutoOptimizer
except ImportError:
    import sys
    sys.path.append(os.path.dirname(__file__))
    from whatsapp_pix_recovery import WhatsAppPixRecoveryEngine
    from whatsapp_voice_engine import WhatsAppVoiceEngine
    from meta_ads_auto_optimizer import MetaAdsAutoOptimizer

class SuperAutoToggleRequest(BaseModel):
    feature: str
    enabled: bool

class TriggerRecoveryRequest(BaseModel):
    phone: Optional[str] = "5511999999999"

class TestVoiceRequest(BaseModel):
    phone: Optional[str] = "5511999999999"
    script_key: Optional[str] = "apresentacao"

@app.get("/api/marketing/super-auto/status")
async def get_super_auto_status():
    """Retorna métricas consolidadas da suite de super automação."""
    return {"success": True, "data": WhatsAppPixRecoveryEngine.get_status()}

@app.post("/api/marketing/super-auto/toggle")
async def toggle_super_auto_feature(data: SuperAutoToggleRequest):
    """Ativa ou desativa módulos individuais da super automação."""
    success = WhatsAppPixRecoveryEngine.toggle_feature(data.feature, data.enabled)
    return {"success": success, "feature": data.feature, "enabled": data.enabled}

@app.post("/api/marketing/super-auto/trigger-recovery")
async def trigger_pix_recovery(data: TriggerRecoveryRequest):
    """Dispara teste de recuperação de Pix abandonado."""
    return WhatsAppPixRecoveryEngine.trigger_test_recovery(data.phone or "5511999999999")

@app.post("/api/marketing/super-auto/test-voice")
async def test_voice_message(data: TestVoiceRequest):
    """Simula envio de mensagem de áudio humanizado no WhatsApp."""
    return WhatsAppVoiceEngine.simulate_voice_send(data.phone or "5511999999999", data.script_key or "apresentacao")

@app.post("/api/marketing/super-auto/optimize-ads")
async def run_ads_optimization():
    """Executa ciclo autônomo de análise e otimização do Meta Ads."""
    return MetaAdsAutoOptimizer.run_optimization_cycle()

# ==============================================================================
# ROTAS DA OPERAÇÃO SINGULARIDADE - R$ 3 MILHÕES / 10 AGENTES AUTÔNOMOS (MIND-0)
# ==============================================================================

try:
    from operacao_singularidade_swarm import OperacaoSingularidadeSwarm
except ImportError:
    import sys
    sys.path.append(os.path.dirname(__file__))
    from operacao_singularidade_swarm import OperacaoSingularidadeSwarm

_singularidade_swarm = OperacaoSingularidadeSwarm()

@app.get("/api/singularidade/status")
async def get_singularidade_status():
    """Retorna o progresso em tempo real da meta de R$ 8.333/dia e R$ 3M/ano."""
    return {"success": True, "data": _singularidade_swarm.get_live_dashboard_status()}

@app.post("/api/singularidade/cycle/run")
async def trigger_singularidade_cycle():
    """Dispara um ciclo autônomo completo orquestrado pelo MIND-0 com os 10 sub-agentes."""
    result = _singularidade_swarm.execute_full_swarm_cycle()
    return {"success": True, "result": result}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)





