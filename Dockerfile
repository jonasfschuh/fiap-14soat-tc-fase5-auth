FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY application/target/auth-service-application-*-exec.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
