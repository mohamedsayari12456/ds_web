# Recharge PATH / JAVA_HOME depuis le registre (utile si VS Code a ete ouvert avant l'installation)
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable("MAVEN_HOME", "User")
$env:Path = [Environment]::GetEnvironmentVariable("Path", "User") + ";" + [Environment]::GetEnvironmentVariable("Path", "Machine")
