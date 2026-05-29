package com.spotbook.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    message = "SpotBook backend is running"
                )
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse(status = "ok", message = "OK"))
        }
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val message: String
)

