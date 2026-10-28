@echo off
title HaxBall Admin Panel
cd /d "%~dp0"
echo.
echo ========================================
echo  🎮 HaxBall DreamTeam - Admin Panel
echo ========================================
echo.
echo 🚀 Iniciando servidor...
echo.

REM Tentar diferentes portas
set PORT=3000
npm run dev
if errorlevel 1 (
    echo.
    echo ⚠️  Porta 3000 ocupada, tentando porta 3001...
    set PORT=3001
    npm run dev
)
if errorlevel 1 (
    echo.
    echo ⚠️  Porta 3001 ocupada, tentando porta 3002...
    set PORT=3002
    npm run dev
)

echo.
echo ✅ Servidor iniciado! Acesse: http://localhost:%PORT%
echo 🔑 Senha: admin123
echo.
pause
