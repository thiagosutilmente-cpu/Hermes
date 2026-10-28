#!/bin/bash
# ==============================================================================
# ATIVAÇÃO DO COCKPIT TÁTICO & DASHBOARD EXECUTIVO (JARVIS HQ)
# ==============================================================================
set -e

echo ">>> [1/4] Copiando admin.html para o Nginx e pasta do projeto..."
cp /root/Hermes/deploy/admin.html /var/www/html/admin.html 2>/dev/null || true

echo ">>> [2/4] Reiniciando serviço do Jarvis FastAPI..."
systemctl restart jarvis

echo ">>> [3/4] Verificando status do serviço..."
sleep 2
systemctl is-active --quiet jarvis && echo ">>> JARVIS SERVICE: ONLINE!" || echo ">>> ALERTA: Verificar logs com journalctl -u jarvis"

echo ">>> [4/4] Testando endpoint de métricas..."
curl -s http://127.0.0.1:8000/api/admin/metrics | head -n 10
echo ""
echo "=================================================================="
echo "🎯 DASHBOARD TÁTICO ATIVO NO NAVEGADOR DO CELULAR:"
echo "👉 http://187.77.248.73/admin"
echo "👉 http://187.77.248.73/dashboard"
echo "=================================================================="
