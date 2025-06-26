# RideBookingService

## Project Overview
RideBookingService is a sample Kotlin project composed of several modules that together simulate a small ride hailing backend. Each module can be run on its own and demonstrates a specific aspect of a typical ride booking platform.

## Features Implemented
- Driver location indexing using Uber's H3 library
- Fare estimation based on configurable rate cards
- Driver dispatching with adaptive batching and multicast selection
- Simple REST API built with Ktor for requesting rides and fare estimates
- Command line simulator showing the booking flow

## Modules & Structure
- **fare-estimator** – contains the `FareEstimator` class and rate card configuration located in `rates.json`.
- **driver-location-service** – implements `DriverLocationIndex` for storing driver positions using H3.
- **dispatch-service** – provides the `Dispatcher` and a small simulator to match drivers to ride requests.
- **ride-api** – exposes `/fare/estimate` and `/ride/request` endpoints using Ktor and integrates the other modules.

## How to Run Locally
1. Install JDK 17+ and Gradle.
2. Build the project:
   ```bash
   gradle build
   ```
3. Start the API server:
   ```bash
   gradle :ride-api:run
   ```
   The server will run on `http://localhost:8080`.

### IDE Setup
When opening the project in IntelliJ IDEA, make sure to **import** it as a Gradle
project. If the `build.gradle.kts` files show `Unresolved reference: plugins`, it
usually means the Kotlin Gradle plugin has not been downloaded yet. Running a
Gradle build once (e.g. `gradle build`) with internet access will allow Gradle
to fetch the required plugin jars so the IDE can resolve the DSL properly.

## Sample Ride Request JSON
```json
{
  "pickupLat": 12.9611,
  "pickupLng": 77.6387,
  "category": "Sedan"
}
```

## Sample Fare Estimate JSON
```json
{
  "pickupLat": 12.9611,
  "pickupLng": 77.6387,
  "dropLat": 12.9620,
  "dropLng": 77.6410,
  "category": "Go"
}
```

## Future Improvements
- Persist driver and ride data in a database
- Real-time driver location updates
- Authentication and user management
- More comprehensive testing
- Enhanced dispatch and pricing algorithms

