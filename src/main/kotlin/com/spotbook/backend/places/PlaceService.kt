@file:Suppress("DEPRECATION")

package com.spotbook.backend.places

import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.Input
import java.nio.file.Files
import java.nio.file.Path

class PlaceService(
    private val placeRepository: PlaceRepository,
    private val photoDirectory: Path = Path.of("uploads", "place_photos")
) {
    fun getPlaces(userId: Long): List<PlaceResponse> {
        return placeRepository.findAll(userId).map { it.toResponse() }
    }

    fun getPlace(userId: Long, placeId: Long): PlaceResponse {
        return placeRepository.findById(userId, placeId)?.toResponse()
            ?: throw PlaceException(HttpStatusCode.NotFound, "Place was not found")
    }

    fun createPlace(userId: Long, request: PlaceRequest): PlaceResponse {
        validateRequest(userId, request)
        return placeRepository.create(userId, request).toResponse()
    }

    fun updatePlace(userId: Long, placeId: Long, request: PlaceRequest): PlaceResponse {
        validateRequest(userId, request)
        return placeRepository.update(userId, placeId, request)?.toResponse()
            ?: throw PlaceException(HttpStatusCode.NotFound, "Place was not found")
    }

    fun deletePlace(userId: Long, placeId: Long) {
        if (!placeRepository.delete(userId, placeId)) {
            throw PlaceException(HttpStatusCode.NotFound, "Place was not found")
        }
    }

    fun updateStatus(userId: Long, placeId: Long, status: PlaceStatus): PlaceResponse {
        return placeRepository.updateStatus(userId, placeId, status)?.toResponse()
            ?: throw PlaceException(HttpStatusCode.NotFound, "Place was not found")
    }

    fun savePhoto(userId: Long, placeId: Long, originalFileName: String?, input: Input): PhotoUploadResponse {
        placeRepository.findById(userId, placeId)
            ?: throw PlaceException(HttpStatusCode.NotFound, "Place was not found")

        Files.createDirectories(photoDirectory)

        val extension = originalFileName
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() && it.length <= 8 }
            ?.lowercase()
            ?: "jpg"
        val fileName = "user_${userId}_place_${placeId}_${System.currentTimeMillis()}.$extension"
        val target = photoDirectory.resolve(fileName)

        input.use { source ->
            Files.newOutputStream(target).use { output ->
                while (source.canRead()) {
                    output.write(source.readByte().toInt())
                }
            }
        }

        val pathForDatabase = photoDirectory.resolve(fileName).toString().replace('\\', '/')
        placeRepository.updatePhoto(userId, placeId, pathForDatabase)
            ?: throw PlaceException(HttpStatusCode.NotFound, "Place was not found")

        return PhotoUploadResponse(photoPath = pathForDatabase)
    }

    private fun validateRequest(userId: Long, request: PlaceRequest) {
        if (request.title.isBlank()) {
            throw PlaceException(HttpStatusCode.BadRequest, "Title is required")
        }
        if (request.address.isBlank()) {
            throw PlaceException(HttpStatusCode.BadRequest, "Address is required")
        }
        if (request.rating !in 1..10) {
            throw PlaceException(HttpStatusCode.BadRequest, "Rating must be between 1 and 10")
        }
        if (request.groupId != null && !placeRepository.groupBelongsToUser(userId, request.groupId)) {
            throw PlaceException(HttpStatusCode.BadRequest, "Group was not found")
        }
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

class PlaceException(
    val status: HttpStatusCode,
    override val message: String
) : RuntimeException(message)
