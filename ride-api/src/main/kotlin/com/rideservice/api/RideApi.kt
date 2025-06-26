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

    routing {
        post("/fare/estimate") {
            val req = call.receive<FareEstimateRequest>()
            val fareEstimator by app.inject<FareEstimator>()
            val distance = 10.0
            val duration = 20.0
            val fare = fareEstimator.estimateFare(
                distance,
                duration,
                req.category,
                pickupLat = req.pickupLat,
                pickupLng = req.pickupLng
            )
            call.respond(FareEstimateResponse(fare))
        }

        post("/ride/request") {
            val req = call.receive<RideRequestDto>()
            val dispatcher by app.inject<Dispatcher>()
            val driver = dispatcher.dispatch(
                Dispatcher.RideRequest(req.pickupLat, req.pickupLng, req.category)
            )
            if (driver != null) {
                call.respond(driver.toDto())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}
