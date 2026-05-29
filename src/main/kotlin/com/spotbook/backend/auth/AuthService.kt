package com.spotbook.backend.auth

import io.ktor.http.HttpStatusCode

class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) {
    fun register(request: AuthRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        validatePassword(request.password)

        if (userRepository.findByEmail(email) != null) {
            throw AuthException(HttpStatusCode.Conflict, "Email is already registered")
        }

        val user = userRepository.create(
            email = email,
            passwordHash = PasswordHasher.hash(request.password)
        )

        return authResponse(user)
    }

    fun login(request: AuthRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        val user = userRepository.findByEmail(email)
            ?: throw AuthException(HttpStatusCode.Unauthorized, "Wrong email or password")

        if (!PasswordHasher.verify(request.password, user.passwordHash)) {
            throw AuthException(HttpStatusCode.Unauthorized, "Wrong email or password")
        }

        return authResponse(user)
    }

    fun findUser(userId: Long): UserResponse? {
        return userRepository.findById(userId)?.toResponse()
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
            email = email,
        )
    }
}

class AuthException(
    val status: HttpStatusCode,
    override val message: String
) : RuntimeException(message)
