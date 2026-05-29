package com.spotbook.backend.places

import kotlinx.serialization.Serializable

@Serializable
data class PlaceResponse(
    val id: Long,
    val title: String,
    val address: String,
    val photoPath: String?,
    val rating: Int,
    val comment: String,
    val status: PlaceStatus,
    val groupId: Long?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PlaceRequest(
    val title: String,
    val address: String,
    val rating: Int,
    val comment: String,
    val status: PlaceStatus,
    val groupId: Long? = null
)

@Serializable
data class UpdatePlaceStatusRequest(
    val status: PlaceStatus
)

@Serializable
data class PhotoUploadResponse(
    val photoPath: String
)

@Serializable
data class PlaceErrorResponse(
    val error: String
)

enum class PlaceStatus {
    WANT_TO_VISIT,
    VISITED
}

data class PlaceRecord(
    val id: Long,
    val userId: Long,
    val title: String,
    val address: String,
    val photoPath: String?,
    val rating: Int,
    val comment: String,
    val status: PlaceStatus,
    val groupId: Long?,
    val createdAt: String,
    val updatedAt: String
)

