FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml lombok.config ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /app/target/drone-ops-1.0.0.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fs http://localhost:8080/api/dashboard/stats || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
