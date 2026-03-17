#!/bin/sh

echo "========================================="
echo "🔍 DIAGNOSTIC DU CONTENEUR"
echo "========================================="
echo "1. Variables d'environnement:"
env | grep -E "DB_|SPRING_|KEYCLOAK_|SERVER_"

echo -e "\n2. Test de connexion à PostgreSQL:"
if [ -z "$DB_URL" ]; then
    echo "❌ DB_URL non définie"
else
    echo "DB_URL = $DB_URL"
    # Extraire l'hôte et le port
    DB_HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\).*/\1/p')
    DB_PORT=$(echo $DB_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    
    echo "Tentative de connexion à $DB_HOST:$DB_PORT..."
    
    # Installer netcat si disponible
    apk add --no-cache netcat-openbsd > /dev/null 2>&1
    
    nc -zv $DB_HOST $DB_PORT 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Port PostgreSQL accessible"
    else
        echo "❌ Impossible d'accéder à PostgreSQL"
    fi
fi

echo -e "\n3. Lancement de l'application..."
echo "========================================="

# Lancer l'application
exec java -jar app.jar
