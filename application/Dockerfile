FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/jobshunter-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8081
CMD ["java","-jar","/app/app.jar"]
