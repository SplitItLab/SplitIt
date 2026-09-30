FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle

RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon -q > /dev/null || true

COPY src src

RUN ./gradlew bootJar --no-daemon -x test \
    && jars="$(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar")" \
    && [ "$(echo "$jars" | grep -c .)" -eq 1 ] && cp "$jars" app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 1001 app

USER app

COPY --from=build --chown=app:app /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]