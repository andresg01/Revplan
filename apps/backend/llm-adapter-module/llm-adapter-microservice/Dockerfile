# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app

# Capa de dependencias (cacheable)
COPY pom.xml /app/pom.xml
# Copia poms de submódulos (ajustados a tus módulos del LLM adapter)
COPY llm-adapter-microservice-domain/pom.xml /app/llm-adapter-microservice-domain/pom.xml
COPY llm-adapter-microservice-application/pom.xml /app/llm-adapter-microservice-application/pom.xml
COPY llm-adapter-microservice-infra-persistence/pom.xml /app/llm-adapter-microservice-infra-persistence/pom.xml
COPY llm-adapter-microservice-infra-api-rest/pom.xml /app/llm-adapter-microservice-infra-api-rest/pom.xml
COPY llm-adapter-microservice-boot/pom.xml /app/llm-adapter-microservice-boot/pom.xml

RUN mvn -q -DskipTests -DskipITs dependency:go-offline

# Copia del código fuente
COPY . /app

# Empaquetar SOLO el módulo boot (y sus dependencias) sin tests
RUN mvn -q -DskipTests -DskipITs -T 1C clean package -pl llm-adapter-microservice-boot -am

# -------- Runtime stage --------
FROM openjdk:21-jdk-slim
WORKDIR /opt/app

# Copia el JAR del módulo boot
COPY --from=build /app/llm-adapter-microservice-boot/target/llm-adapter-microservice-boot-*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
