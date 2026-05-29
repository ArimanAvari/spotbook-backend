package com.spotbook.backend.database

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

class DatabaseFactory(
    private val databasePath: Path = Path.of("data", "spotbook.db")
) {
    private val jdbcUrl: String
        get() = "jdbc:sqlite:${databasePath.toAbsolutePath()}"

    fun init() {
        Files.createDirectories(databasePath.toAbsolutePath().parent)

        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("PRAGMA foreign_keys = ON;")
                statement.executeUpdate(Tables.CREATE_USERS)
                statement.executeUpdate(Tables.CREATE_GROUPS)
                statement.executeUpdate(Tables.CREATE_PLACE_CARDS)
            }
        }
    }

    fun connection(): Connection {
        val connection = DriverManager.getConnection(jdbcUrl)
        connection.createStatement().use { statement ->
            statement.executeUpdate("PRAGMA foreign_keys = ON;")
        }
        return connection
    }
}

