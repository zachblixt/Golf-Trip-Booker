# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Dependencies resolve in their own layer, so editing a .java file does not
# re-download every dependency on the next build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests are skipped here deliberately. The 47 repository tests need a live
# MySQL running test-schema.sql, and an image build has no database to talk
# to. CI runs them against a MySQL service container instead.
RUN mvn -B clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home spring
COPY --from=build --chown=spring:spring /app/target/golf-trip-booker.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
