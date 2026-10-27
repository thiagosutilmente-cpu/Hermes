#!/usr/bin/env bash
# ==============================================================================
# SCRIPT DE ATIVAÇÃO 1-CLIQUE - JARVIS NEURAL COCKPIT (VPS LINUX)
# ==============================================================================
set -e

echo "🚀 Iniciando configuração do Jarvis Neural Cockpit na VPS..."

# 1. Identificar onde estão os arquivos do repositório
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "📂 Diretório do script: $SCRIPT_DIR"

# 2. Atualizar e instalar pacotes do sistema
apt-get update -y
apt-get install -y python3 python3-pip python3-venv curl psmisc nginx

# 3. Diretório de execução do Backend
APP_DIR="/root/jarvis_backend"
mkdir -p "$APP_DIR"

# Copiar arquivos do repositório para o diretório de execução
if [ -f "$SCRIPT_DIR/vps_backend_complete.py" ]; then
    echo "📋 Copiando arquivos do backend a partir de $SCRIPT_DIR..."
    cp -f "$SCRIPT_DIR/vps_backend_complete.py" "$APP_DIR/"
    [ -f "$SCRIPT_DIR/privacy.html" ] && cp -f "$SCRIPT_DIR/privacy.html" "$APP_DIR/"
    [ -f "$SCRIPT_DIR/.env" ] && cp -f "$SCRIPT_DIR/.env" "$APP_DIR/" 2>/dev/null || true
fi

# Copiar privacy.html também para o Nginx padrão
mkdir -p /var/www/html
if [ -f "$APP_DIR/privacy.html" ]; then
    cp -f "$APP_DIR/privacy.html" /var/www/html/privacy.html
fi

cd "$APP_DIR"

# 4. Criar ambiente virtual Python
if [ ! -d "venv" ]; then
    echo "📦 Criando ambiente virtual Python..."
    python3 -m venv venv
fi

source venv/bin/activate

# 5. Instalar dependências Python
echo "⚡ Instalando FastAPI, Uvicorn e dependências..."
pip install --upgrade pip
pip install fastapi uvicorn requests pydantic google-generativeai

# 6. Liberar porta 8000 caso haja processo antigo travando
echo "🔄 Liberando porta 8000..."
fuser -k 8000/tcp 2>/dev/null || true
pkill -f "vps_backend_complete" 2>/dev/null || true

# 7. Criar serviço systemd (jarvis.service)
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

# 8. Configurar e habilitar Nginx na porta 80
echo "🌐 Configurando Nginx na porta 80..."
systemctl unmask nginx 2>/dev/null || true
systemctl enable nginx

cat << 'EOF' > /etc/nginx/sites-available/jarvis
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    client_max_body_size 50M;

    location /privacy.html {
        alias /var/www/html/privacy.html;
    }

    location /privacy {
        alias /var/www/html/privacy.html;
    }

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
nginx -t && systemctl restart nginx || echo "⚠️ Aviso ao iniciar Nginx, verificando..."

# 9. Recarregar e iniciar o serviço jarvis
systemctl daemon-reload
systemctl enable jarvis
systemctl restart jarvis

echo "⏳ Aguardando serviço iniciar..."
sleep 3

# 10. Testar resposta
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8000/api/ping || echo "000")

if [ "$STATUS_CODE" -eq 200 ] || [ "$STATUS_CODE" -eq 404 ]; then
    echo "✅ SUCESSO! O Jarvis Backend está ONLINE nas portas 80 e 8000!"
    echo "🔗 Política de Privacidade: http://187.77.248.73/privacy.html"
    echo "🔗 Swagger Docs: http://187.77.248.73:8000/docs"
else
    echo "⚠️ Status retornado: $STATUS_CODE."
    echo "Últimos logs do serviço:"
    journalctl -u jarvis -n 15 --no-pager
fi
