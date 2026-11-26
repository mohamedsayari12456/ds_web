@echo off
rem DriveEasy - verifie les logiciels installes (resultat dans logs\env.txt)
cd /d "%~dp0.."
echo ==== %date% %time% > logs\env.txt
echo ---- java >> logs\env.txt
java -version >> logs\env.txt 2>&1
echo ---- JAVA_HOME=%JAVA_HOME% >> logs\env.txt
echo ---- mvn >> logs\env.txt
call mvn -v >> logs\env.txt 2>&1
echo ---- node >> logs\env.txt
node -v >> logs\env.txt 2>&1
call npm -v >> logs\env.txt 2>&1
echo ---- docker >> logs\env.txt
docker version >> logs\env.txt 2>&1
echo ---- where >> logs\env.txt
where java mvn node docker >> logs\env.txt 2>&1
dir /b "C:\Program Files\Java" >> logs\env.txt 2>&1
dir /b "C:\Program Files\Eclipse Adoptium" >> logs\env.txt 2>&1
dir /b "C:\Program Files\Docker" >> logs\env.txt 2>&1
dir /b "D:\IntelliJ IDEA 2026.3" >> logs\env.txt 2>&1
echo ==== FIN >> logs\env.txt
