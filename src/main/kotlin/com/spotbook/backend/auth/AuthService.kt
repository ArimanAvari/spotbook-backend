package com.spotbook.backend.auth

import io.ktor.http.HttpStatusCode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AuthService(
    private val jwtService: JwtService
) {
    private val nextId = AtomicLong(1)
    private val users = ConcurrentHashMap<Long, StoredUser>()
    private val userIdsByEmail = ConcurrentHashMap<String, Long>()

    @Synchronized
    fun register(request: AuthRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        validatePassword(request.password)

        if (userIdsByEmail.containsKey(email)) {
            throw AuthException(HttpStatusCode.Conflict, "Email is already registered")
        }

        val user = StoredUser(
            id = nextId.getAndIncrement(),
            email = email,
            passwordHash = PasswordHasher.hash(request.password)
        )

        users[user.id] = user
        userIdsByEmail[email] = user.id

        return authResponse(user)
    }

    fun login(request: AuthRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        val userId = userIdsByEmail[email]
            ?: throw AuthException(HttpStatusCode.Unauthorized, "Wrong email or password")
        val user = users[userId]
            ?: throw AuthException(HttpStatusCode.Unauthorized, "Wrong email or password")

        if (!PasswordHasher.verify(request.password, user.passwordHash)) {
            throw AuthException(HttpStatusCode.Unauthorized, "Wrong email or password")
        }

        return authResponse(user)
    }

    fun findUser(userId: Long): UserResponse? {
        return users[userId]?.toResponse()
    }

    private fun authResponse(user: StoredUser): AuthResponse {
        val response = user.toResponse()
        return AuthResponse(
            token = jwtService.createToken(response),
            user = response
        )
    }

    private fun normalizeEmail(email: String): String {
        val normalized = email.trim().lowercase()
        if (!normalized.contains("@") || normalized.length < 5) {
            throw AuthException(HttpStatusCode.BadRequest, "Email is invalid")
        }
        return normalized
    }

    private fun validatePassword(password: String) {
        if (password.length < 6) {
            throw AuthException(HttpStatusCode.BadRequest, "Password must contain at least 6 characters")
        }
    }

    private fun StoredUser.toResponse(): UserResponse {
        return UserResponse(
            id = id,
            email = email
        )
    }
}

class AuthException(
    val status: HttpStatusCode,
    override val message: String
) : RuntimeException(message)

