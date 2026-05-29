package com.spotbook.backend.groups

import com.spotbook.backend.database.DatabaseFactory
import com.spotbook.backend.places.PlaceRecord
import com.spotbook.backend.places.PlaceStatus
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

class GroupRepository(
    private val databaseFactory: DatabaseFactory
) {
    fun findAll(userId: Long): List<GroupRecord> {
        val sql = """
            SELECT id, user_id, name, created_at, updated_at
            FROM groups
            WHERE user_id = ?
            ORDER BY updated_at DESC, id DESC
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.executeQuery().use { result ->
                    val groups = mutableListOf<GroupRecord>()
                    while (result.next()) {
                        groups += result.toGroupRecord()
                    }
                    return groups
                }
            }
        }
    }

    fun findById(userId: Long, groupId: Long): GroupRecord? {
        val sql = """
            SELECT id, user_id, name, created_at, updated_at
            FROM groups
            WHERE id = ? AND user_id = ?
            LIMIT 1
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, groupId)
                statement.setLong(2, userId)
                statement.executeQuery().use { result ->
                    return if (result.next()) result.toGroupRecord() else null
                }
            }
        }
    }

    fun create(userId: Long, name: String): GroupRecord {
        val now = Instant.now().toString()
        val sql = """
            INSERT INTO groups (user_id, name, created_at, updated_at)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, name.trim())
                statement.setString(3, now)
                statement.setString(4, now)
                statement.executeUpdate()

                statement.generatedKeys.use { keys ->
                    if (keys.next()) {
                        return findById(userId, keys.getLong(1)) ?: error("Group was not saved")
                    }
                }
            }
        }

        error("Group id was not generated")
    }

    fun delete(userId: Long, groupId: Long): Boolean {
        val sql = "DELETE FROM groups WHERE id = ? AND user_id = ?"

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, groupId)
                statement.setLong(2, userId)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun addPlaceToGroup(userId: Long, groupId: Long, placeId: Long): Boolean {
        val sql = """
            UPDATE place_cards
            SET group_id = ?, updated_at = ?
            WHERE id = ? AND user_id = ?
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, groupId)
                statement.setString(2, Instant.now().toString())
                statement.setLong(3, placeId)
                statement.setLong(4, userId)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun removePlaceFromGroup(userId: Long, groupId: Long, placeId: Long): Boolean {
        val sql = """
            UPDATE place_cards
            SET group_id = NULL, updated_at = ?
            WHERE id = ? AND user_id = ? AND group_id = ?
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, Instant.now().toString())
                statement.setLong(2, placeId)
                statement.setLong(3, userId)
                statement.setLong(4, groupId)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun placeBelongsToUser(userId: Long, placeId: Long): Boolean {
        val sql = "SELECT id FROM place_cards WHERE id = ? AND user_id = ? LIMIT 1"

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, placeId)
                statement.setLong(2, userId)
                statement.executeQuery().use { result ->
                    return result.next()
                }
            }
        }
    }

    fun findPlacesByGroup(userId: Long, groupId: Long): List<PlaceRecord> {
        val sql = """
            SELECT id, user_id, title, address, photo_path, rating, comment, status, group_id, created_at, updated_at
            FROM place_cards
            WHERE user_id = ? AND group_id = ?
            ORDER BY updated_at DESC, id DESC
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.setLong(2, groupId)
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

    private fun ResultSet.toGroupRecord(): GroupRecord {
        return GroupRecord(
            id = getLong("id"),
            userId = getLong("user_id"),
            name = getString("name"),
            createdAt = getString("created_at"),
            updatedAt = getString("updated_at")
        )
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
