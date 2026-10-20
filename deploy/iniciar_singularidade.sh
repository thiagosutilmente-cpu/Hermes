#!/usr/bin/env bash
# ==============================================================================
# OPERAÇÃO SINGULARIDADE - INICIADOR RÁPIDO PARA PC E VPS
# ==============================================================================
cd "$(dirname "$0")" || exit 1

echo "======================================================================"
echo "Iniciando Operação Singularidade (MIND-0 + 10 Agentes Autônomos)..."
echo "======================================================================"

python3 executar_operacao_singularidade.py "$@"
