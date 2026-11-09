# HackTheSnackDB API — container image (used by Render, works anywhere).
#
#   docker build -t hackthesnackdb-api .
#   docker run -p 8087:8087 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... hackthesnackdb-api

# ---- build stage -------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app/ms-java-user-management-pecus

# Download dependencies first so they are cached between source changes.
COPY ms-java-user-management-pecus/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY ms-java-user-management-pecus/src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 10001 app
COPY --from=build /app/ms-java-user-management-pecus/target/ms-java-user-management-pecus-0.0.1.jar app.jar
USER app

# Render injects PORT; locally the app falls back to 8087.
ENV PORT=8087
EXPOSE 8087

# Tuned for small instances (Render free: 512 MB RAM, shared CPU):
# heap capped at 70 % of the container, serial GC, and C1-only JIT for a faster cold start.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

CMD ["java", "-jar", "app.jar"]
