package com.spotbook.backend.groups

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
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.groupRoutes(groupService: GroupService) {
    authenticate("auth-jwt") {
        route("/api/groups") {
            get {
                call.runGroupAction {
                    call.respond(HttpStatusCode.OK, groupService.getGroups(call.currentUserId()))
                }
            }

            post {
                call.runGroupAction {
                    call.respond(
                        HttpStatusCode.Created,
                        groupService.createGroup(call.currentUserId(), call.receive())
                    )
                }
            }

            delete("/{id}") {
                call.runGroupAction {
                    groupService.deleteGroup(call.currentUserId(), call.groupId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            get("/{id}/places") {
                call.runGroupAction {
                    call.respond(
                        HttpStatusCode.OK,
                        groupService.getPlacesInGroup(call.currentUserId(), call.groupId())
                    )
                }
            }

            post("/{groupId}/places/{placeId}") {
                call.runGroupAction {
                    call.respond(
                        HttpStatusCode.OK,
                        groupService.addPlaceToGroup(
                            userId = call.currentUserId(),
                            groupId = call.groupIdFromRoute(),
                            placeId = call.placeIdFromRoute()
                        )
                    )
                }
            }

            delete("/{groupId}/places/{placeId}") {
                call.runGroupAction {
                    call.respond(
                        HttpStatusCode.OK,
                        groupService.removePlaceFromGroup(
                            userId = call.currentUserId(),
                            groupId = call.groupIdFromRoute(),
                            placeId = call.placeIdFromRoute()
                        )
                    )
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
        ?: throw GroupException(HttpStatusCode.Unauthorized, "Invalid token")
}

private fun ApplicationCall.groupId(): Long {
    return parameters["id"]?.toLongOrNull()
        ?: throw GroupException(HttpStatusCode.BadRequest, "Group id is invalid")
}

private fun ApplicationCall.groupIdFromRoute(): Long {
    return parameters["groupId"]?.toLongOrNull()
        ?: throw GroupException(HttpStatusCode.BadRequest, "Group id is invalid")
}

private fun ApplicationCall.placeIdFromRoute(): Long {
    return parameters["placeId"]?.toLongOrNull()
        ?: throw GroupException(HttpStatusCode.BadRequest, "Place id is invalid")
}

private suspend fun ApplicationCall.runGroupAction(block: suspend () -> Unit) {
    try {
        block()
    } catch (error: GroupException) {
        respond(error.status, GroupErrorResponse(error.message))
    } catch (error: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("Request is invalid"))
    }
}

