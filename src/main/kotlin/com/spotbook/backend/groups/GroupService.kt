package com.spotbook.backend.groups

import com.spotbook.backend.places.PlaceRecord
import com.spotbook.backend.places.PlaceResponse
import io.ktor.http.HttpStatusCode

class GroupService(
    private val groupRepository: GroupRepository
) {
    fun getGroups(userId: Long): List<GroupResponse> {
        return groupRepository.findAll(userId).map { it.toResponse() }
    }

    fun createGroup(userId: Long, request: GroupRequest): GroupResponse {
        if (request.name.isBlank()) {
            throw GroupException(HttpStatusCode.BadRequest, "Group name is required")
        }

        return groupRepository.create(userId, request.name).toResponse()
    }

    fun deleteGroup(userId: Long, groupId: Long) {
        if (!groupRepository.delete(userId, groupId)) {
            throw GroupException(HttpStatusCode.NotFound, "Group was not found")
        }
    }

    fun addPlaceToGroup(userId: Long, groupId: Long, placeId: Long): GroupPlacesResponse {
        val group = findGroup(userId, groupId)
        ensurePlaceExists(userId, placeId)

        if (!groupRepository.addPlaceToGroup(userId, groupId, placeId)) {
            throw GroupException(HttpStatusCode.NotFound, "Place was not found")
        }

        return GroupPlacesResponse(
            group = group.toResponse(),
            places = groupRepository.findPlacesByGroup(userId, groupId).map { it.toResponse() }
        )
    }

    fun removePlaceFromGroup(userId: Long, groupId: Long, placeId: Long): GroupPlacesResponse {
        val group = findGroup(userId, groupId)
        ensurePlaceExists(userId, placeId)
        groupRepository.removePlaceFromGroup(userId, groupId, placeId)

        return GroupPlacesResponse(
            group = group.toResponse(),
            places = groupRepository.findPlacesByGroup(userId, groupId).map { it.toResponse() }
        )
    }

    fun getPlacesInGroup(userId: Long, groupId: Long): GroupPlacesResponse {
        val group = findGroup(userId, groupId)
        return GroupPlacesResponse(
            group = group.toResponse(),
            places = groupRepository.findPlacesByGroup(userId, groupId).map { it.toResponse() }
        )
    }

    private fun findGroup(userId: Long, groupId: Long): GroupRecord {
        return groupRepository.findById(userId, groupId)
            ?: throw GroupException(HttpStatusCode.NotFound, "Group was not found")
    }

    private fun ensurePlaceExists(userId: Long, placeId: Long) {
        if (!groupRepository.placeBelongsToUser(userId, placeId)) {
            throw GroupException(HttpStatusCode.NotFound, "Place was not found")
        }
    }

    private fun GroupRecord.toResponse(): GroupResponse {
        return GroupResponse(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PlaceRecord.toResponse(): PlaceResponse {
        return PlaceResponse(
            id = id,
            title = title,
            address = address,
            photoPath = photoPath,
            rating = rating,
            comment = comment,
            status = status,
            groupId = groupId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

class GroupException(
    val status: HttpStatusCode,
    override val message: String
) : RuntimeException(message)

