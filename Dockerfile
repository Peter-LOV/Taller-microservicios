# syntax=docker/dockerfile:1
# Una sola imagen parametrizable: docker build --build-arg MODULE=servicio-catalogo .
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests -pl ${MODULE} -am package

FROM eclipse-temurin:21-jre
ARG MODULE
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* && useradd -r -u 1001 app
WORKDIR /app
COPY --from=build /src/${MODULE}/target/*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
