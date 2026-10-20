#!/usr/bin/env bash
# ==============================================================================
# SCRIPT DE ATIVAÇÃO 1-CLIQUE - JARVIS NEURAL COCKPIT (VPS LINUX)
# ==============================================================================
# Como usar:
# 1. Conecte na sua VPS via SSH: ssh root@187.77.248.73
# 2. Execute: bash setup_vps.sh
# ==============================================================================

set -e

echo "🚀 Iniciando configuração do Jarvis Neural Cockpit na VPS..."

# 1. Atualizar e instalar Python e dependências do sistema
apt-get update -y
apt-get install -y python3 python3-pip python3-venv curl

# 2. Criar diretório do app caso não exista
APP_DIR="/root/jarvis_backend"
mkdir -p "$APP_DIR"
cd "$APP_DIR"

# 3. Criar ambiente virtual Python
if [ ! -d "venv" ]; then
    echo "📦 Criando ambiente virtual Python..."
    python3 -m venv venv
fi

source venv/bin/activate

# 4. Instalar dependências Python
echo "⚡ Instalando FastAPI, Uvicorn e Google Generative AI..."
pip install --upgrade pip
pip install fastapi uvicorn requests pydantic google-generativeai

# 5. Criar serviço no systemd para reiniciar automaticamente em caso de falha ou reboot
echo "⚙️ Configurando serviço systemd (jarvis.service)..."
cat << 'EOF' > /etc/systemd/system/jarvis.service
[Unit]
Description=Jarvis Neural Cockpit Backend FastAPI
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/root/jarvis_backend
EnvironmentFile=-/root/jarvis_backend/.env
ExecStart=/root/jarvis_backend/venv/bin/uvicorn vps_backend_complete:app --host 0.0.0.0 --port 8000 --workers 2
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# 6. Recarregar e iniciar o serviço
systemctl daemon-reload
systemctl enable jarvis
systemctl restart jarvis

sleep 2

# 7. Testar se o servidor está respondendo
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/health || echo "000")

if [ "$STATUS_CODE" -eq 200 ]; then
    echo "✅ SUCESSO! O Jarvis Backend está ONLINE na porta 8000!"
    echo "🔗 Teste público: http://187.77.248.73:8000/health"
else
    echo "⚠️ O serviço foi iniciado mas retornou status $STATUS_CODE. Verifique com: journalctl -u jarvis -n 20"
fi
