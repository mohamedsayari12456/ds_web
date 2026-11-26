# Verifie les logiciels installes ; resultat dans logs\env.txt
$sortie = "logs\env.txt"
New-Item -ItemType Directory -Force -Path logs | Out-Null
& {
  "== $(Get-Date)"
  "-- java";   cmd /c "java -version 2>&1"
  "-- JAVA_HOME=$env:JAVA_HOME"
  "-- mvn";    (Get-Command mvn -ErrorAction SilentlyContinue).Source
  "-- node";   cmd /c "node -v 2>&1"; cmd /c "npm -v 2>&1"
  "-- docker"; cmd /c "docker version --format {{.Server.Version}} 2>&1"
  "-- JDK presents"
  Get-ChildItem "C:\Program Files\Java","C:\Program Files\Eclipse Adoptium","C:\Program Files\Microsoft","C:\Program Files\Zulu" -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "jdk|java" } | ForEach-Object { $_.FullName }
  Get-ChildItem "D:\IntelliJ IDEA 2026.3\jbr" -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
  Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
  "== FIN"
} *>&1 | Tee-Object -FilePath $sortie
