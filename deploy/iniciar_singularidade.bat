@echo off
REM ==============================================================================
REM OPERAÇÃO SINGULARIDADE - INICIADOR RÁPIDO PARA WINDOWS (CLAUDE NO PC)
REM ==============================================================================
cd /d "%~dp0"
echo ======================================================================
echo Iniciando Operacao Singularidade (MIND-0 + 10 Agentes Autonomos)...
echo ======================================================================
python executar_operacao_singularidade.py %*
pause
