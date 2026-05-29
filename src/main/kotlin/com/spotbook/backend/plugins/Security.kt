package com.spotbook.backend.plugins

import com.spotbook.backend.auth.JwtService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity(jwtService: JwtService) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "spotbook"
            verifier(jwtService.verifier())
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asLong()
                val email = credential.payload.getClaim("email").asString()

                if (userId != null && !email.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
