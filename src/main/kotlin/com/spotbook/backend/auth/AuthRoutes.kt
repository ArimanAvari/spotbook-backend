package com.spotbook.backend.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            call.runAuthAction {
                call.respond(HttpStatusCode.Created, authService.register(call.receive()))
            }
        }

        post("/login") {
            call.runAuthAction {
                call.respond(HttpStatusCode.OK, authService.login(call.receive()))
            }
        }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload
                    ?.getClaim("userId")
                    ?.asLong()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
                    return@get
                }

                val user = authService.findUser(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
                    return@get
                }

                call.respond(HttpStatusCode.OK, user)
            }
        }
    }
}

private suspend fun ApplicationCall.runAuthAction(block: suspend () -> Unit) {
    try {
        block()
    } catch (error: AuthException) {
        respond(error.status, ErrorResponse(error.message))
    }
}
