# ---------- BUILD STAGE ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Prevent Render or system from injecting broken Maven config
ENV MAVEN_CONFIG=""

# Copy Maven wrapper and config
COPY mvnw .
# Convert Windows CRLF → Linux LF inside Docker
RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw

COPY .mvn .mvn

# Copy pom.xml first (better caching)
COPY pom.xml .

# Force clear Maven config again to be safe
ENV MAVEN_CONFIG=""

RUN ./mvnw -B dependency:go-offline

# Copy source code
COPY src src

# Build Spring Boot application
RUN ./mvnw -B package -DskipTests

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
