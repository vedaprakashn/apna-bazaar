# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S apna && adduser -S apna -G apna
USER apna
COPY --from=build /app/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
