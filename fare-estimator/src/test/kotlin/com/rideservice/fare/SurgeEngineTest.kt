package com.rideservice.fare

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.random.Random

class SurgeEngineTest {
    private class StubRandom(vararg values: Int) : Random() {
        private val data = values.toList()
        private var index = 0
        override fun nextInt(from: Int, until: Int): Int = data[index++]
        override fun nextBits(bitCount: Int): Int = 0
    }

    @Test
    fun factorComputedWithoutDropOrCategory() {
        val engine = SurgeEngine(random = StubRandom(10, 5))
        val factor = engine.getSurgeMultiplier(1L)
        assertEquals(3.0, factor, 0.0001)
        assertEquals(3.0, engine.currentSurgeMap()[1L], 0.0001)
    }

    @Test
    fun factorWithDropAndCategoryUsesAllRandomValues() {
        val engine = SurgeEngine(random = StubRandom(8, 4, 2, 4, 2))
        val factor = engine.getSurgeMultiplier(10L, dropCellId = 20L, category = "Premium")
        assertEquals(4.0, factor, 0.0001)
        assertEquals(4.0, engine.currentSurgeMap()[10L], 0.0001)
    }

    @Test
    fun higherPickupDemandIncreasesSurge() {
        val lowDemand = SurgeEngine(random = StubRandom(5, 5))
        val highDemand = SurgeEngine(random = StubRandom(15, 5))

        val lowFactor = lowDemand.getSurgeMultiplier(1L)
        val highFactor = highDemand.getSurgeMultiplier(1L)

        assertTrue(highFactor > lowFactor, "Expected higher demand to yield larger surge")
    }

    @Test
    fun higherDropDemandLowersSurge() {
        val lowDrop = SurgeEngine(random = StubRandom(5, 5, 2, 4))
        val highDrop = SurgeEngine(random = StubRandom(5, 5, 8, 4))

        val lowFactor = lowDrop.getSurgeMultiplier(2L, dropCellId = 20L)
        val highFactor = highDrop.getSurgeMultiplier(2L, dropCellId = 20L)

        assertTrue(highFactor < lowFactor, "Expected higher drop demand to reduce surge")
    }

    @Test
    fun moreCategoryCarsReducesSurge() {
        val fewCars = SurgeEngine(random = StubRandom(10, 10, 2))
        val manyCars = SurgeEngine(random = StubRandom(10, 10, 10))

        val highFactor = fewCars.getSurgeMultiplier(3L, category = "Sedan")
        val lowFactor = manyCars.getSurgeMultiplier(3L, category = "Sedan")

        assertTrue(lowFactor < highFactor, "Expected more available cars to lower surge")
    }
}
