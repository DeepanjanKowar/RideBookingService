package com.rideservice.dispatch

import com.rideservice.location.DriverLocationIndex
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.concurrent.thread
import java.util.concurrent.locks.ReentrantLock

/**
 * Simple dispatcher that selects the best driver for a ride request.
 * Uses dummy driver data and mocked ETA/distance calculations.
 */
class Dispatcher(private val locationIndex: DriverLocationIndex = DriverLocationIndex()) {

    data class Driver(
        val id: String,
        var lat: Double,
        var lng: Double,
        val category: String,
        val rating: Double
    )

    data class RideRequest(
        val pickupLat: Double,
        val pickupLng: Double,
        val category: String
    )

    private val drivers = mutableMapOf<String, Driver>()

    /**
     * Registers a driver in the index with dummy information.
     */
    fun registerDriver(driver: Driver) {
        drivers[driver.id] = driver
        locationIndex.updateDriverLocation(driver.id, driver.lat, driver.lng)
    }

    /**
     * Finds the best driver for a ride request. Returns null when no driver
     * satisfies the constraints.
     */
    fun dispatch(request: RideRequest): Driver? {
        val nearbyIds = locationIndex.getDriversNear(request.pickupLat, request.pickupLng)
        val candidates = nearbyIds.mapNotNull { drivers[it] }
            .filter { it.category.equals(request.category, ignoreCase = true) }
            .filter { estimateEtaMinutes(it, request.pickupLat, request.pickupLng) < 5.0 }

        val alpha = 1.0
        val beta = 2.0
        val gamma = 3.0

        return candidates.minByOrNull { driver ->
            val eta = estimateEtaMinutes(driver, request.pickupLat, request.pickupLng)
            val dist = distanceKm(driver, request.pickupLat, request.pickupLng)
            alpha * eta + beta * dist - gamma * driver.rating
        }
    }

    /**
     * Attempts to dispatch a ride using a multicast strategy.
     *
     * The request is offered to the top [maxCandidates] drivers by ETA and each
     * driver randomly accepts or rejects within [timeoutMs]. The first driver to
     * accept is returned.
     */
    fun dispatchMulticast(
        request: RideRequest,
        maxCandidates: Int = 3,
        timeoutMs: Long = 3000
    ): Driver? {
        val nearbyIds = locationIndex.getDriversNear(request.pickupLat, request.pickupLng)
        val sorted = nearbyIds.mapNotNull { drivers[it] }
            .filter { it.category.equals(request.category, ignoreCase = true) }
            .map { it to estimateEtaMinutes(it, request.pickupLat, request.pickupLng) }
            .filter { it.second < 5.0 }
            .sortedBy { it.second }
            .take(maxCandidates)

        if (sorted.isEmpty()) {
            return null
        }

        val lock = ReentrantLock()
        var accepted: Driver? = null
        val threads = sorted.map { (driver, _) ->
            thread(start = true) {
                // Random delay simulating driver's response time
                val delay = Random.nextLong(timeoutMs)
                Thread.sleep(delay)

                // Check if someone already accepted
                lock.lock()
                try {
                    if (accepted != null) return@thread
                } finally {
                    lock.unlock()
                }

                if (Random.nextBoolean()) {
                    lock.lock()
                    try {
                        if (accepted == null) {
                            accepted = driver
                        }
                    } finally {
                        lock.unlock()
                    }
                }
            }
        }

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            lock.lock()
            val done = accepted != null
            lock.unlock()
            if (done) break
            Thread.sleep(50)
        }

        threads.forEach { it.join(10) }
        return accepted
    }

    /**
     * Mocked ETA calculation based on distance and a constant speed of 40km/h.
     */
    private fun estimateEtaMinutes(driver: Driver, destLat: Double, destLng: Double): Double {
        val dist = distanceKm(driver, destLat, destLng)
        return (dist / 40.0) * 60.0
    }

    /**
     * Mocked distance calculation using a simple haversine formula.
     */
    private fun distanceKm(driver: Driver, destLat: Double, destLng: Double): Double {
        val R = 6371.0
        val lat1 = Math.toRadians(driver.lat)
        val lat2 = Math.toRadians(destLat)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(destLng - driver.lng)
        val a = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
