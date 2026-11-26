# DriveEasy - demarrage de tout le backend sous Windows (PowerShell)
# Usage : clic droit > "Executer avec PowerShell"  ou  .\start-all.ps1
# Ordre impose : Zipkin -> Eureka -> Config Server -> microservices -> Gateway
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# --- Verification de Java 21 ---
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java introuvable : installez le JDK 21 (https://adoptium.net) puis rouvrez PowerShell."
}
$versionJava = (cmd /c "java -version 2>&1" | Select-Object -First 1)
Write-Host "Java detecte : $versionJava"

# --- Maven : 'mvn' s'il est installe, sinon le Maven Wrapper du projet (telecharge Maven automatiquement) ---
if (Get-Command mvn -ErrorAction SilentlyContinue) { $maven = "mvn" } else { $maven = ".\mvnw.cmd"; Write-Host "Maven non installe : utilisation du Maven Wrapper (mvnw.cmd)" -ForegroundColor Yellow }

Write-Host "== Compilation ($maven clean package) ==" -ForegroundColor Cyan
& $maven -q clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Echec de la compilation Maven" }

Write-Host "== Zipkin (Docker) ==" -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) { Write-Warning "Docker indisponible : les services demarrent sans Zipkin." }

function Lancer-Module($module, $port) {
    $jar = Get-ChildItem "$module\target\$module-*.jar" | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
    Write-Host "Demarrage de $module (port $port)..." -ForegroundColor Green
    Start-Process -FilePath "java" -ArgumentList "-Xmx256m", "-jar", $jar.FullName -WorkingDirectory $PSScriptRoot `
        -RedirectStandardOutput "logs\$module.log" -RedirectStandardError "logs\$module.err.log" -WindowStyle Hidden
    $ok = $false
    for ($i = 0; $i -lt 90; $i++) {
        try { Invoke-WebRequest -UseBasicParsing "http://localhost:$port/actuator/health" -TimeoutSec 2 | Out-Null; $ok = $true; break }
        catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ok) { throw "$module ne repond pas sur le port $port (voir logs\$module.log)" }
    Write-Host "  $module OK" -ForegroundColor Green
}

New-Item -ItemType Directory -Force -Path logs | Out-Null
Lancer-Module "service-registry" 8761
Lancer-Module "config-server" 8888
Lancer-Module "vehicule-service" 8081
Lancer-Module "client-service" 8082
Lancer-Module "location-service" 8083
Lancer-Module "api-gateway" 8080

Write-Host ""
Write-Host "Backend pret :" -ForegroundColor Cyan
Write-Host "  Eureka  : http://localhost:8761"
Write-Host "  Gateway : http://localhost:8080/api/vehicules"
Write-Host "  Zipkin  : http://localhost:9411"
Write-Host "Arret : .\stop-all.ps1"
