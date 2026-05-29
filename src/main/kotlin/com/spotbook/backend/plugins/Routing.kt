package com.spotbook.backend.plugins

import com.spotbook.backend.auth.AuthService
import com.spotbook.backend.auth.authRoutes
import com.spotbook.backend.groups.GroupService
import com.spotbook.backend.groups.groupRoutes
import com.spotbook.backend.places.PlaceService
import com.spotbook.backend.places.placeRoutes
import com.spotbook.backend.sync.SyncService
import com.spotbook.backend.sync.syncRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

fun Application.configureRouting(
    authService: AuthService,
    placeService: PlaceService,
    groupService: GroupService,
    syncService: SyncService
) {
    routing {
        get("/") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    message = "SpotBook backend is running"
                )
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse(status = "ok", message = "OK"))
        }

        authRoutes(authService)
        placeRoutes(placeService)
        groupRoutes(groupService)
        syncRoutes(syncService)
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val message: String
)
