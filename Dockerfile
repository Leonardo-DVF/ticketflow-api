FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S ticketflow && adduser -S ticketflow -G ticketflow

COPY --from=build /workspace/target/*.jar app.jar

USER ticketflow

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
