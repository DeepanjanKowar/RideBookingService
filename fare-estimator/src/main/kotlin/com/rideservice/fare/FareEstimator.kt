package com.rideservice.fare

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Provides fare estimation for different ride categories using a configurable rate card.
 */
class FareEstimator(
    private val rateCard: Map<String, Rate> = RateCard.rates,
    private val surgeEngine: SurgeEngine = SurgeEngine()
) {
    @Serializable
    data class Rate(val base: Double, val perKm: Double, val perMin: Double)

    /**
     * Estimates the fare for a trip.
     *
     * @param distanceInKm distance travelled in kilometres
     * @param durationInMinutes trip duration in minutes
     * @param category ride category name
     * @param surgeMultiplier multiplier for surge pricing. Defaults to 1.0 when not provided or invalid
     * @return calculated fare
     */
    fun estimateFare(
        distanceInKm: Double,
        durationInMinutes: Double,
        category: String,
        surgeMultiplier: Double = 1.0,
        pickupLat: Double? = null,
        pickupLng: Double? = null
    ): Double {
        println("Estimating fare: distance=$distanceInKm km, duration=$durationInMinutes min, category=$category")
        require(distanceInKm >= 0) { "distanceInKm must be non-negative" }
        require(durationInMinutes >= 0) { "durationInMinutes must be non-negative" }

        val baseMultiplier = if (surgeMultiplier > 0) surgeMultiplier else 1.0
        val surgeFactor = if (pickupLat != null && pickupLng != null) {
            surgeEngine.getSurgeMultiplier(pickupLat, pickupLng)
        } else {
            1.0
        }
        val multiplier = baseMultiplier * surgeFactor
        val rate = rateCard[category]
            ?: throw IllegalArgumentException("Rate card missing category: $category")

        println("Using rate card base=${rate.base}, perKm=${rate.perKm}, perMin=${rate.perMin}")
        println("Surge factor=$surgeFactor}, base multiplier=$baseMultiplier}, total multiplier=$multiplier")

        val fare = (rate.base + (distanceInKm * rate.perKm) + (durationInMinutes * rate.perMin)) * multiplier
        println("Calculated fare amount: $fare")
        return fare
    }
}

/** Loads the rate card configuration from rates.json. */
object RateCard {
    val rates: Map<String, FareEstimator.Rate>

    init {
        val jsonText = RateCard::class.java.classLoader.getResource("rates.json")
            ?.readText() ?: throw IllegalStateException("rates.json not found")
        rates = Json.decodeFromString(jsonText)
    }
}

fun main() {
    println("Fare Estimator Service running")
}
