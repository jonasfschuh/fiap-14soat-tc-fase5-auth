FROM eclipse-temurin:21-jre-alpine AS runtime

# ── New Relic Java Agent ────────────────────────────────────────────────────
ARG NEW_RELIC_AGENT_VERSION=8.18.0
RUN apk add --no-cache curl \
    && mkdir -p /app/newrelic \
    && curl -sSL \
       "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/${NEW_RELIC_AGENT_VERSION}/newrelic-agent-${NEW_RELIC_AGENT_VERSION}.jar" \
       -o /app/newrelic/newrelic.jar

WORKDIR /app
COPY application/target/auth-service-application-*-exec.jar app.jar
COPY newrelic/newrelic.yml /app/newrelic/newrelic.yml
EXPOSE 8090
ENTRYPOINT ["java", \
  "-javaagent:/app/newrelic/newrelic.jar", \
  "-Dnewrelic.config.file=/app/newrelic/newrelic.yml", \
  "-jar", "app.jar"]
