FROM gradle:8.10.2-jdk21
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
EXPOSE 8081 5006
CMD ["./gradlew", "bootRun", "--continuous", "--no-daemon"]
