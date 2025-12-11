# === STAGE 1: build cu JDK 25 + Maven ===
FROM eclipse-temurin:25-jdk-alpine AS build

# Instalăm Maven
RUN apk update && apk add --no-cache maven

WORKDIR /app

# Copiem TOT proiectul (pom.xml din root + module)
COPY . .

# Build pe modulul "application" cu toate deps (-am)
RUN mvn -pl application -am clean package -DskipTests


# === STAGE 2: runtime, doar JRE 25 ===
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copiem doar jar-ul final din stage-ul de build
COPY --from=build /app/application/target/jobshunter-1.0.0.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8081

CMD ["java","-jar","/app/app.jar"]
