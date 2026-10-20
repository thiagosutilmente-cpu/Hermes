#!/bin/bash
# ==============================================================================
# SCRIPT DE INSTALAÇÃO E INICIALIZAÇÃO DA AUTOMAÇÃO DE MARKETING NA VPS
# ==============================================================================
# Executar como root na VPS Hostinger (IP: 187.77.248.73)
# ==============================================================================

set -e

echo "🚀 Iniciando configuração do Jarvis Marketing Engine (Meta Ads + WhatsApp)..."

# 1. Atualizar pacotes e dependências Python
apt-get update -y
apt-get install -y python3-pip python3-venv sqlite3 curl ufw

# 2. Instalar dependências Python
pip install --upgrade pip
pip install fastapi uvicorn requests pydantic python-dotenv

# 3. Criar diretório de dados se não existir
mkdir -p /opt/jarvis_marketing
cd /opt/jarvis_marketing

# 4. Criar arquivo de variáveis de ambiente se não existir
if [ ! -f .env ]; then
    cat <<EOT > .env
# Chaves da Meta Marketing API
META_ACCESS_TOKEN=""
META_AD_ACCOUNT_ID=""
META_PAGE_ID=""

# Chaves da Evolution API (WhatsApp)
EVOLUTION_API_URL="http://localhost:8080"
EVOLUTION_API_KEY="jarvis_evolution_secret_key"
EVOLUTION_INSTANCE="jarvis_owner_whatsapp"
OWNER_PERSONAL_PHONE="(11) 98765-4321"

# Configurações do Asaas Pix (Liberação Instantânea)
ASAAS_API_KEY=""
ASAAS_WEBHOOK_SECRET="radar_asaas_webhook_secret_2026"

# Servidor FastAPI
PORT=8000
HOST=0.0.0.0
EOT
    echo "📄 Arquivo .env criado em /opt/jarvis_marketing/.env com suporte a IA e Pix Asaas"
fi

# 5. Liberar portas necessárias no firewall
ufw allow 8000/tcp || true
ufw allow 8080/tcp || true

# 6. Criar serviço systemd para o Backend rodar 24/7 de forma resiliente
cat <<EOT > /etc/systemd/system/jarvis-marketing.service
[Unit]
Description=Jarvis Marketing Engine - Meta Ads & WhatsApp Automation
After=network.target

[Service]
User=root
WorkingDirectory=/opt/jarvis_marketing
ExecStart=/usr/local/bin/uvicorn vps_backend_complete:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=5
EnvironmentFile=/opt/jarvis_marketing/.env

[Install]
WantedBy=multi-user.target
EOT

systemctl daemon-reload
systemctl enable jarvis-marketing
systemctl restart jarvis-marketing || true

echo "✅ Jarvis Marketing Engine configurado e ativo na VPS!"
echo "📡 Verifique o status com: systemctl status jarvis-marketing"
echo "🌐 API disponível em: http://187.77.248.73:8000/health"
