# Arrete tous les services DriveEasy (processus java lances depuis ce dossier) et Zipkin
Set-Location $PSScriptRoot
Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -match "service-registry|config-server|vehicule-service|client-service|location-service|api-gateway" } |
    ForEach-Object { Write-Host "Arret de $($_.ProcessId)"; Stop-Process -Id $_.ProcessId -Force }
docker compose down
