package com.rideservice.fare

import kotlin.test.Test
import kotlin.test.assertEquals

class FareEstimatorTest {
    private class StubSurgeEngine(private val factor: Double) : SurgeEngine() {
        override fun getSurgeMultiplier(lat: Double, lng: Double, resolution: Int): Double = factor
        override fun getSurgeMultiplier(cellId: Long): Double = factor
    }

    private val rateCard = mapOf("Test" to FareEstimator.Rate(base = 10.0, perKm = 2.0, perMin = 1.0))

    @Test
    fun surgeAppliedFromEngine() {
        val estimator = FareEstimator(rateCard, StubSurgeEngine(2.0))
        val fare = estimator.estimateFare(5.0, 10.0, "Test", pickupLat = 0.0, pickupLng = 0.0)
        val expected = (10.0 + 5.0 * 2.0 + 10.0 * 1.0) * 2.0
        assertEquals(expected, fare, 0.0001)
    }

    @Test
    fun negativeMultiplierFallsBackToOne() {
        val estimator = FareEstimator(rateCard, StubSurgeEngine(1.0))
        val fare = estimator.estimateFare(5.0, 5.0, "Test", surgeMultiplier = -2.0)
        val expected = 10.0 + 5.0 * 2.0 + 5.0 * 1.0
        assertEquals(expected, fare, 0.0001)
    }

    @Test
    fun highSurgeBeyondCapHandled() {
        val estimator = FareEstimator(rateCard, StubSurgeEngine(5.0))
        val fare = estimator.estimateFare(1.0, 0.0, "Test", pickupLat = 0.0, pickupLng = 0.0)
        val expected = (10.0 + 1.0 * 2.0 + 0.0) * 5.0
        assertEquals(expected, fare, 0.0001)
    }

    @Test
    fun fractionalFareRoundsUp() {
        val estimator = FareEstimator(rateCard, StubSurgeEngine(1.0))
        val fare = estimator.estimateFare(5.5, 3.2, "Test")
        val expected = 25.0
        assertEquals(expected, fare, 0.0001)
    }
}
