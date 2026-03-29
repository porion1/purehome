# Use official OpenJDK image
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies first (cached)
COPY pom.xml .
COPY mvnw .
COPY .mvn/ .mvn/
RUN ./mvnw dependency:go-offline -B

# Copy all source code
COPY src ./src

# Build the app
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8081

# Run the app
ENTRYPOINT ["java","-jar","target/ui-core-service-1.0.0.jar"]