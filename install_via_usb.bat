@echo off
chcp 65001 >nul
echo ==========================================================
echo 🎯 RADAR COORDINATOR — INSTALACAO DO APK VIA USB (WINDOWS)
echo ==========================================================

set APK_FILE=RadarCoordinator.apk

if not exist "%APK_FILE%" (
    if exist "build\outputs\apk\debug\app-debug.apk" set APK_FILE=build\outputs\apk\debug\app-debug.apk
    if exist ".build-outputs\app-debug.apk" set APK_FILE=.build-outputs\app-debug.apk
    if exist "app\build\outputs\apk\debug\app-debug.apk" set APK_FILE=app\build\outputs\apk\debug\app-debug.apk
)

echo 📦 Pacote APK identificado: %APK_FILE%

where adb >nul 2>nul
if %errorlevel% equ 0 (
    echo 🔍 Verificando conexao USB via ADB...
    adb devices
    echo 📲 Tentando instalacao automatica via ADB...
    adb install -r "%APK_FILE%"
    if %errorlevel% equ 0 (
        echo.
        echo 🎉 SUCESSO: Radar Coordinator instalado no seu celular!
        pause
        exit /b 0
    )
)

echo.
echo ==========================================================
echo 📋 INSTRUCOES PARA INSTALACAO MANUAL VIA CABO USB:
echo ==========================================================
echo 1. Conecte seu celular Android ao computador via cabo USB.
echo 2. No celular, escolha a opcao 'Transferencia de Arquivos' (MTP).
echo 3. Abra o Explorador de Arquivos do Windows (Meu Computador).
echo 4. Copie o arquivo '%APK_FILE%' para a pasta 'Downloads' do celular.
echo 5. No celular, abra o app 'Meus Arquivos' ou 'Downloads'.
echo 6. Toque no 'RadarCoordinator.apk' e selecione 'Instalar'.
echo    (Se solicitado, habilite 'Permitir desta fonte').
echo ==========================================================
pause
