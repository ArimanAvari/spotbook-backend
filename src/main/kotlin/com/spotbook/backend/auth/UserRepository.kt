package com.spotbook.backend.auth

import com.spotbook.backend.database.DatabaseFactory
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

class UserRepository(
    private val databaseFactory: DatabaseFactory
) {
    fun create(email: String, passwordHash: String): StoredUser {
        val sql = """
            INSERT INTO users (email, password_hash, created_at)
            VALUES (?, ?, ?)
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setString(1, email)
                statement.setString(2, passwordHash)
                statement.setString(3, Instant.now().toString())
                statement.executeUpdate()

                statement.generatedKeys.use { keys ->
                    if (keys.next()) {
                        return StoredUser(
                            id = keys.getLong(1),
                            email = email,
                            passwordHash = passwordHash
                        )
                    }
                }
            }
        }

        error("User id was not generated")
    }

    fun findByEmail(email: String): StoredUser? {
        val sql = """
            SELECT id, email, password_hash
            FROM users
            WHERE email = ?
            LIMIT 1
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { result ->
                    return result.toStoredUserOrNull()
                }
            }
        }
    }

    fun findById(id: Long): StoredUser? {
        val sql = """
            SELECT id, email, password_hash
            FROM users
            WHERE id = ?
            LIMIT 1
        """.trimIndent()

        databaseFactory.connection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { result ->
                    return result.toStoredUserOrNull()
                }
            }
        }
    }

    private fun ResultSet.toStoredUserOrNull(): StoredUser? {
        if (!next()) {
            return null
        }

        return StoredUser(
            id = getLong("id"),
            email = getString("email"),
            passwordHash = getString("password_hash")
        )
    }
}

