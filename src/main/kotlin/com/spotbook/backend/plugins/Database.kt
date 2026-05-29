package com.spotbook.backend.plugins

import com.spotbook.backend.database.DatabaseFactory
import io.ktor.server.application.Application

fun Application.configureDatabase(databaseFactory: DatabaseFactory) {
    databaseFactory.init()
}
