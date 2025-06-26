package com.rideservice.fare

import com.uber.h3core.H3Core
import kotlin.random.Random

/**
 * Simple engine that simulates surge pricing based on demand and supply per H3 cell.
 * The surge factor is calculated as `1 + (demand / supply)` and stored in-memory.
 */
open class SurgeEngine(
    private val h3: H3Core = H3Core.newInstance(),
    private val random: Random = Random.Default
) {
    private val surgeMap: MutableMap<Long, Double> = mutableMapOf()

    /**
     * Returns the surge multiplier for a given latitude and longitude. Random
     * demand and supply values are generated on each call to mimic fluctuations.
     */
    open fun getSurgeMultiplier(lat: Double, lng: Double, resolution: Int = 9): Double {
        val cellId = h3.geoToH3(lat, lng, resolution)
        return getSurgeMultiplier(cellId)
    }

    /**
     * Returns the surge multiplier for a specific H3 cell.
     */
    open fun getSurgeMultiplier(cellId: Long): Double {
        val demand = random.nextInt(0, 20)
        val supply = random.nextInt(1, 20)
        val factor = 1.0 + demand.toDouble() / supply.toDouble()
        surgeMap[cellId] = factor
        println("Surge calculation for cell ${'$'}cellId -> demand=${'$'}demand, supply=${'$'}supply, factor=${'$'}factor")
        return factor
    }

    /** Exposes the current surge factors for inspection. */
    fun currentSurgeMap(): Map<Long, Double> = surgeMap
}
