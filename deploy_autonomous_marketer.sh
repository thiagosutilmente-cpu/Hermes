#!/bin/bash
# ==============================================================================
# Deploy do Agente Autônomo de Marketing & Vendas Jarvis (100% Celular / VPS)
# ==============================================================================
set -e

echo "🚀 Iniciando Deploy do Agente de Marketing Jarvis com Claude..."

# 1. Garante dependências básicas de Python
python3 -c "import sqlite3, urllib.request, json; print('✅ Python3 e módulos padrão prontos.')"

# 2. Inicializa o banco de dados e gera o primeiro lote de 5 anúncios
python3 -c "import sys; sys.path.append('.'); import marketing.jarvis_autonomous_marketer as jam; jam.init_marketing_db(); jam.generate_daily_marketing_batch()"

# 3. Mata processos anteriores do marketer se existirem
pkill -f "jarvis_autonomous_marketer.py" 2>/dev/null || true

# 4. Inicia o Agente Autônomo em segundo plano 24/7
nohup python3 marketing/jarvis_autonomous_marketer.py > marketer.log 2>&1 &

echo "======================================================================"
echo "✅ Agente Autônomo de Marketing & Vendas ATIVO e rodando 24/7!"
echo "📡 Relatório Matinal agendado diariamente para as 08:00 no WhatsApp."
echo "💰 Teto Diário Rígido: R$ 15,00/dia no Meta Ads."
echo "📝 Para ver os logs em tempo real: tail -f marketer.log"
echo "======================================================================"
