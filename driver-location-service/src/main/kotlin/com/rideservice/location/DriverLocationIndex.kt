package com.rideservice.location

import com.uber.h3core.H3Core

/**
 * Indexes driver locations using Uber's H3 library.
 */
class DriverLocationIndex(
    private val h3: H3Core = H3Core.newInstance(),
    private val resolution: Int = 9
) {
    private val indexToDrivers: MutableMap<Long, MutableSet<String>> = mutableMapOf()
    private val driverToIndex: MutableMap<String, Long> = mutableMapOf()

    /**
     * Maps latitude and longitude to an H3 index using resolution 9.
     */
    fun latLngToIndex(lat: Double, lng: Double, resolution: Int = this.resolution): Long =
        h3.geoToH3(lat, lng, resolution)

    /**
     * Updates the stored location for a driver.
     */
    fun updateDriverLocation(driverId: String, lat: Double, lng: Double) {
        val newIndex = latLngToIndex(lat, lng)
        val oldIndex = driverToIndex.put(driverId, newIndex)

        if (oldIndex != null && oldIndex != newIndex) {
            indexToDrivers[oldIndex]?.let { drivers ->
                drivers.remove(driverId)
                if (drivers.isEmpty()) {
                    indexToDrivers.remove(oldIndex)
                }
            }
        }

        indexToDrivers.computeIfAbsent(newIndex) { mutableSetOf() }.add(driverId)
    }

    /**
     * Retrieves IDs of drivers near a location. The search radius is controlled
     * by the kRing value which defaults to 1.
     */
    fun getDriversNear(lat: Double, lng: Double, kRing: Int = 1): Set<String> {
        val center = latLngToIndex(lat, lng)
        val indexes = h3.kRing(center, kRing)
        val result = mutableSetOf<String>()

        for (idx in indexes) {
            indexToDrivers[idx]?.let { result.addAll(it) }
        }

        return result
    }
}
