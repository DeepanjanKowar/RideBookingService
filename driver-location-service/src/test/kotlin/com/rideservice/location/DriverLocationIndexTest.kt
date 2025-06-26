package com.rideservice.location

import com.uber.h3core.H3Core
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DriverLocationIndexTest {
    @Test
    fun latLngToIndexMatchesH3() {
        val index = DriverLocationIndex()
        val h3 = H3Core.newInstance()
        val lat = 12.9611
        val lng = 77.6387
        assertEquals(h3.geoToH3(lat, lng, 9), index.latLngToIndex(lat, lng))
    }

    @Test
    fun kRingSearchFindsNearbyDrivers() {
        val index = DriverLocationIndex()
        index.updateDriverLocation("d1", 12.9611, 77.6387)
        index.updateDriverLocation("d2", 12.9612, 77.6387)
        index.updateDriverLocation("d3", 0.0, 0.0)

        val nearby = index.getDriversNear(12.9611, 77.6387, 1)
        assertTrue("d1" in nearby)
        assertTrue("d2" in nearby)
        assertTrue("d3" !in nearby)

        val none = index.getDriversNear(89.0, 179.0, 1)
        assertTrue(none.isEmpty())
    }
}
