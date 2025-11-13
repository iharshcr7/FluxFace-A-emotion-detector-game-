# Restart Emotion Detection Server
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Restarting Emotion Detection Server" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Stop any running Python processes
Write-Host "`nStopping existing Python server..." -ForegroundColor Yellow
Get-Process python -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# Start the server
Write-Host "`nStarting emotion detection server..." -ForegroundColor Green
Write-Host "Location: emotion_detector/app.py" -ForegroundColor Gray
Write-Host "`nServer logs will appear below:" -ForegroundColor Yellow
Write-Host "----------------------------------------`n" -ForegroundColor Gray

Set-Location -Path "$PSScriptRoot\emotion_detector"
python app.py
