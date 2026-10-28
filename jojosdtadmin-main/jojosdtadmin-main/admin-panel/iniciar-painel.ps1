# HaxBall Admin Panel - Iniciador
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 🎮 HaxBall DreamTeam - Admin Panel" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🚀 Iniciando servidor..." -ForegroundColor Green
Write-Host ""

# Mudar para o diretório do script
Set-Location $PSScriptRoot

# Iniciar o servidor
npm run dev

# Manter a janela aberta
Write-Host ""
Write-Host "Pressione qualquer tecla para fechar..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
