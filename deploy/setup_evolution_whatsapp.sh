#!/usr/bin/env bash
# ==============================================================================
# INSTALADOR DE WHATSAPP (EVOLUTION API DOCKER) - VPS HOSTINGER (187.77.248.73)
# ==============================================================================
set -e

echo "📱 Configurando Evolution API (WhatsApp WebSocket) na porta 8080..."

# 1. Instalar Docker se ainda não existir
if ! command -v docker >/dev/null 2>&1; then
    echo "🐳 Instalando Docker..."
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg openssl
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
fi

# 2. Definir chave e instância consistentes com /root/jarvis_backend/.env
ENV_FILE="/root/jarvis_backend/.env"
INSTANCE_NAME="jarvis_owner_whatsapp"
EVOLUTION_KEY="jarvis_evolution_$(openssl rand -hex 8)"

if [ -f "$ENV_FILE" ]; then
    EXISTING_KEY=$(grep -E '^EVOLUTION_API_KEY=' "$ENV_FILE" | cut -d'=' -f2- | tr -d '"' | tr -d "'" || true)
    if [ -n "$EXISTING_KEY" ] && [ "$EXISTING_KEY" != "sua_chave_global_evolution_aqui" ] && [ "$EXISTING_KEY" != "SUA_CHAVE_EVOLUTION_VPS" ]; then
        EVOLUTION_KEY="$EXISTING_KEY"
    fi
fi

# 3. Subir container Evolution API v1.8.2 (compatível com payloads textMessage do Jarvis)
mkdir -p /root/evolution_data/instances /root/evolution_data/store
docker rm -f evolution_api 2>/dev/null || true

docker run -d \
  --name evolution_api \
  --restart always \
  -p 8080:8080 \
  -e SERVER_URL=http://187.77.248.73:8080 \
  -e AUTHENTICATION_TYPE=apikey \
  -e AUTHENTICATION_API_KEY="${EVOLUTION_KEY}" \
  -e AUTHENTICATION_EXPOSE_IN_FETCH_INSTANCES=true \
  -e DEL_INSTANCE=false \
  -e WEBHOOK_GLOBAL_URL=http://172.17.0.1:8000/webhook/whatsapp/lead \
  -e WEBHOOK_GLOBAL_ENABLED=false \
  -v /root/evolution_data/instances:/evolution/instances \
  -v /root/evolution_data/store:/evolution/store \
  atendai/evolution-api:v1.8.2

# 4. Liberar porta 8080 no UFW se ativo
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
    ufw allow 8080/tcp || true
fi

# 5. Atualizar .env do Jarvis Backend e reiniciar serviço
if [ -f "$ENV_FILE" ]; then
    sed -i "s|^EVOLUTION_API_URL=.*|EVOLUTION_API_URL=http://127.0.0.1:8080|" "$ENV_FILE"
    sed -i "s|^EVOLUTION_API_KEY=.*|EVOLUTION_API_KEY=${EVOLUTION_KEY}|" "$ENV_FILE"
    sed -i "s|^EVOLUTION_INSTANCE=.*|EVOLUTION_INSTANCE=${INSTANCE_NAME}|" "$ENV_FILE"
    systemctl restart jarvis || true
fi

echo "⏳ Aguardando Evolution API iniciar (8s)..."
sleep 8

# 6. Criar instância do WhatsApp automaticamente
curl -s -X POST "http://127.0.0.1:8080/instance/create" \
  -H "apikey: ${EVOLUTION_KEY}" \
  -H "Content-Type: application/json" \
  -d "{\"instanceName\":\"${INSTANCE_NAME}\",\"qrcode\":true}" >/dev/null || true

echo ""
echo "=================================================================="
echo "✅ EVOLUTION API WHATSAPP ONLINE!"
echo "🔑 Chave Global (apikey): ${EVOLUTION_KEY}"
echo "📱 Instância criada: ${INSTANCE_NAME}"
echo "👉 Abra no navegador para ler o QR Code: http://187.77.248.73:8080/manager"
echo "=================================================================="
