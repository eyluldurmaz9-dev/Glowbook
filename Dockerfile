FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
