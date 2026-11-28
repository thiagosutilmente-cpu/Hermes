#!/bin/bash
# ==============================================================================
# ATIVAÇÃO DO COCKPIT TÁTICO & DASHBOARD EXECUTIVO ISOLADO (JARVIS HQ)
# ==============================================================================
set -e

echo ">>> [1/4] Atualizando admin.html apenas na pasta isolada do backend (fora do webroot público)..."
mkdir -p /root/jarvis_backend
cp -f /root/Hermes/deploy/admin.html /root/jarvis_backend/admin.html 2>/dev/null || true
rm -f /var/www/html/admin.html

echo ">>> [2/4] Reiniciando serviço do Jarvis FastAPI e Nginx..."
systemctl restart jarvis
nginx -t && systemctl reload nginx 2>/dev/null || true

echo ">>> [3/4] Verificando status do serviço..."
sleep 2
systemctl is-active --quiet jarvis && echo ">>> JARVIS SERVICE: ONLINE!" || echo ">>> ALERTA: Verificar logs com journalctl -u jarvis"

echo ">>> [4/4] Verificando bloqueio de acesso não autenticado..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8000/api/admin/metrics || echo "000")
echo "Resposta sem token em /api/admin/metrics: HTTP $HTTP_CODE (esperado: 401)"
echo ""
echo "=================================================================="
echo "🛡️ DASHBOARD TÁTICO ISOLADO (ACESSO VIA TÚNEL SSH SEGURO):"
echo "1) No seu computador/termux, abra o túnel:"
echo "   ssh -N -L 8443:127.0.0.1:8443 root@187.77.248.73"
echo "2) Acesse no navegador:"
echo "   👉 http://127.0.0.1:8443/"
echo "=================================================================="
