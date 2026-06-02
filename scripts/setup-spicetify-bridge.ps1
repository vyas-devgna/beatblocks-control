# scripts/setup-spicetify-bridge.ps1 — Guided setup for Spicetify bridge extension
# This script is NEVER run automatically by Minecraft. It is a manual user tool.
# Usage: .\scripts\setup-spicetify-bridge.ps1

$ErrorActionPreference = 'Stop'

Write-Host "=== BeatBlocks Spicetify Bridge Setup ===" -ForegroundColor Cyan
Write-Host "This script helps you install the Spicetify bridge extension for BeatBlocks."
Write-Host "It will NOT modify anything without your explicit confirmation."
Write-Host ""

# Step 1: Check Spicetify
Write-Host "[Step 1] Checking for Spicetify CLI..." -ForegroundColor Yellow
$spicetify = Get-Command spicetify -ErrorAction SilentlyContinue
if (-not $spicetify) {
    Write-Host "  Spicetify CLI not found." -ForegroundColor Red
    Write-Host "  To install, open a NEW PowerShell window and run:"
    Write-Host "    iwr -useb https://raw.githubusercontent.com/spicetify/cli/main/install.ps1 | iex" -ForegroundColor White
    Write-Host "    spicetify backup apply" -ForegroundColor White
    Write-Host ""
    Write-Host "  After installing, re-run this script."
    exit 0
}
$version = & spicetify --version 2>&1
Write-Host "  Found: spicetify $version" -ForegroundColor Green

# Step 2: Find extension source
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$sourceFile = Join-Path $root "beatblocks-api.js"
if (-not (Test-Path $sourceFile)) {
    Write-Host "  ERROR: beatblocks-api.js not found at $sourceFile" -ForegroundColor Red
    exit 1
}

# Step 3: Copy extension
$destDir = "$env:APPDATA\spicetify\Extensions"
$destFile = Join-Path $destDir "beatblocks-api.js"
Write-Host ""
Write-Host "[Step 2] Copy extension to Spicetify" -ForegroundColor Yellow
Write-Host "  Source: $sourceFile"
Write-Host "  Destination: $destFile"

if (Test-Path $destFile) {
    Write-Host "  Extension already exists at destination." -ForegroundColor Yellow
    $overwrite = Read-Host "  Overwrite? (y/N)"
    if ($overwrite -ne 'y' -and $overwrite -ne 'Y') {
        Write-Host "  Skipping copy."
    } else {
        Copy-Item $sourceFile $destFile -Force
        Write-Host "  Copied." -ForegroundColor Green
    }
} else {
    $confirm = Read-Host "  Copy extension? (y/N)"
    if ($confirm -ne 'y' -and $confirm -ne 'Y') {
        Write-Host "  Aborted."
        exit 0
    }
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    Copy-Item $sourceFile $destFile -Force
    Write-Host "  Copied." -ForegroundColor Green
}

# Step 4: Register and apply
Write-Host ""
Write-Host "[Step 3] Register and apply extension" -ForegroundColor Yellow
Write-Host "  This will run:"
Write-Host "    spicetify config extensions beatblocks-api.js" -ForegroundColor White
Write-Host "    spicetify apply" -ForegroundColor White
Write-Host "  The desktop player will restart."

$apply = Read-Host "  Proceed? (y/N)"
if ($apply -ne 'y' -and $apply -ne 'Y') {
    Write-Host "  Skipped. Run these commands manually when ready."
    exit 0
}

& spicetify config extensions beatblocks-api.js
& spicetify apply

Write-Host ""
Write-Host "=== Setup complete ===" -ForegroundColor Green
Write-Host "Launch Minecraft and press Alt+I to open BeatBlocks."
Write-Host "Select Enhanced Spicetify Mode in the settings overlay."
