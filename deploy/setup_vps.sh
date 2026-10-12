#!/usr/bin/env bash
# ==============================================================================
# JARVIS NEURAL COCKPIT - SCRIPT AUTOMÁTICO DE DEPLOY (EXECUTE NA SUA VPS HOSTINGER)
# ==============================================================================
set -e

echo "🚀 Iniciando instalação e deploy automático do JARVIS NEURAL COCKPIT..."

# 1. Atualizar e instalar dependências essenciais
apt update && apt install -y python3 python3-pip python3-venv sqlite3 curl ufw

# 2. Criar diretório da aplicação
mkdir -p /root/jarvis_backend
cd /root/jarvis_backend

# 3. Criar ambiente virtual
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install fastapi "uvicorn[standard]" pydantic "google-generativeai>=0.8.0" requests python-dotenv

# 4. Baixar o arquivo main.py caso hospedado ou criar estrutura padrão
echo "⚙️ Configurando serviço systemd..."

cat << 'EOF' > /etc/systemd/system/jarvis.service
[Unit]
Description=Jarvis Neural Cockpit Backend FastAPI
After=network.target

[Service]
User=root
WorkingDirectory=/root/jarvis_backend
Environment="PATH=/root/jarvis_backend/venv/bin"
Environment="GEMINI_API_KEY="
Environment="ASAAS_ENV=sandbox"
ExecStart=/root/jarvis_backend/venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000 --workers 2
Restart=always

[Install]
WantedBy=multi-user.target
EOF

ufw allow 8000/tcp || true
systemctl daemon-reload
systemctl enable jarvis

echo "✅ Ambiente da VPS Hostinger preparado com sucesso!"
echo "👉 Basta copiar o arquivo main.py para /root/jarvis_backend/main.py e rodar: systemctl restart jarvis"
