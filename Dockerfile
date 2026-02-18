# backend/Dockerfile

# ÉTAPE 1: BUILD
# Utilisation d'une image Maven avec JDK 17 pour compiler l'application
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Définition du répertoire de travail dans le conteneur
WORKDIR /app

# Optimisation: Copie d'abord les fichiers de dépendances (pom.xml)
# Cela permet de mettre en cache les dépendances si le pom.xml n'a pas changé
COPY pom.xml .

# Téléchargement des dépendances (sans compiler le code)
# Cette étape sera mise en cache tant que pom.xml reste inchangé
RUN mvn dependency:go-offline

# Copie du code source
COPY src ./src

# Compilation et packaging de l'application
# -DskipTests permet d'ignorer les tests pour accélérer le build
# (les tests devraient déjà avoir été exécutés dans le pipeline CI)
RUN mvn clean package -DskipTests

# ÉTAPE 2: IMAGE FINALE (plus légère)
# Utilisation d'une image JRE légère pour l'exécution
FROM eclipse-temurin:17-jre-alpine

# Informations sur l'image (optionnel)
LABEL maintainer="votre-email@exemple.com"
LABEL version="1.0"
LABEL description="Backend Spring Boot de votre application"

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Définition du répertoire de travail
WORKDIR /app

# Copie du JAR depuis l'étape de build
# Le wildcard (*) permet de prendre le premier fichier .jar trouvé
COPY --from=builder /app/target/*.jar app.jar

# Exposition du port sur lequel l'application tourne
# (à ajuster selon votre configuration, 8080 est la valeur par défaut)
EXPOSE 8080

# Variables d'environnement pour la configuration (optionnel)
# Ces valeurs peuvent être surchargées au moment de l'exécution
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Commande d'exécution
# Utilisation de exec form pour une meilleure gestion des signaux
ENTRYPOINT ["java", \
            "-jar", \
            "/app/app.jar"]

# Alternative avec plus d'options JVM:
# ENTRYPOINT ["java", \
#             "-Xms512m", \
#             "-Xmx1024m", \
#             "-XX:+UseG1GC", \
#             "-jar", \
#             "/app/app.jar"]