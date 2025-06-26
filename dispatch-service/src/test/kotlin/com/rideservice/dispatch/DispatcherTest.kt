package com.rideservice.dispatch

import com.rideservice.location.DriverLocationIndex
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DispatcherTest {
    private class BooleanRandom(private val values: MutableList<Boolean>) : Random() {
        override fun nextBits(bitCount: Int): Int {
            if (bitCount == 1 && values.isNotEmpty()) {
                return if (values.removeAt(0)) 1 else 0
            }
            return 0
        }
    }

    @Test
    fun noDriversReturnsNull() {
        val dispatcher = Dispatcher(DriverLocationIndex(), random = BooleanRandom(mutableListOf()))
        val req = Dispatcher.RideRequest(0.0, 0.0, "Sedan")
        assertNull(dispatcher.dispatch(req))
    }

    @Test
    fun unicastSelectsClosestDriver() {
        val index = DriverLocationIndex()
        val dispatcher = Dispatcher(index, random = BooleanRandom(mutableListOf()))
        dispatcher.registerDriver(Dispatcher.Driver("d1", 1.0, 1.0, "Sedan", 4.5))
        dispatcher.registerDriver(Dispatcher.Driver("d2", 2.0, 2.0, "Sedan", 5.0))
        val req = Dispatcher.RideRequest(1.0, 1.0, "Sedan")
        val result = dispatcher.dispatch(req)
        assertNotNull(result)
        assertEquals("d1", result.id)
    }

    @Test
    fun multicastAllRejectReturnsNull() {
        val index = DriverLocationIndex()
        val dispatcher = Dispatcher(index, random = BooleanRandom(mutableListOf(false, false)))
        dispatcher.registerDriver(Dispatcher.Driver("d1", 1.0, 1.0, "Sedan", 4.5))
        dispatcher.registerDriver(Dispatcher.Driver("d2", 1.0, 1.0005, "Sedan", 4.0))
        val req = Dispatcher.RideRequest(1.0, 1.0, "Sedan")
        val result = dispatcher.dispatchMulticast(req, maxCandidates = 2, timeoutMs = 100)
        assertNull(result)
    }

    @Test
    fun multicastReturnsFirstAcceptingDriver() {
        val index = DriverLocationIndex()
        val dispatcher = Dispatcher(index, random = BooleanRandom(mutableListOf(true)))
        dispatcher.registerDriver(Dispatcher.Driver("d1", 1.0, 1.0, "Sedan", 4.5))
        val req = Dispatcher.RideRequest(1.0, 1.0, "Sedan")
        val result = dispatcher.dispatchMulticast(req, maxCandidates = 1, timeoutMs = 100)
        assertNotNull(result)
        assertEquals("d1", result.id)
    }
}
