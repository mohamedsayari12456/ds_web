#!/usr/bin/env bash
# DriveEasy - demarrage du backend (Linux / macOS / Git Bash)
set -e
cd "$(dirname "$0")"
MVN=$(command -v mvn || echo ./mvnw)
$MVN -q clean package -DskipTests
docker compose up -d || echo "Docker indisponible : demarrage sans Zipkin"
mkdir -p logs
demarrer() {
  local module=$1 port=$2
  local jar=$(ls $module/target/$module-*.jar | grep -v original | head -1)
  echo "Demarrage de $module ($port)..."
  nohup java -Xmx256m -jar "$jar" > logs/$module.log 2>&1 &
  for i in $(seq 1 90); do
    curl -sf http://localhost:$port/actuator/health > /dev/null && { echo "  $module OK"; return 0; }
    sleep 2
  done
  echo "$module ne demarre pas (logs/$module.log)"; exit 1
}
demarrer service-registry 8761
demarrer config-server 8888
demarrer vehicule-service 8081
demarrer client-service 8082
demarrer location-service 8083
demarrer api-gateway 8080
echo "Backend pret : Eureka http://localhost:8761 | Gateway http://localhost:8080 | Zipkin http://localhost:9411"
