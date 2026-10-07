FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app/backend
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
COPY frontend ../frontend
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system apna && useradd --system --gid apna apna
COPY --from=build /app/backend/target/apna-bazaar-1.0.0.jar app.jar
USER apna
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
