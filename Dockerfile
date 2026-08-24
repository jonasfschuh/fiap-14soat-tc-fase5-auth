FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY infrastructure/target/auth-service-*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
