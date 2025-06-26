package com.rideservice.simulator

import com.rideservice.dispatch.Dispatcher
import com.rideservice.location.DriverLocationIndex
import com.rideservice.fare.FareEstimator
import com.rideservice.fare.SurgeEngine
import kotlin.random.Random
import kotlin.math.pow

/**
 * Simple simulator that demonstrates the ride booking flow using the existing
 * modules in this repository.
 */
fun main() {
    val locationIndex = DriverLocationIndex()
    val dispatcher = Dispatcher(locationIndex)
    val surgeEngine = SurgeEngine()
    val fareEstimator = FareEstimator(surgeEngine = surgeEngine)

    // Generate mock drivers
    val categories = listOf("Go", "Sedan", "SUV")
    val baseLat = 12.9611
    val baseLng = 77.6387
    repeat(10) { i ->
        val lat = baseLat + Random.nextDouble(-0.01, 0.01)
        val lng = baseLng + Random.nextDouble(-0.01, 0.01)
        val cat = categories.random()
        val rating = Random.nextDouble(3.5, 5.0)
        val driver = Dispatcher.Driver("driver${i + 1}", lat, lng, cat, rating)
        dispatcher.registerDriver(driver)
    }

    // Create a ride request
    val pickupLat = baseLat + Random.nextDouble(-0.005, 0.005)
    val pickupLng = baseLng + Random.nextDouble(-0.005, 0.005)
    val dropLat = baseLat + Random.nextDouble(0.02, 0.04)
    val dropLng = baseLng + Random.nextDouble(0.02, 0.04)
    val category = categories.random()

    // Estimate fare
    val distanceKm = haversine(pickupLat, pickupLng, dropLat, dropLng)
    val durationMin = (distanceKm / 40.0) * 60.0
    val fare = fareEstimator.estimateFare(
        distanceKm,
        durationMin,
        category,
        pickupLat = pickupLat,
        pickupLng = pickupLng
    )
    println("Fare estimated: %.2f".format(fare))

    // Nearby drivers
    val nearby = locationIndex.getDriversNear(pickupLat, pickupLng)
    println("Nearby drivers found: ${nearby.joinToString()}")

    // Dispatch driver (set to true for multicast mode)
    val useMulticast = true
    val request = Dispatcher.RideRequest(pickupLat, pickupLng, category)
    val matched = if (useMulticast) {
        dispatcher.dispatchMulticast(request)
    } else {
        dispatcher.dispatch(request)
    }
    println("Driver matched: ${matched?.id ?: "none"}")

    if (matched != null) {
        println("Ride assigned to ${matched.id} from ($pickupLat,$pickupLng) to ($dropLat,$dropLng)")
    } else {
        println("No driver available for the ride request")
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).pow(2.0) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).pow(2.0)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return R * c
}
