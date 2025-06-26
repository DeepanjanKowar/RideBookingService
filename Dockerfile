# Build stage - use Gradle with JDK to compile the project
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :ride-api:installDist --no-daemon

# Runtime image using Kotlin JDK
FROM kotlin:latest-jdk
WORKDIR /app
COPY --from=build /app/ride-api/build/install/ride-api/ .
EXPOSE 8080
CMD ["bin/ride-api"]
