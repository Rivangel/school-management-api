# Etapa 1: compilación
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Descarga las dependencias primero para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Etapa 2: imagen final ligera
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/school-management-api-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
