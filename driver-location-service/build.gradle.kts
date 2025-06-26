plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("com.rideservice.location.DriverLocationServiceKt")
}

dependencies {
    api("com.uber:h3:3.7.2")
}
