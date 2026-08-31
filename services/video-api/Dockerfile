FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl services/video-api -am package -DskipTests
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
COPY --from=build /src/services/video-api/target/video-api-*.jar /app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
