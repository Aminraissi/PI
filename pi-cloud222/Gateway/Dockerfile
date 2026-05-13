
FROM maven:3.9.14-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN mvn -B -DskipTests package


FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
