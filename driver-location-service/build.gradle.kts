plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("com.rideservice.location.DriverLocationServiceKt")
}

dependencies {
    implementation("com.uber:h3:3.7.2")
}
