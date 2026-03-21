# Use Java 17 image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy maven wrapper and pom file
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Convert line endings of mvnw (in case building on Windows)
RUN sed -i 's/\r$//' mvnw

# Resolve dependencies (better caching)
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src ./src

# Build the application skipping tests
RUN ./mvnw clean package -DskipTests

# Run the notification service
CMD ["java", "-jar", "target/notification-service-0.0.1-SNAPSHOT.jar"]
