# Étape 1: Build avec Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2: Exécution
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Ajouter des outils de diagnostic
RUN apk add --no-cache curl netcat-openbsd

# Copier le JAR
COPY --from=build /app/target/*.jar app.jar

# Copier le script d'entrée
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8081

# Santé du conteneur
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Utiliser le script d'entrée
ENTRYPOINT ["/entrypoint.sh"]
