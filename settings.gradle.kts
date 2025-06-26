pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "RideBookingService"
include(
    "fare-estimator",
    "dispatch-service",
    "driver-location-service",
    "ride-api"
)
