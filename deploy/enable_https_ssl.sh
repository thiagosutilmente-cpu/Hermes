#!/usr/bin/env bash
# ==============================================================================
# ATIVADOR DE HTTPS (SSL/TLS) COM LET'S ENCRYPT + NGINX PARA PRODUÇÃO
# Uso: bash deploy/enable_https_ssl.sh seudominio.com.br seu@email.com
# ==============================================================================
set -e

DOMAIN="${1:-}"
EMAIL="${2:-thiagosutilmente@gmail.com}"

if [ -z "$DOMAIN" ]; then
    echo "Uso: bash deploy/enable_https_ssl.sh <dominio> [email]"
    echo "Exemplo: bash deploy/enable_https_ssl.sh app.radarcoordinator.com.br thiagosutilmente@gmail.com"
    exit 1
fi

echo "🔐 Configurando SSL HTTPS para o domínio: $DOMAIN"
apt-get update -y
apt-get install -y certbot python3-certbot-nginx

sed -i "s/server_name _;/server_name ${DOMAIN} www.${DOMAIN};/" /etc/nginx/sites-available/jarvis || true
nginx -t && systemctl reload nginx

certbot --nginx \
    -d "$DOMAIN" \
    --non-interactive \
    --agree-tos \
    -m "$EMAIL" \
    --redirect

systemctl enable certbot.timer || true
nginx -t && systemctl reload nginx

echo "✅ HTTPS ativado com sucesso!"
echo "👉 Landing Page: https://${DOMAIN}/"
echo "👉 Política de Privacidade (Play Console): https://${DOMAIN}/privacy.html"
echo "👉 API Backend: https://${DOMAIN}/api/ping"
