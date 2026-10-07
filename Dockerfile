FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src/main/java src/main/java
COPY src/main/resources src/main/resources
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system app
USER app

COPY --from=build /app/target/*.jar app.jar

ENV TZ=Europe/Rome
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "app.jar"]
