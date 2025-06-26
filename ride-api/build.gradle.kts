plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

application {
    mainClass.set("com.rideservice.api.RideApiKt")
}

dependencies {
    implementation(project(":fare-estimator"))
    implementation(project(":dispatch-service"))
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.insert-koin:koin-ktor:3.5.0")
    implementation("ch.qos.logback:logback-classic:1.5.5")
}
