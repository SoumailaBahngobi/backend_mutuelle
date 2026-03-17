# Étape 1: Build avec Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier pom.xml et les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler l'application
RUN mvn clean package -DskipTests

# Étape 2: Exécution avec JRE
FROM eclipse-temurin:21-jre-alpine

# Définir le répertoire de travail
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port (8081 selon votre configuration)
EXPOSE 8081

# Variables d'environnement pour la configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8081

# Santé du conteneur
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

# Démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]