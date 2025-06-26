package com.rideservice.runner

import com.rideservice.dispatch.Dispatcher
import com.rideservice.location.DriverLocationIndex
import com.rideservice.fare.FareEstimator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.*

@Serializable
data class DriverInput(val id: String, val lat: Double, val lng: Double, val category: String)

@Serializable
data class RideInput(
    val pickup_lat: Double,
    val pickup_lng: Double,
    val drop_lat: Double,
    val drop_lng: Double,
    val category: String
)

fun main() {
    val json = Json { ignoreUnknownKeys = true }

    val driversText = object {}.javaClass.classLoader.getResource("drivers.json")?.readText()
        ?: error("drivers.json not found")
    val ridesText = object {}.javaClass.classLoader.getResource("rides.json")?.readText()
        ?: error("rides.json not found")

    val drivers = json.decodeFromString<List<DriverInput>>(driversText)
    val rides = json.decodeFromString<List<RideInput>>(ridesText)

    val locationIndex = DriverLocationIndex()
    val dispatcher = Dispatcher(locationIndex)
    val fareEstimator = FareEstimator()

    // Index drivers
    drivers.forEach { d ->
        val driver = Dispatcher.Driver(d.id, d.lat, d.lng, d.category, rating = 4.5)
        dispatcher.registerDriver(driver)
    }

    // Process rides
    for (ride in rides) {
        val distance = haversine(ride.pickup_lat, ride.pickup_lng, ride.drop_lat, ride.drop_lng)
        val duration = (distance / 40.0) * 60.0
        val fare = fareEstimator.estimateFare(
            distance,
            duration,
            ride.category,
            pickupLat = ride.pickup_lat,
            pickupLng = ride.pickup_lng
        )

        val request = Dispatcher.RideRequest(ride.pickup_lat, ride.pickup_lng, ride.category)
        val driver = dispatcher.dispatch(request)

        if (driver != null) {
            println("Matched driver ${driver.id} for ride (${ride.pickup_lat},${ride.pickup_lng}) -> (${ride.drop_lat},${ride.drop_lng}) Fare: %.2f".format(fare))
        } else {
            println("No driver available for ride (${ride.pickup_lat},${ride.pickup_lng}) -> (${ride.drop_lat},${ride.drop_lng})")
        }
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
