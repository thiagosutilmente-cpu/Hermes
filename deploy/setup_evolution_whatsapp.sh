#!/usr/bin/env bash
# ==============================================================================
# INSTALADOR DE WHATSAPP (EVOLUTION API v2 + POSTGRES) - VPS 187.77.248.73
# ==============================================================================
set -e

echo "📱 Configurando Evolution API Oficial (evoapicloud/evolution-api) na porta 8080..."

# 1. Garantir Docker e Docker Compose Plugin
if ! command -v docker >/dev/null 2>&1; then
    echo "🐳 Instalando Docker..."
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg openssl
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
fi

if ! docker compose version >/dev/null 2>&1; then
    apt-get update -y && apt-get install -y docker-compose-plugin || true
fi

# 2. Definir chave e instância consistentes com /root/jarvis_backend/.env
ENV_FILE="/root/jarvis_backend/.env"
INSTANCE_NAME="jarvis_owner_whatsapp"
EVOLUTION_KEY="jarvis_evolution_$(openssl rand -hex 8)"
PG_PASS="jarvis_pg_$(openssl rand -hex 8)"

if [ -f "$ENV_FILE" ]; then
    EXISTING_KEY=$(grep -E '^EVOLUTION_API_KEY=' "$ENV_FILE" | cut -d'=' -f2- | tr -d '"' | tr -d "'" || true)
    if [ -n "$EXISTING_KEY" ] && [ "$EXISTING_KEY" != "sua_chave_global_evolution_aqui" ] && [ "$EXISTING_KEY" != "SUA_CHAVE_EVOLUTION_VPS" ]; then
        EVOLUTION_KEY="$EXISTING_KEY"
    fi
fi

# 3. Criar stack Compose oficial (evoapicloud/evolution-api + postgres:15-alpine)
STACK_DIR="/root/evolution_stack"
mkdir -p "$STACK_DIR"
docker rm -f evolution_api evolution_postgres 2>/dev/null || true

cat <<EOF > "$STACK_DIR/.env"
SERVER_URL=http://187.77.248.73:8080
AUTHENTICATION_API_KEY=${EVOLUTION_KEY}
AUTHENTICATION_EXPOSE_IN_FETCH_INSTANCES=true
DEL_INSTANCE=false
DATABASE_ENABLED=true
DATABASE_PROVIDER=postgresql
DATABASE_CONNECTION_URI=postgresql://evolution_user:${PG_PASS}@evolution-postgres:5432/evolution_db?schema=public
DATABASE_CONNECTION_CLIENT_NAME=evolution_exchange
CACHE_REDIS_ENABLED=false
CACHE_LOCAL_ENABLED=true
EOF

cat <<EOF > "$STACK_DIR/docker-compose.yml"
services:
  evolution-postgres:
    image: postgres:15-alpine
    container_name: evolution_postgres
    restart: always
    environment:
      POSTGRES_DB: evolution_db
      POSTGRES_USER: evolution_user
      POSTGRES_PASSWORD: ${PG_PASS}
    volumes:
      - evolution_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U evolution_user -d evolution_db"]
      interval: 5s
      timeout: 5s
      retries: 10

  evolution-api:
    image: evoapicloud/evolution-api:latest
    container_name: evolution_api
    restart: always
    depends_on:
      evolution-postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    env_file:
      - .env
    volumes:
      - evolution_instances:/evolution/instances

volumes:
  evolution_instances:
  evolution_postgres_data:
EOF

cd "$STACK_DIR"
docker compose down 2>/dev/null || true
docker compose up -d

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

# 6. Aguardar Evolution API subir na porta 8080 e criar instância
echo "⏳ Aguardando Evolution API inicializar (até 45s)..."
for i in $(seq 1 15); do
    if curl -sf "http://127.0.0.1:8080/" >/dev/null 2>&1; then
        break
    fi
    sleep 3
done

curl -s -X POST "http://127.0.0.1:8080/instance/create" \
  -H "apikey: ${EVOLUTION_KEY}" \
  -H "Content-Type: application/json" \
  -d "{\"instanceName\":\"${INSTANCE_NAME}\",\"qrcode\":true,\"integration\":\"WHATSAPP-BAILEYS\"}" >/dev/null || true

echo ""
echo "=================================================================="
echo "✅ EVOLUTION API v2 (WHATSAPP) ONLINE!"
echo "🔑 Chave Global (apikey): ${EVOLUTION_KEY}"
echo "📱 Instância criada: ${INSTANCE_NAME}"
echo "👉 Abra no navegador para ler o QR Code: http://187.77.248.73:8080/manager"
echo "=================================================================="
