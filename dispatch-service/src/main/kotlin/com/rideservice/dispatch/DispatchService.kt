package com.rideservice.dispatch

fun main() {
    val dispatcher = Dispatcher()

    dispatcher.registerDriver(Dispatcher.Driver("d1", 12.9611, 77.6387, "Sedan", 4.5))
    dispatcher.registerDriver(Dispatcher.Driver("d2", 12.9620, 77.6410, "Go", 4.8))
    dispatcher.registerDriver(Dispatcher.Driver("d3", 12.9640, 77.6400, "Sedan", 4.2))

    val request = Dispatcher.RideRequest(12.9611, 77.6387, "Sedan")
    val driver = dispatcher.dispatch(request)

    println("Selected driver: ${driver?.id ?: "none"}")
}
