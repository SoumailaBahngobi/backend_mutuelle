# Étape 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2: Exécution
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Exposer le port
EXPOSE 8081

# Santé du conteneur
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

# Démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]