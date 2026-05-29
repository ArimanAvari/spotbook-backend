package com.spotbook.backend

import com.spotbook.backend.auth.AuthService
import com.spotbook.backend.auth.JwtService
import com.spotbook.backend.auth.UserRepository
import com.spotbook.backend.database.DatabaseFactory
import com.spotbook.backend.groups.GroupRepository
import com.spotbook.backend.groups.GroupService
import com.spotbook.backend.places.PlaceRepository
import com.spotbook.backend.places.PlaceService
import com.spotbook.backend.plugins.configureDatabase
import com.spotbook.backend.plugins.configureRouting
import com.spotbook.backend.plugins.configureSecurity
import com.spotbook.backend.plugins.configureSerialization
import com.spotbook.backend.sync.SyncService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val databaseFactory = DatabaseFactory()
    val jwtService = JwtService()
    val userRepository = UserRepository(databaseFactory)
    val authService = AuthService(jwtService, userRepository)
    val placeRepository = PlaceRepository(databaseFactory)
    val placeService = PlaceService(placeRepository)
    val groupRepository = GroupRepository(databaseFactory)
    val groupService = GroupService(groupRepository)
    val syncService = SyncService(databaseFactory)

    configureSerialization()
    configureSecurity(jwtService)
    configureDatabase(databaseFactory)
    configureRouting(authService, placeService, groupService, syncService)
}
