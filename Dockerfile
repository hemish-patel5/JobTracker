FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/jobtracker-0.0.1-SNAPSHOT.jar app.jar

# Render supplies PORT at runtime and defaults web services to port 10000.
# This ENV also gives the container the same default when run directly.
ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
