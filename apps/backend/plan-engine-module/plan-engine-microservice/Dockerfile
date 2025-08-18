# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-21 AS build

# Capa de dependencias (cacheable)
WORKDIR /app
COPY pom.xml /app/pom.xml
# Copia poms de submódulos para resolver deps sin copiar todo el código aún
COPY plan-engine-microservice-domain/pom.xml /app/plan-engine-microservice-domain/pom.xml
COPY plan-engine-microservice-application/pom.xml /app/plan-engine-microservice-application/pom.xml
COPY plan-engine-microservice-infra-persistence/pom.xml /app/plan-engine-microservice-infra-persistence/pom.xml
COPY plan-engine-microservice-infra-api-rest/pom.xml /app/plan-engine-microservice-infra-api-rest/pom.xml
COPY plan-engine-microservice-boot/pom.xml /app/plan-engine-microservice-boot/pom.xml

RUN mvn -q -DskipTests -DskipITs dependency:go-offline

# Copia del código fuente
COPY . /app

# Empaquetar SOLO el módulo boot (y sus dependencias) sin tests
RUN mvn -q -DskipTests -DskipITs -T 1C clean package -pl plan-engine-microservice-boot -am

# -------- Runtime stage --------
FROM openjdk:21-jdk-slim
WORKDIR /opt/app

# Copia el JAR del módulo boot (siempre el correcto, no tests)
COPY --from=build /app/plan-engine-microservice-boot/target/plan-engine-microservice-boot-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
