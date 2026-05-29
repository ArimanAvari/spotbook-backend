package com.spotbook.backend.sync

import com.spotbook.backend.places.PlaceStatus
import kotlinx.serialization.Serializable

@Serializable
data class SyncExportRequest(
    val groups: List<SyncGroupItem> = emptyList(),
    val places: List<SyncPlaceItem> = emptyList()
)

@Serializable
data class SyncGroupItem(
    val localId: Long,
    val serverId: Long? = null,
    val name: String,
    val syncStatus: SyncStatus,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class SyncPlaceItem(
    val localId: Long,
    val serverId: Long? = null,
    val title: String,
    val address: String,
    val photoPath: String? = null,
    val rating: Int,
    val comment: String,
    val status: PlaceStatus,
    val groupLocalId: Long? = null,
    val groupServerId: Long? = null,
    val syncStatus: SyncStatus,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class SyncExportResponse(
    val groups: List<SyncIdMapping>,
    val places: List<SyncIdMapping>,
    val data: SyncImportResponse
)

@Serializable
data class SyncIdMapping(
    val localId: Long,
    val serverId: Long?
)

@Serializable
data class SyncImportResponse(
    val groups: List<ServerGroupDto>,
    val places: List<ServerPlaceDto>
)

@Serializable
data class ServerGroupDto(
    val serverId: Long,
    val name: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ServerPlaceDto(
    val serverId: Long,
    val title: String,
    val address: String,
    val photoPath: String?,
    val rating: Int,
    val comment: String,
    val status: PlaceStatus,
    val groupServerId: Long?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class SyncErrorResponse(
    val error: String
)

enum class SyncStatus {
    SYNCED,
    NOT_SYNCED,
    DELETED
}

