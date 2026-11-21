#!/usr/bin/env bash
# ==============================================================================
# SCRIPT DE ATIVAÇÃO 1-CLIQUE - JARVIS NEURAL COCKPIT (VPS LINUX)
# ==============================================================================
set -e

echo "🚀 Iniciando configuração do Jarvis Neural Cockpit na VPS..."

GITHUB_USER="thiagosutilmente-cpu"
GITHUB_REPO="Hermes"
REPO_URL="https://github.com/${GITHUB_USER}/${GITHUB_REPO}.git"
REPO_DIR="/root/${GITHUB_REPO}"

# 1. Identificar onde estão os arquivos do repositório
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "📂 Diretório do script: $SCRIPT_DIR"

# 2. Atualizar e instalar pacotes do sistema
apt-get update -y
apt-get install -y python3 python3-pip python3-venv curl psmisc nginx git ufw

# Garantir repositório atualizado em /root/Hermes caso executado de forma avulsa
SOURCE_DIR="$SCRIPT_DIR"
if [ ! -f "$SOURCE_DIR/vps_backend_complete.py" ]; then
    echo "📥 Clonando repositório oficial ($REPO_URL) em $REPO_DIR..."
    if [ -d "$REPO_DIR/.git" ]; then
        git -C "$REPO_DIR" pull origin main || true
    else
        rm -rf "$REPO_DIR"
        git clone "$REPO_URL" "$REPO_DIR"
    fi
    SOURCE_DIR="$REPO_DIR/deploy"
fi

# 3. Diretório de execução do Backend
APP_DIR="/root/jarvis_backend"
mkdir -p "$APP_DIR" /var/www/html

# Copiar todos os arquivos Python, HTMLs e configurações do repositório
if [ -f "$SOURCE_DIR/vps_backend_complete.py" ]; then
    echo "📋 Copiando todos os módulos do backend a partir de $SOURCE_DIR..."
    cp -f "$SOURCE_DIR"/*.py "$APP_DIR/"
    [ -f "$SOURCE_DIR/requirements.txt" ] && cp -f "$SOURCE_DIR/requirements.txt" "$APP_DIR/"
    [ -f "$SOURCE_DIR/admin.html" ] && cp -f "$SOURCE_DIR/admin.html" "$APP_DIR/" && cp -f "$SOURCE_DIR/admin.html" /var/www/html/admin.html
    [ -f "$SOURCE_DIR/index.html" ] && cp -f "$SOURCE_DIR/index.html" "$APP_DIR/" && cp -f "$SOURCE_DIR/index.html" /var/www/html/index.html
    [ -f "$SOURCE_DIR/privacy.html" ] && cp -f "$SOURCE_DIR/privacy.html" "$APP_DIR/" && cp -f "$SOURCE_DIR/privacy.html" /var/www/html/privacy.html

    # Sincroniza vídeos, pôsteres e imagens da landing page para /var/www/html/assets
    REPO_ROOT="$(dirname "$SOURCE_DIR")"
    mkdir -p /var/www/html/assets "$APP_DIR/assets"
    if [ -d "$REPO_ROOT/public/assets" ]; then
        cp -rf "$REPO_ROOT/public/assets/"* /var/www/html/assets/ 2>/dev/null || true
        cp -rf "$REPO_ROOT/public/assets/"* "$APP_DIR/assets/" 2>/dev/null || true
    fi
    if [ -d "$SOURCE_DIR/assets" ]; then
        cp -rf "$SOURCE_DIR/assets/"* /var/www/html/assets/ 2>/dev/null || true
        cp -rf "$SOURCE_DIR/assets/"* "$APP_DIR/assets/" 2>/dev/null || true
    fi

    # Fallback para arquivos na raiz do repositório (um nível acima de deploy/)
    [ ! -f /var/www/html/index.html ] && [ -f "$REPO_ROOT/index.html" ] && cp -f "$REPO_ROOT/index.html" /var/www/html/index.html && cp -f "$REPO_ROOT/index.html" "$APP_DIR/index.html"
    [ ! -f /var/www/html/privacy.html ] && [ -f "$REPO_ROOT/privacy.html" ] && cp -f "$REPO_ROOT/privacy.html" /var/www/html/privacy.html && cp -f "$REPO_ROOT/privacy.html" "$APP_DIR/privacy.html"

    # Configurar .env (preserva .env existente se já preenchido)
    if [ -f "$SOURCE_DIR/.env" ]; then
        cp -f "$SOURCE_DIR/.env" "$APP_DIR/.env"
    elif [ ! -f "$APP_DIR/.env" ] && [ -f "$SOURCE_DIR/.env.production.example" ]; then
        cp -f "$SOURCE_DIR/.env.production.example" "$APP_DIR/.env"
        echo "📝 Arquivo modelo $APP_DIR/.env criado. Lembre-se de preencher suas chaves de API."
    fi
else
    echo "❌ Erro: vps_backend_complete.py não encontrado em $SOURCE_DIR"
    exit 1
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
if [ -f "requirements.txt" ]; then
    pip install -r requirements.txt
else
    pip install fastapi uvicorn requests pydantic google-generativeai python-dotenv
fi

# 6. Liberar portas no Firewall (se UFW estiver ativo) e matar processo antigo
if ufw status | grep -q "Status: active"; then
    ufw allow 22/tcp || true
    ufw allow 80/tcp || true
    ufw allow 443/tcp || true
    ufw allow 8000/tcp || true
fi
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

    root /var/www/html;
    index index.html;

    client_max_body_size 50M;

    location / {
        try_files $uri $uri/ @backend;
    }

    location @backend {
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
