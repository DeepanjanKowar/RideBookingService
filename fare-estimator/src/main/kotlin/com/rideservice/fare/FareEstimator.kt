package com.rideservice.fare

/**
 * Provides fare estimation for different ride categories using a simple rate card.
 */
class FareEstimator {
    private data class Rate(val base: Double, val perKm: Double, val perMin: Double)

    private val rateCard = mapOf(
        "Go" to Rate(base = 50.0, perKm = 15.0, perMin = 2.0),
        "Sedan" to Rate(base = 70.0, perKm = 18.0, perMin = 3.0),
        "SUV" to Rate(base = 90.0, perKm = 22.0, perMin = 4.0)
    )

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
        surgeMultiplier: Double = 1.0
    ): Double {
        require(distanceInKm >= 0) { "distanceInKm must be non-negative" }
        require(durationInMinutes >= 0) { "durationInMinutes must be non-negative" }

        val multiplier = if (surgeMultiplier > 0) surgeMultiplier else 1.0
        val rate = rateCard[category] ?: throw IllegalArgumentException("Unknown category: $category")

        return (rate.base + (distanceInKm * rate.perKm) + (durationInMinutes * rate.perMin)) * multiplier
    }
}

fun main() {
    println("Fare Estimator Service running")
}
