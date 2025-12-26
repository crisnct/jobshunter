# === STAGE 1: build cu JDK 25 + Maven ===
FROM eclipse-temurin:25-jdk-alpine AS build

# Instalăm Maven
RUN apk update && apk add --no-cache maven

WORKDIR /app

# Copiem TOT proiectul (pom.xml din root + module)
COPY . .

# Build pe modulul "application" cu toate deps (-am)
RUN mvn -pl application -am clean package -DskipTests

# Generează classlist și apoi arhiva AppCDS în timpul build-ului (nu la runtime)
RUN SPRING_PROFILES_ACTIVE=cds java -Xshare:off \
    -XX:DumpLoadedClassList=/tmp/app-cds.classlist \
    -Dapp.cds.warmup=true \
    -jar /app/application/target/jobshunter-1.0.0.jar \
    --spring.profiles.active=cds

# Creează arhiva AppCDS din classlist-ul generat
RUN java -Xshare:dump \
    -XX:SharedClassListFile=/tmp/app-cds.classlist \
    -XX:SharedArchiveFile=/tmp/app-cds.jsa


# === STAGE 2: runtime, folosim același JDK 25 pentru compatibilitate AppCDS ===
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

# Copiem jar-ul final și entrypoint-ul
COPY --from=build /app/application/target/jobshunter-1.0.0.jar /app/app.jar
COPY --from=build /tmp/app-cds.jsa /app/app-cds.jsa
COPY --from=build /tmp/app-cds.classlist /app/app-cds.classlist
COPY application/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8443

ENTRYPOINT ["/app/docker-entrypoint.sh"]
