param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputDir = "releases"
)

$ErrorActionPreference = "Stop"

$targets = @(
    @{ Minecraft = "1.21.5"; Yarn = "1.21.5+build.1"; Fabric = "0.128.2+1.21.5" },
    @{ Minecraft = "1.21.4"; Yarn = "1.21.4+build.8"; Fabric = "0.119.4+1.21.4" },
    @{ Minecraft = "1.21.3"; Yarn = "1.21.3+build.2"; Fabric = "0.114.1+1.21.3" },
    @{ Minecraft = "1.21.2"; Yarn = "1.21.2+build.1"; Fabric = "0.106.1+1.21.2" },
    @{ Minecraft = "1.21.1"; Yarn = "1.21.1+build.3"; Fabric = "0.116.12+1.21.1" },
    @{ Minecraft = "1.21";   Yarn = "1.21+build.9";   Fabric = "0.102.0+1.21" }
)

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

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

foreach ($target in $targets) {
    $mc = $target.Minecraft
    Write-Host "==> Building BeatBlocks for Minecraft $mc"
    & .\gradlew.bat clean build `
        "-Pminecraft_version=$($target.Minecraft)" `
        "-Pyarn_mappings=$($target.Yarn)" `
        "-Pfabric_version=$($target.Fabric)"

    if ($LASTEXITCODE -ne 0) {
        throw "Build failed for Minecraft $mc"
    }

    $jar = Get-ChildItem "build\libs" -Filter "beatblocks-control-*.jar" |
        Where-Object { $_.Name -notmatch "sources|dev" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "No remapped mod jar produced for Minecraft $mc"
    }

    $dest = Join-Path $OutputDir ("beatblocks-control-mc-$mc.jar")
    Copy-Item -Force -Path $jar.FullName -Destination $dest
    Write-Host "    Wrote $dest"
}

Write-Host "All target jars are in $OutputDir. Upload them as GitHub Release assets; do not commit them."
