# DriveEasy - installe JDK 21 (Temurin), Maven 3.9.11 et Node.js 22 LTS dans le profil utilisateur
# (sans droits administrateur), puis configure JAVA_HOME, MAVEN_HOME et le PATH utilisateur.
# Journal : logs\install.txt
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$racine = Join-Path $env:USERPROFILE "dev-tools"
New-Item -ItemType Directory -Force -Path $racine | Out-Null
$tmp = Join-Path $env:TEMP "driveeasy-install"
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

function Installer($nom, $url, $zip, $testDossier) {
    $existant = Get-ChildItem $racine -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -like $testDossier } | Select-Object -First 1
    if ($existant) { Write-Host "$nom deja present : $($existant.FullName)"; return $existant.FullName }
    Write-Host "Telechargement de $nom : $url"
    $fichier = Join-Path $tmp $zip
    Invoke-WebRequest -Uri $url -OutFile $fichier -UseBasicParsing
    Write-Host "Extraction de $nom..."
    tar -xf $fichier -C $racine
    if ($LASTEXITCODE -ne 0) { throw "Echec de l'extraction de $nom" }
    $dossier = Get-ChildItem $racine -Directory | Where-Object { $_.Name -like $testDossier } | Select-Object -First 1
    Write-Host "$nom installe : $($dossier.FullName)"
    return $dossier.FullName
}

# 1. JDK 21 (Eclipse Temurin, derniere version 21 GA)
$java = Installer "JDK 21 (Temurin)" "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" "jdk21.zip" "jdk-21*"

# 2. Maven 3.9.11
$maven = Installer "Maven 3.9.11" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip" "maven.zip" "apache-maven-3.9.11"

# 3. Node.js 22 LTS (derniere 22.x)
$index = Invoke-RestMethod "https://nodejs.org/dist/index.json"
$v22 = ($index | Where-Object { $_.version -like "v22.*" -and $_.files -contains "win-x64-zip" } | Select-Object -First 1).version
$node = Installer "Node.js $v22" "https://nodejs.org/dist/$v22/node-$v22-win-x64.zip" "node.zip" "node-v22*-win-x64"

# 4. Variables d'environnement utilisateur
[Environment]::SetEnvironmentVariable("JAVA_HOME", $java, "User")
[Environment]::SetEnvironmentVariable("MAVEN_HOME", $maven, "User")
$pathUser = [Environment]::GetEnvironmentVariable("Path", "User")
$entrees = @("$java\bin", "$maven\bin", $node, (Join-Path $env:APPDATA "npm"))
$parties = @($pathUser -split ";" | Where-Object { $_ -and ($_ -notlike "*dev-tools*") })
$nouveau = ($entrees + $parties) -join ";"
[Environment]::SetEnvironmentVariable("Path", $nouveau, "User")
Write-Host "JAVA_HOME=$java"
Write-Host "MAVEN_HOME=$maven"
Write-Host "PATH utilisateur mis a jour"

# 5. Verification dans la session courante
$env:JAVA_HOME = $java
$env:Path = "$java\bin;$maven\bin;$node;" + $env:Path
Write-Host "---- java"; cmd /c "java -version 2>&1"
Write-Host "---- mvn";  cmd /c "mvn -v 2>&1"
Write-Host "---- node"; cmd /c "node -v 2>&1"; cmd /c "npm -v 2>&1"
Write-Host "INSTALLATION TERMINEE"
