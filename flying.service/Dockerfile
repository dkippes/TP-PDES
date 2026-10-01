# dev: el código se monta como volumen (docker-compose.yml, target: dev)
FROM gradle:8.10.2-jdk21 AS dev
WORKDIR /app
EXPOSE 8081
CMD ["sh", "./gradlew", "bootRun", "--no-daemon"]

FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY . .
RUN sh ./gradlew bootJar --no-daemon -x test

# runtime: imagen autocontenida que publica el CD (último stage = default)
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8081
ENV SERVER_PORT=8081
ENTRYPOINT ["java", "-jar", "app.jar"]
