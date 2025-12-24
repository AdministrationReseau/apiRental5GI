# ==========================================
# Stage 1 : Build avec Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copie des fichiers de configuration Maven
COPY pom.xml .
# Copie du dossier .mvn s'il existe (wrapper), sinon on utilise le maven de l'image
COPY src src

# Construction du package (skip tests pour accélérer le build Docker)
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2 : Runtime (Image légère)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Installation de curl pour d'éventuels tests (optionnel)
RUN apk add --no-cache curl tzdata

# Configuration de la Timezone (Cameroun)
ENV TZ=Africa/Douala

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S rentalgroup && \
    adduser -u 1001 -S rentaluser -G rentalgroup

WORKDIR /app

# Copie du JAR généré depuis l'étape de build
# Note : On utilise un wildcard (*) pour ne pas dépendre de la version exacte
COPY --from=build /app/target/apirental-*.jar app.jar

# Création du dossier uploads avec les bonnes permissions
RUN mkdir -p /app/uploads && \
    chown -R rentaluser:rentalgroup /app

# Passage à l'utilisateur non-root
USER rentaluser

# Exposition du port (Spring Boot par défaut est 8080)
EXPOSE 8080

# Variables d'environnement par défaut (peuvent être surchargées par docker-compose)
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
