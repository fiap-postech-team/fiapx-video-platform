FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl services/notification-worker -am package -DskipTests
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
COPY --from=build /src/services/notification-worker/target/notification-worker-*.jar /app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app.jar"]
