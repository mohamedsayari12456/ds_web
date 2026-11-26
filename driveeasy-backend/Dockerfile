# Image unique contenant les 6 composants DriveEasy (Java 21).
# Etape 1 : compilation Maven ; etape 2 : JRE 21 + jars + config-repo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY service-registry/pom.xml service-registry/
COPY config-server/pom.xml config-server/
COPY vehicule-service/pom.xml vehicule-service/
COPY client-service/pom.xml client-service/
COPY location-service/pom.xml location-service/
COPY api-gateway/pom.xml api-gateway/
COPY config-repo config-repo
RUN mvn -q -B dependency:go-offline -DskipTests || true
COPY . .
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/service-registry/target/service-registry-1.0.0.jar /app/
COPY --from=build /src/config-server/target/config-server-1.0.0.jar /app/
COPY --from=build /src/vehicule-service/target/vehicule-service-1.0.0.jar /app/
COPY --from=build /src/client-service/target/client-service-1.0.0.jar /app/
COPY --from=build /src/location-service/target/location-service-1.0.0.jar /app/
COPY --from=build /src/api-gateway/target/api-gateway-1.0.0.jar /app/
COPY config-repo /app/config-repo
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseSerialGC"
