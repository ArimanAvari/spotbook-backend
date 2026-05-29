package com.spotbook.backend.places

import com.spotbook.backend.database.DatabaseFactory
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

class PlaceRepository(
    private val databaseFactory: DatabaseFactory
) {
    fun findAll(userId: Long): List<PlaceRecord> {
        val sql = """
            SELECT id, user_id, title, address, photo_path, rating, comment, status, group_id, created_at, updated_at
            FROM place_cards
            WHERE user_id = ?
            ORDER BY updated_at DESC, id DESC
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.executeQuery().use { result ->
                    val places = mutableListOf<PlaceRecord>()
                    while (result.next()) {
                        places += result.toPlaceRecord()
                    }
                    return places
                }
            }
        }
    }

    fun findById(userId: Long, placeId: Long): PlaceRecord? {
        val sql = """
            SELECT id, user_id, title, address, photo_path, rating, comment, status, group_id, created_at, updated_at
            FROM place_cards
            WHERE id = ? AND user_id = ?
            LIMIT 1
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, placeId)
                statement.setLong(2, userId)
                statement.executeQuery().use { result ->
                    return if (result.next()) result.toPlaceRecord() else null
                }
            }
        }
    }

    fun create(userId: Long, request: PlaceRequest): PlaceRecord {
        val now = Instant.now().toString()
        val sql = """
            INSERT INTO place_cards (
                user_id, title, address, photo_path, rating, comment, status, group_id, created_at, updated_at
            )
            VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, request.title.trim())
                statement.setString(3, request.address.trim())
                statement.setInt(4, request.rating)
                statement.setString(5, request.comment.trim())
                statement.setString(6, request.status.name)
                if (request.groupId == null) {
                    statement.setObject(7, null)
                } else {
                    statement.setLong(7, request.groupId)
                }
                statement.setString(8, now)
                statement.setString(9, now)
                statement.executeUpdate()

                statement.generatedKeys.use { keys ->
                    if (keys.next()) {
                        return findById(userId, keys.getLong(1)) ?: error("Place was not saved")
                    }
                }
            }
        }

        error("Place id was not generated")
    }

    fun update(userId: Long, placeId: Long, request: PlaceRequest): PlaceRecord? {
        val sql = """
            UPDATE place_cards
            SET title = ?, address = ?, rating = ?, comment = ?, status = ?, group_id = ?, updated_at = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, request.title.trim())
                statement.setString(2, request.address.trim())
                statement.setInt(3, request.rating)
                statement.setString(4, request.comment.trim())
                statement.setString(5, request.status.name)
                if (request.groupId == null) {
                    statement.setObject(6, null)
                } else {
                    statement.setLong(6, request.groupId)
                }
                statement.setString(7, Instant.now().toString())
                statement.setLong(8, placeId)
                statement.setLong(9, userId)

                if (statement.executeUpdate() == 0) {
                    return null
                }
            }
        }

        return findById(userId, placeId)
    }

    fun delete(userId: Long, placeId: Long): Boolean {
        val sql = "DELETE FROM place_cards WHERE id = ? AND user_id = ?"

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, placeId)
                statement.setLong(2, userId)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun updateStatus(userId: Long, placeId: Long, status: PlaceStatus): PlaceRecord? {
        val sql = """
            UPDATE place_cards
            SET status = ?, updated_at = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, status.name)
                statement.setString(2, Instant.now().toString())
                statement.setLong(3, placeId)
                statement.setLong(4, userId)

                if (statement.executeUpdate() == 0) {
                    return null
                }
            }
        }

        return findById(userId, placeId)
    }

    fun updatePhoto(userId: Long, placeId: Long, photoPath: String): PlaceRecord? {
        val sql = """
            UPDATE place_cards
            SET photo_path = ?, updated_at = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, photoPath)
                statement.setString(2, Instant.now().toString())
                statement.setLong(3, placeId)
                statement.setLong(4, userId)

                if (statement.executeUpdate() == 0) {
                    return null
                }
            }
        }

        return findById(userId, placeId)
    }

    fun groupBelongsToUser(userId: Long, groupId: Long): Boolean {
        val sql = "SELECT id FROM groups WHERE id = ? AND user_id = ? LIMIT 1"

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, groupId)
                statement.setLong(2, userId)
                statement.executeQuery().use { result ->
                    return result.next()
                }
            }
        }
    }

    private fun ResultSet.toPlaceRecord(): PlaceRecord {
        val groupId = getLong("group_id")
        val hasGroupId = !wasNull()
        return PlaceRecord(
            id = getLong("id"),
            userId = getLong("user_id"),
            title = getString("title"),
            address = getString("address"),
            photoPath = getString("photo_path"),
            rating = getInt("rating"),
            comment = getString("comment"),
            status = PlaceStatus.valueOf(getString("status")),
            groupId = if (hasGroupId) groupId else null,
            createdAt = getString("created_at"),
            updatedAt = getString("updated_at")
        )
    }
}
