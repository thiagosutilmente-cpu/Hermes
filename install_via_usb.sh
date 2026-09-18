#!/usr/bin/env bash
# ==============================================================================
# RADAR COORDINATOR — SCRIPT DE INSTALAÇÃO VIA USB (LINUX / MACOS)
# ==============================================================================

echo "=========================================================="
echo "🎯 RADAR COORDINATOR — INSTALAÇÃO DO APK VIA USB"
echo "=========================================================="

APK_FILE="RadarCoordinator.apk"

if [ ! -f "$APK_FILE" ]; then
    if [ -f "build/outputs/apk/debug/app-debug.apk" ]; then
        APK_FILE="build/outputs/apk/debug/app-debug.apk"
    elif [ -f ".build-outputs/app-debug.apk" ]; then
        APK_FILE=".build-outputs/app-debug.apk"
    elif [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_FILE="app/build/outputs/apk/debug/app-debug.apk"
    else
        echo "❌ Arquivo APK não encontrado. Execute a compilação primeiro."
        exit 1
    fi
fi

echo "📦 Pacote APK identificado: $APK_FILE"

# Verifica se o utilitário ADB está instalado
if command -v adb &> /dev/null; then
    echo "🔍 Procurando dispositivos Android conectados via USB..."
    DEVICES=$(adb devices | grep -w "device" | awk '{print $1}')
    
    if [ -n "$DEVICES" ]; then
        echo "✅ Dispositivo conectado detectado!"
        echo "📲 Instalando Radar Coordinator no dispositivo..."
        adb install -r "$APK_FILE"
        if [ $? -eq 0 ]; then
            echo ""
            echo "🎉 SUCESSO: Radar Coordinator instalado no seu celular!"
            echo "👉 Abra o app 'Radar Coordinator' no seu Android para começar."
            exit 0
        else
            echo "⚠️ Falha ao instalar via adb install."
        fi
    else
        echo "⚠️ Nenhum dispositivo com 'Depuração USB' autorizado encontrado."
    fi
else
    echo "ℹ️ ADB não detectado no sistema."
fi

echo ""
echo "=========================================================="
echo "📋 INSTRUÇÕES PARA INSTALAÇÃO MANUAL VIA CABO USB:"
echo "=========================================================="
echo "1. Conecte seu celular Android ao computador via cabo USB."
echo "2. No celular, escolha a opção 'Transferência de Arquivos' (MTP)."
echo "3. Abra o explorador de arquivos do PC e copie o arquivo:"
echo "   -> $APK_FILE"
echo "   Para a pasta 'Downloads' da memória interna do celular."
echo "4. No celular, abra o aplicativo 'Arquivos' ou 'Downloads'."
echo "5. Toque no 'RadarCoordinator.apk' e selecione 'Instalar'."
echo "   (Se o Android solicitar, marque 'Permitir desta fonte')."
echo "=========================================================="
