FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src src
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

RUN chown app:app app.jar
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]