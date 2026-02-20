# Use eclipse-temurin for a lightweight JRE image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# The build process produces the executable fat jar with -exec classifier
COPY target/fix-mock-server-*-exec.jar app.jar

# Expose default Spring Boot port and default FIX acceptor port
EXPOSE 9090
EXPOSE 9899

ENTRYPOINT ["java", "-jar", "app.jar"]
