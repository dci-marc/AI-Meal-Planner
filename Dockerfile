FROM maven:3.9.9-ibm-semeru-23-jammy AS build

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:23.0.1_11-jre-alpine

COPY --from=build /target/AI-Meal-Planner-0.0.1-SNAPSHOT.jar app.jar