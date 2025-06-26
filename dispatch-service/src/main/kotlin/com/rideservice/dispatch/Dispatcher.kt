package com.rideservice.dispatch

import com.rideservice.location.DriverLocationIndex
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantLock

/**
 * Simple dispatcher that selects the best driver for a ride request.
 * Uses dummy driver data and mocked ETA/distance calculations.
 */
class Dispatcher(
    private val locationIndex: DriverLocationIndex = DriverLocationIndex(),
    private val random: Random = Random.Default
) {

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

    // Maps H3 index to number of active ride requests and available drivers
    private val requestCounts = mutableMapOf<Long, Int>()
    private val driverCounts = mutableMapOf<Long, Int>()

    // Adaptive batching parameters
    private var batchIntervalMs: Long = 5000
    private var searchRadius: Int = 1
    private val statsLock = ReentrantLock()

    init {
        thread(isDaemon = true) {
            while (true) {
                runAdaptiveBatch()
                val delay = statsLock.withLock { batchIntervalMs }
                Thread.sleep(delay)
            }
        }
    }

    /**
     * Registers a driver in the index with dummy information.
     */
    fun registerDriver(driver: Driver) {
        drivers[driver.id] = driver
        locationIndex.updateDriverLocation(driver.id, driver.lat, driver.lng)
        val idx = locationIndex.latLngToIndex(driver.lat, driver.lng)
        statsLock.withLock {
            driverCounts[idx] = (driverCounts[idx] ?: 0) + 1
        }
    }

    /**
     * Finds the best driver for a ride request. Returns null when no driver
     * satisfies the constraints.
     */
    fun dispatch(request: RideRequest): Driver? {
        val nearbyIds = locationIndex.getDriversNear(request.pickupLat, request.pickupLng, searchRadius)
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
        val reqIdx = locationIndex.latLngToIndex(request.pickupLat, request.pickupLng)
        statsLock.withLock {
            requestCounts[reqIdx] = (requestCounts[reqIdx] ?: 0) + 1
        }
        val nearbyIds = locationIndex.getDriversNear(request.pickupLat, request.pickupLng, searchRadius)
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
                val delay = random.nextLong(timeoutMs)
                Thread.sleep(delay)

                // Check if someone already accepted
                lock.lock()
                try {
                    if (accepted != null) return@thread
                } finally {
                    lock.unlock()
                }

                if (random.nextBoolean()) {
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
        accepted?.let { acc ->
            val dIdx = locationIndex.latLngToIndex(acc.lat, acc.lng)
            statsLock.withLock {
                driverCounts[dIdx] = (driverCounts[dIdx] ?: 1) - 1
            }
        }
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

    /**
     * Runs periodically to adapt batch interval and search radius based on
     * demand/supply ratios observed in each H3 cell.
     */
    private fun runAdaptiveBatch() {
        statsLock.withLock {
            val cells = requestCounts.keys + driverCounts.keys
            for (cell in cells) {
                val demand = requestCounts[cell] ?: 0
                val supply = driverCounts[cell] ?: 0
                val ratio = if (supply == 0) Double.POSITIVE_INFINITY else demand.toDouble() / supply

                when {
                    ratio > 1.0 -> {
                        batchIntervalMs = 3000L
                        searchRadius = minOf(3, searchRadius + 1)
                    }
                    ratio < 0.5 -> {
                        batchIntervalMs = 7000L
                        searchRadius = maxOf(1, searchRadius - 1)
                    }
                    else -> {
                        batchIntervalMs = 5000L
                    }
                }
            }
            requestCounts.clear()
        }
    }
}
