package com.spotbook.backend.groups

import com.spotbook.backend.places.PlaceResponse
import kotlinx.serialization.Serializable

@Serializable
data class GroupRequest(
    val name: String
)

@Serializable
data class GroupResponse(
    val id: Long,
    val name: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class GroupPlacesResponse(
    val group: GroupResponse,
    val places: List<PlaceResponse>
)

@Serializable
data class GroupErrorResponse(
    val error: String
)

data class GroupRecord(
    val id: Long,
    val userId: Long,
    val name: String,
    val createdAt: String,
    val updatedAt: String
)

