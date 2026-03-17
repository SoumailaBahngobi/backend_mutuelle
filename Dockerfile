# 1. Construire le projet Maven
./mvnw clean package -DskipTests

# 2. Construire l'image Docker
docker build -t mutuelle-backend .

# 3. Tester avec ElephantSQL
docker run -p 8081:8081 \
  -e DB_URL="jdbc:postgresql://tyke.db.elephantsql.com:5432/votre_user?sslmode=require" \
  -e DB_USERNAME="votre_user" \
  -e DB_PASSWORD="votre_password" \
  -e JPA_DDL_AUTO="update" \
  -e SERVER_PORT="8081" \
  mutuelle-backend

# 4. Vérifier que l'application répond
curl http://localhost:8081/actuator/health
