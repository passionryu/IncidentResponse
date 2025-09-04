## ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Pre-copy wrapper and gradle config to leverage Docker layer caching
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
COPY src src

RUN chmod +x gradlew \
  && ./gradlew --no-daemon clean bootJar

## ---------- Run stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]


