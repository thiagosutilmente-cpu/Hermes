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

# 6. Configurar Nginx para a porta 80 encaminhar transparente para a 8000
echo "🌐 Configurando Nginx na porta 80..."
apt-get install -y nginx
cat << 'EOF' > /etc/nginx/sites-available/jarvis
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
ln -sf /etc/nginx/sites-available/jarvis /etc/nginx/sites-enabled/default
systemctl restart nginx || true

# 7. Recarregar e iniciar o serviço jarvis
systemctl daemon-reload
systemctl enable jarvis
systemctl restart jarvis

sleep 2

# 8. Testar se o servidor está respondendo
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/health || echo "000")

if [ "$STATUS_CODE" -eq 200 ]; then
    echo "✅ SUCESSO! O Jarvis Backend está ONLINE nas portas 80 e 8000!"
    echo "🔗 Verificação Health: http://187.77.248.73/health"
    echo "🔗 Política de Privacidade: http://187.77.248.73/privacy.html"
    echo "🔗 Swagger Docs: http://187.77.248.73:8000/docs"
else
    echo "⚠️ O serviço foi iniciado mas retornou status $STATUS_CODE. Verifique com: journalctl -u jarvis -n 20"
fi
