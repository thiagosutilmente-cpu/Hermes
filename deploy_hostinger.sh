#!/bin/bash
# ==============================================================================
# Script de Inicialização Rápida do Radar Coordinator na Hostinger (VPS / Cloud)
# ==============================================================================

echo "🚀 Iniciando Radar Coordinator — Jarvis Neural Cockpit..."

# 1. Garante que o Python3 está disponível
if ! command -v python3 &> /dev/null; then
    echo "📦 Instalando Python3..."
    sudo apt-get update && sudo apt-get install -y python3 python3-pip
fi

# 2. Mata processos anteriores se existirem
pkill -f "app.py" 2>/dev/null

# 3. Executa em segundo plano com reinicialização resiliente
export PYTHON_PORT=80
nohup python3 app.py > server.log 2>&1 &

echo "✅ Cockpit no ar! Acesse pelo IP da sua VPS ou pelo seu domínio na porta 80."
echo "📝 Logs de execução: tail -f server.log"
