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
     * Returns the surge multiplier for a pickup and optional drop location.
     * Demand and supply are randomly generated for each cell to simulate
     * changing conditions. When the drop location is provided, the surge factor
     * is adjusted inversely to the demand at that destination. Optionally the
     * ride category can be provided which reduces available supply to mimic
     * category specific scarcity.
     */
    open fun getSurgeMultiplier(
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double? = null,
        dropLng: Double? = null,
        category: String? = null,
        resolution: Int = 9
    ): Double {
        val pickupCell = h3.geoToH3(pickupLat, pickupLng, resolution)
        val dropCell = if (dropLat != null && dropLng != null) {
            h3.geoToH3(dropLat, dropLng, resolution)
        } else {
            null
        }
        return getSurgeMultiplier(pickupCell, dropCell, category)
    }

    /**
     * Returns the surge multiplier for a specific H3 cell. When [dropCellId] or
     * [category] are provided they influence the computation as described in the
     * class documentation.
     */
    open fun getSurgeMultiplier(
        pickupCellId: Long,
        dropCellId: Long? = null,
        category: String? = null
    ): Double {
        val pickupDemand = random.nextInt(0, 20)
        val pickupSupply = random.nextInt(1, 20)
        val categorySupply = if (category != null) {
            // Fewer cars of the requested category increases surge
            random.nextInt(1, pickupSupply + 1)
        } else {
            pickupSupply
        }

        var factor = 1.0 + pickupDemand.toDouble() / categorySupply.toDouble()

        if (dropCellId != null) {
            val dropDemand = random.nextInt(0, 20)
            val dropSupply = random.nextInt(1, 20)
            val dropRatio = dropDemand.toDouble() / dropSupply.toDouble()
            // Higher demand in the drop cell lowers surge and vice versa
            factor += (1.0 - dropRatio)
        }

        surgeMap[pickupCellId] = factor
        println(
            "Surge calculation pickup=$pickupCellId drop=${dropCellId ?: "-"} " +
                "factor=$factor"
        )
        return factor
    }

    /** Compatibility overload that only requires a single cell id. */
    open fun getSurgeMultiplier(cellId: Long): Double =
        getSurgeMultiplier(cellId, null, null)

    /** Exposes the current surge factors for inspection. */
    fun currentSurgeMap(): Map<Long, Double> = surgeMap
}
