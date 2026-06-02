# scripts/test-spicetify-bridge.ps1 — Test Spicetify bridge connectivity
# Usage: .\scripts\test-spicetify-bridge.ps1 [-Port 50321]

param(
    [int]$Port = 50321
)

$ErrorActionPreference = 'Continue'

Write-Host "=== BeatBlocks Bridge Connectivity Test ===" -ForegroundColor Cyan
Write-Host ""

# 1. Check Spicetify CLI
Write-Host "[1/5] Checking Spicetify CLI..." -ForegroundColor Yellow
$spicetify = Get-Command spicetify -ErrorAction SilentlyContinue
if ($spicetify) {
    $version = & spicetify --version 2>&1
    Write-Host "  PASS: Spicetify CLI found ($version)" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Spicetify CLI not found in PATH" -ForegroundColor Red
    Write-Host "  Install: iwr -useb https://raw.githubusercontent.com/spicetify/cli/main/install.ps1 | iex"
}

# 2. Check extension file
Write-Host "[2/5] Checking extension file..." -ForegroundColor Yellow
$extPath = "$env:APPDATA\spicetify\Extensions\beatblocks-api.js"
if (Test-Path $extPath) {
    Write-Host "  PASS: Extension found at $extPath" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Extension not found at $extPath" -ForegroundColor Red
    Write-Host "  Copy beatblocks-api.js to $extPath"
}

# 3. Check desktop player process (Windows: Spotify.exe)
Write-Host "[3/5] Checking desktop player..." -ForegroundColor Yellow
$player = Get-Process Spotify -ErrorAction SilentlyContinue
if ($player) {
    Write-Host "  PASS: Desktop player is running" -ForegroundColor Green
} else {
    Write-Host "  WARN: Desktop player is not running" -ForegroundColor Yellow
}

# 4. Check port listener
Write-Host "[4/5] Checking port $Port..." -ForegroundColor Yellow
try {
    $listener = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listener) {
        Write-Host "  PASS: Port $Port is listening" -ForegroundColor Green
    } else {
        Write-Host "  WARN: Port $Port is not listening (Minecraft may not be running)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  SKIP: Could not check port (admin may be required)" -ForegroundColor Yellow
}

# 5. Test HTTP endpoint
Write-Host "[5/5] Testing bridge HTTP endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/state" -Method GET -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
    Write-Host "  PASS: Bridge responded (status $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "  WARN: Bridge not responding (Minecraft or extension may not be running)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Bridge test complete ===" -ForegroundColor Cyan
