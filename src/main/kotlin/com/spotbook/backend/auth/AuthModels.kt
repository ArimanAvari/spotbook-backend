package com.spotbook.backend.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

data class StoredUser(
    val id: Long,
    val email: String,
    val passwordHash: String
)

