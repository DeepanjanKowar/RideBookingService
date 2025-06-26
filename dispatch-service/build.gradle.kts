plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("com.rideservice.dispatch.DispatchServiceKt")
}

dependencies {
    implementation(project(":driver-location-service"))
    implementation(project(":fare-estimator"))
}
