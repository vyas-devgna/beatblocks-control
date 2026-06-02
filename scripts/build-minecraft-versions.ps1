param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputDir = "releases",
    [string]$TargetsFile = ""
)

$ErrorActionPreference = "Stop"

if (-not $TargetsFile) {
    $TargetsFile = Join-Path $PSScriptRoot "fabric-targets.json"
}

if (-not (Test-Path $TargetsFile)) {
    throw "Missing $TargetsFile. Run: python scripts/resolve-fabric-targets.py"
}

$targets = Get-Content $TargetsFile -Raw | ConvertFrom-Json

# Correct known resolver mistakes (suffix matching picked wrong fabric-api)
$fixes = @{
    "1.21.1" = "0.116.12+1.21.1"
    "1.21"   = "0.102.0+1.21"
}
foreach ($t in $targets) {
    if ($fixes.ContainsKey($t.Minecraft)) {
        $t.Fabric = $fixes[$t.Minecraft]
    }
}

if (-not $JavaHome) {
    $prismJava = Join-Path $env:APPDATA "PrismLauncher\java\java-runtime-delta"
    if (Test-Path (Join-Path $prismJava "bin\java.exe")) {
        $JavaHome = $prismJava
    }
}

if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Java 21 not found. Pass -JavaHome <path-to-jdk-21> or set JAVA_HOME."
}

$env:JAVA_HOME = $JavaHome
$env:PATH = (Join-Path $JavaHome "bin") + [IO.Path]::PathSeparator + $env:PATH

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$built = @()
$failed = @()

foreach ($target in $targets) {
    $mc = $target.Minecraft
    Write-Host "==> Building BeatBlocks for Minecraft $mc"
    try {
        & .\gradlew.bat clean build `
            "-Pminecraft_version=$($target.Minecraft)" `
            "-Pyarn_mappings=$($target.Yarn)" `
            "-Pfabric_version=$($target.Fabric)" `
            --no-daemon 2>&1 | Write-Host

        if ($LASTEXITCODE -ne 0) {
            throw "Gradle exit code $LASTEXITCODE"
        }

        $jar = Get-ChildItem "build\libs" -Filter "beatblocks-control-*.jar" |
            Where-Object { $_.Name -notmatch "sources|dev" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

        if (-not $jar) {
            throw "No remapped mod jar produced"
        }

        $dest = Join-Path $OutputDir ("beatblocks-control-mc-$mc.jar")
        Copy-Item -Force -Path $jar.FullName -Destination $dest
        Write-Host "    Wrote $dest" -ForegroundColor Green
        $built += $mc
    } catch {
        Write-Host "    FAILED $mc : $_" -ForegroundColor Red
        $failed += $mc
    }
}

Write-Host ""
Write-Host "Built: $($built -join ', ')"
if ($failed.Count -gt 0) {
    Write-Host "Failed: $($failed -join ', ')" -ForegroundColor Yellow
}
Write-Host "Jars in $OutputDir — upload to GitHub Releases; do not commit."