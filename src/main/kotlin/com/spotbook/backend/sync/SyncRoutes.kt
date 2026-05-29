package com.spotbook.backend.sync

import com.spotbook.backend.auth.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.syncRoutes(syncService: SyncService) {
    authenticate("auth-jwt") {
        route("/api/sync") {
            post("/export") {
                call.runSyncAction {
                    call.respond(
                        HttpStatusCode.OK,
                        syncService.exportData(call.currentUserId(), call.receive())
                    )
                }
            }

            get("/import") {
                call.runSyncAction {
                    call.respond(HttpStatusCode.OK, syncService.importData(call.currentUserId()))
                }
            }
        }
    }
}

private fun ApplicationCall.currentUserId(): Long {
    return principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asLong()
        ?: throw SyncException("Invalid token")
}

private suspend fun ApplicationCall.runSyncAction(block: suspend () -> Unit) {
    try {
        block()
    } catch (error: SyncException) {
        respond(HttpStatusCode.BadRequest, SyncErrorResponse(error.message))
    } catch (error: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("Request is invalid"))
    }
}

