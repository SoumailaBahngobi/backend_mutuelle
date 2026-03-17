FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar app.jar

# Ajouter curl pour le healthcheck
RUN apk add --no-cache curl

EXPOSE 8081

# Santé du conteneur avec plus de temps
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
