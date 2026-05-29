package com.spotbook.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtService {
    private val issuer = "spotbook-backend"
    private val audience = "spotbook-users"
    private val secret = System.getenv("JWT_SECRET") ?: "spotbook-course-work-secret"
    private val expiresInMillis = 24L * 60L * 60L * 1000L
    private val algorithm = Algorithm.HMAC256(secret)

    fun verifier(): JWTVerifier {
        return JWT
            .require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    fun createToken(user: UserResponse): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id.toString())
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expiresInMillis))
            .sign(algorithm)
    }
}

