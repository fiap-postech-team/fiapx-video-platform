FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl services/video-processor -am package -DskipTests
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 app
COPY --from=build /src/services/video-processor/target/video-processor-*.jar /app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app.jar"]
