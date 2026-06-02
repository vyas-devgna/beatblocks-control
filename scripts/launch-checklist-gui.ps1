# scripts/launch-checklist-gui.ps1 — Launch the BeatBlocks QA Checklist GUI
# Usage: .\scripts\launch-checklist-gui.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host "=== BeatBlocks QA Checklist GUI ===" -ForegroundColor Cyan

$javaApp = Join-Path $root "tools\ChecklistApp.java"
if (-not (Test-Path $javaApp)) {
    Write-Host "ERROR: ChecklistApp.java not found at $javaApp" -ForegroundColor Red
    exit 1
}

# Check Java
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "ERROR: Java not found in PATH. Install Java 21+." -ForegroundColor Red
    exit 1
}

Write-Host "Launching checklist GUI..."
Set-Location $root
& java $javaApp
