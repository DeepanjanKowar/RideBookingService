# RideBookingService

This repository contains a simple Kotlin multi-module Gradle setup with modules:
- fare-estimator
- dispatch-service
- driver-location-service

Each module provides a main class for its respective service. The `fare-estimator` module
also exposes a `FareEstimator` class that calculates ride costs based on distance,
duration and ride category.
