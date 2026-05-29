package com.spotbook.backend.sync

import com.spotbook.backend.database.DatabaseFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

class SyncService(
    private val databaseFactory: DatabaseFactory
) {
    fun exportData(userId: Long, request: SyncExportRequest): SyncExportResponse {
        databaseFactory.connection().use { connection ->
            connection.autoCommit = false
            try {
                val groupMappings = mutableListOf<SyncIdMapping>()
                val localGroupToServer = mutableMapOf<Long, Long>()

                request.groups.forEach { group ->
                    val serverId = exportGroup(connection, userId, group)
                    groupMappings += SyncIdMapping(localId = group.localId, serverId = serverId)
                    if (serverId != null) {
                        localGroupToServer[group.localId] = serverId
                    }
                }

                val placeMappings = request.places.map { place ->
                    SyncIdMapping(
                        localId = place.localId,
                        serverId = exportPlace(connection, userId, place, localGroupToServer)
                    )
                }

                connection.commit()

                return SyncExportResponse(
                    groups = groupMappings,
                    places = placeMappings,
                    data = importData(userId)
                )
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun importData(userId: Long): SyncImportResponse {
        databaseFactory.connection().use { connection ->
            return SyncImportResponse(
                groups = loadGroups(connection, userId),
                places = loadPlaces(connection, userId)
            )
        }
    }

    private fun exportGroup(connection: Connection, userId: Long, group: SyncGroupItem): Long? {
        if (group.syncStatus == SyncStatus.DELETED) {
            if (group.serverId != null) {
                deleteGroup(connection, userId, group.serverId)
            }
            return null
        }

        validateGroup(group)

        if (group.serverId != null && groupExists(connection, userId, group.serverId)) {
            updateGroup(connection, userId, group)
            return group.serverId
        }

        return insertGroup(connection, userId, group)
    }

    private fun exportPlace(
        connection: Connection,
        userId: Long,
        place: SyncPlaceItem,
        localGroupToServer: Map<Long, Long>
    ): Long? {
        if (place.syncStatus == SyncStatus.DELETED) {
            if (place.serverId != null) {
                deletePlace(connection, userId, place.serverId)
            }
            return null
        }

        validatePlace(place)
        val groupId = resolveGroupId(place, localGroupToServer)
        if (groupId != null && !groupExists(connection, userId, groupId)) {
            throw SyncException("Group was not found")
        }

        if (place.serverId != null && placeExists(connection, userId, place.serverId)) {
            updatePlace(connection, userId, place, groupId)
            return place.serverId
        }

        return insertPlace(connection, userId, place, groupId)
    }

    private fun resolveGroupId(place: SyncPlaceItem, localGroupToServer: Map<Long, Long>): Long? {
        return place.groupServerId ?: place.groupLocalId?.let { localGroupToServer[it] }
    }

    private fun validateGroup(group: SyncGroupItem) {
        if (group.name.isBlank()) {
            throw SyncException("Group name is required")
        }
    }

    private fun validatePlace(place: SyncPlaceItem) {
        if (place.title.isBlank()) {
            throw SyncException("Place title is required")
        }
        if (place.address.isBlank()) {
            throw SyncException("Place address is required")
        }
        if (place.rating !in 1..5) {
            throw SyncException("Rating must be between 1 and 5")
        }
    }

    private fun insertGroup(connection: Connection, userId: Long, group: SyncGroupItem): Long {
        val now = Instant.now().toString()
        val createdAt = group.createdAt ?: now
        val updatedAt = group.updatedAt ?: now
        val sql = """
            INSERT INTO groups (user_id, name, created_at, updated_at)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setLong(1, userId)
            statement.setString(2, group.name.trim())
            statement.setString(3, createdAt)
            statement.setString(4, updatedAt)
            statement.executeUpdate()

            statement.generatedKeys.use { keys ->
                if (keys.next()) {
                    return keys.getLong(1)
                }
            }
        }

        error("Group id was not generated")
    }

    private fun updateGroup(connection: Connection, userId: Long, group: SyncGroupItem) {
        val sql = """
            UPDATE groups
            SET name = ?, updated_at = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, group.name.trim())
            statement.setString(2, group.updatedAt ?: Instant.now().toString())
            statement.setLong(3, group.serverId ?: return)
            statement.setLong(4, userId)
            statement.executeUpdate()
        }
    }

    private fun deleteGroup(connection: Connection, userId: Long, groupId: Long) {
        connection.prepareStatement("DELETE FROM groups WHERE id = ? AND user_id = ?").use { statement ->
            statement.setLong(1, groupId)
            statement.setLong(2, userId)
            statement.executeUpdate()
        }
    }

    private fun insertPlace(connection: Connection, userId: Long, place: SyncPlaceItem, groupId: Long?): Long {
        val now = Instant.now().toString()
        val createdAt = place.createdAt ?: now
        val updatedAt = place.updatedAt ?: now
        val sql = """
            INSERT INTO place_cards (
                user_id, title, address, photo_path, rating, comment, status, group_id, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setLong(1, userId)
            statement.setString(2, place.title.trim())
            statement.setString(3, place.address.trim())
            statement.setString(4, place.photoPath)
            statement.setInt(5, place.rating)
            statement.setString(6, place.comment.trim())
            statement.setString(7, place.status.name)
            if (groupId == null) {
                statement.setObject(8, null)
            } else {
                statement.setLong(8, groupId)
            }
            statement.setString(9, createdAt)
            statement.setString(10, updatedAt)
            statement.executeUpdate()

            statement.generatedKeys.use { keys ->
                if (keys.next()) {
                    return keys.getLong(1)
                }
            }
        }

        error("Place id was not generated")
    }

    private fun updatePlace(connection: Connection, userId: Long, place: SyncPlaceItem, groupId: Long?) {
        val sql = """
            UPDATE place_cards
            SET title = ?, address = ?, photo_path = ?, rating = ?, comment = ?, status = ?, group_id = ?, updated_at = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, place.title.trim())
            statement.setString(2, place.address.trim())
            statement.setString(3, place.photoPath)
            statement.setInt(4, place.rating)
            statement.setString(5, place.comment.trim())
            statement.setString(6, place.status.name)
            if (groupId == null) {
                statement.setObject(7, null)
            } else {
                statement.setLong(7, groupId)
            }
            statement.setString(8, place.updatedAt ?: Instant.now().toString())
            statement.setLong(9, place.serverId ?: return)
            statement.setLong(10, userId)
            statement.executeUpdate()
        }
    }

    private fun deletePlace(connection: Connection, userId: Long, placeId: Long) {
        connection.prepareStatement("DELETE FROM place_cards WHERE id = ? AND user_id = ?").use { statement ->
            statement.setLong(1, placeId)
            statement.setLong(2, userId)
            statement.executeUpdate()
        }
    }

    private fun groupExists(connection: Connection, userId: Long, groupId: Long): Boolean {
        connection.prepareStatement("SELECT id FROM groups WHERE id = ? AND user_id = ? LIMIT 1").use { statement ->
            statement.setLong(1, groupId)
            statement.setLong(2, userId)
            statement.executeQuery().use { result ->
                return result.next()
            }
        }
    }

    private fun placeExists(connection: Connection, userId: Long, placeId: Long): Boolean {
        connection.prepareStatement("SELECT id FROM place_cards WHERE id = ? AND user_id = ? LIMIT 1").use { statement ->
            statement.setLong(1, placeId)
            statement.setLong(2, userId)
            statement.executeQuery().use { result ->
                return result.next()
            }
        }
    }

    private fun loadGroups(connection: Connection, userId: Long): List<ServerGroupDto> {
        val sql = """
            SELECT id, name, created_at, updated_at
            FROM groups
            WHERE user_id = ?
            ORDER BY updated_at DESC, id DESC
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.executeQuery().use { result ->
                val groups = mutableListOf<ServerGroupDto>()
                while (result.next()) {
                    groups += ServerGroupDto(
                        serverId = result.getLong("id"),
                        name = result.getString("name"),
                        createdAt = result.getString("created_at"),
                        updatedAt = result.getString("updated_at")
                    )
                }
                return groups
            }
        }
    }

    private fun loadPlaces(connection: Connection, userId: Long): List<ServerPlaceDto> {
        val sql = """
            SELECT id, title, address, photo_path, rating, comment, status, group_id, created_at, updated_at
            FROM place_cards
            WHERE user_id = ?
            ORDER BY updated_at DESC, id DESC
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, userId)
            statement.executeQuery().use { result ->
                val places = mutableListOf<ServerPlaceDto>()
                while (result.next()) {
                    places += result.toServerPlaceDto()
                }
                return places
            }
        }
    }

    private fun ResultSet.toServerPlaceDto(): ServerPlaceDto {
        val groupId = getLong("group_id")
        val hasGroupId = !wasNull()
        return ServerPlaceDto(
            serverId = getLong("id"),
            title = getString("title"),
            address = getString("address"),
            photoPath = getString("photo_path"),
            rating = getInt("rating"),
            comment = getString("comment"),
            status = com.spotbook.backend.places.PlaceStatus.valueOf(getString("status")),
            groupServerId = if (hasGroupId) groupId else null,
            createdAt = getString("created_at"),
            updatedAt = getString("updated_at")
        )
    }
}

class SyncException(
    override val message: String
) : RuntimeException(message)

