package com.rideservice.api

import com.rideservice.dispatch.Dispatcher
import com.rideservice.fare.FareEstimator
import com.rideservice.fare.SurgeEngine
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respond
import io.ktor.server.request.receive
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject

@Serializable
data class FareEstimateRequest(
    val pickupLat: Double,
    val pickupLng: Double,
    val dropLat: Double,
    val dropLng: Double,
    val category: String
)

@Serializable
data class FareEstimateResponse(val fare: Double)

@Serializable
data class RideRequestDto(val pickupLat: Double, val pickupLng: Double, val category: String)

@Serializable
data class DriverDto(val id: String, val lat: Double, val lng: Double, val category: String, val rating: Double)

fun Dispatcher.Driver.toDto() = DriverDto(id, lat, lng, category, rating)

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(Koin) {
        modules(
            module {
                single { SurgeEngine() }
                single { FareEstimator(surgeEngine = get()) }
                single { Dispatcher() }
            }
        )
    }

    val app = this

    // Register some sample drivers on startup so the dispatch endpoint can
    // return a driver instead of 404 when the system is empty.
    val dispatcher by app.inject<Dispatcher>()
    dispatcher.registerDriver(Dispatcher.Driver("d1", 12.9611, 77.6387, "Sedan", 4.5))
    dispatcher.registerDriver(Dispatcher.Driver("d2", 12.9620, 77.6410, "Go", 4.8))
    dispatcher.registerDriver(Dispatcher.Driver("d3", 12.9640, 77.6400, "Sedan", 4.2))
    dispatcher.registerDriver(Dispatcher.Driver("d4", 12.9650, 77.6390, "SUV", 4.6))
    dispatcher.registerDriver(Dispatcher.Driver("d5", 12.9660, 77.6420, "Go", 4.3))
    dispatcher.registerDriver(Dispatcher.Driver("d6", 12.9675, 77.6405, "Sedan", 4.9))
    dispatcher.registerDriver(Dispatcher.Driver("d7", 12.9683, 77.6375, "Go", 4.1))
    dispatcher.registerDriver(Dispatcher.Driver("d8", 12.9695, 77.6382, "SUV", 4.4))
    dispatcher.registerDriver(Dispatcher.Driver("d9", 12.9700, 77.6412, "Sedan", 4.7))
    dispatcher.registerDriver(Dispatcher.Driver("d10", 12.9710, 77.6398, "Go", 4.0))
    dispatcher.registerDriver(Dispatcher.Driver("d11", 12.9725, 77.6401, "SUV", 4.5))
    dispatcher.registerDriver(Dispatcher.Driver("d12", 12.9735, 77.6425, "Sedan", 4.6))
    dispatcher.registerDriver(Dispatcher.Driver("d13", 12.9740, 77.6380, "Go", 4.8))
    dispatcher.registerDriver(Dispatcher.Driver("d14", 12.9750, 77.6395, "SUV", 4.2))
    dispatcher.registerDriver(Dispatcher.Driver("d15", 12.9760, 77.6415, "Sedan", 4.7))

    routing {
        post("/fare/estimate") {
            val req = call.receive<FareEstimateRequest>()
            println("Received fare estimate request: $req")
            val fareEstimator by app.inject<FareEstimator>()
            // Distance and duration are fixed for demo purposes
            val distance = 10.0
            val duration = 20.0
            val fare = fareEstimator.estimateFare(
                distance,
                duration,
                req.category,
                pickupLat = req.pickupLat,
                pickupLng = req.pickupLng,
                dropLat = req.dropLat,
                dropLng = req.dropLng
            )
            println("Calculated fare: %.2f".format(fare))
            call.respond(FareEstimateResponse(fare))
        }

        post("/ride/request") {
            val req = call.receive<RideRequestDto>()
            println("Received ride request: $req")
            val dispatcher by app.inject<Dispatcher>()
            // Delegate driver selection to the dispatcher
            val driver = dispatcher.dispatch(
                Dispatcher.RideRequest(req.pickupLat, req.pickupLng, req.category)
            )
            if (driver != null) {
                println("Matched driver ${driver.id} for request")
                call.respond(driver.toDto())
            } else {
                println("No driver available for request")
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}
