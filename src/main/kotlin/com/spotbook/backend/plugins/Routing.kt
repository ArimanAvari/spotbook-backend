package com.spotbook.backend.plugins

import com.spotbook.backend.auth.AuthService
import com.spotbook.backend.auth.authRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

fun Application.configureRouting(authService: AuthService) {
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

        authRoutes(authService)
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val message: String
)
