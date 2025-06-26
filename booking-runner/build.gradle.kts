plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

application {
    mainClass.set("com.rideservice.runner.BookingRunnerKt")
}

dependencies {
    implementation(project(":driver-location-service"))
    implementation(project(":dispatch-service"))
    implementation(project(":fare-estimator"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
