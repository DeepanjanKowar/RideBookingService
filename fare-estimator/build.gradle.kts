plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

application {
    mainClass.set("com.rideservice.fare.FareEstimatorKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
