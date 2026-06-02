# scripts/test-all.ps1 — Run Gradle build and tests for BeatBlocks Control
# Usage: .\scripts\test-all.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

Write-Host "=== BeatBlocks Test Suite ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build
Write-Host "[1/3] Building project..." -ForegroundColor Yellow
& .\gradlew.bat build --no-daemon 2>&1 | ForEach-Object { Write-Host "  $_" }
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "Build OK" -ForegroundColor Green
Write-Host ""

# Step 2: Run tests
Write-Host "[2/3] Running tests..." -ForegroundColor Yellow
& .\gradlew.bat test --no-daemon 2>&1 | ForEach-Object { Write-Host "  $_" }
if ($LASTEXITCODE -ne 0) {
    Write-Host "TESTS FAILED — see build/reports/tests/test/index.html" -ForegroundColor Red
    exit 1
}
Write-Host "Tests OK" -ForegroundColor Green
Write-Host ""

# Step 3: Report
Write-Host "[3/3] Results" -ForegroundColor Yellow
$reportPath = Join-Path $root "build\reports\tests\test\index.html"
if (Test-Path $reportPath) {
    Write-Host "  Test report: $reportPath"
} else {
    Write-Host "  No HTML report generated (tests may have been up-to-date)"
}
$jarPath = Get-ChildItem "$root\build\libs\*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jarPath) {
    Write-Host "  Output JAR: $($jarPath.FullName)"
}
Write-Host ""
Write-Host "=== All checks passed ===" -ForegroundColor Green
