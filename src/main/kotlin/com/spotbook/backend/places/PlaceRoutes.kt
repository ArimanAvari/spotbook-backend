package com.spotbook.backend.places

import com.spotbook.backend.auth.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.placeRoutes(placeService: PlaceService) {
    authenticate("auth-jwt") {
        route("/api/places") {
            get {
                call.runPlaceAction {
                    call.respond(HttpStatusCode.OK, placeService.getPlaces(call.currentUserId()))
                }
            }

            post {
                call.runPlaceAction {
                    call.respond(
                        HttpStatusCode.Created,
                        placeService.createPlace(call.currentUserId(), call.receive())
                    )
                }
            }

            get("/{id}") {
                call.runPlaceAction {
                    call.respond(HttpStatusCode.OK, placeService.getPlace(call.currentUserId(), call.placeId()))
                }
            }

            put("/{id}") {
                call.runPlaceAction {
                    call.respond(
                        HttpStatusCode.OK,
                        placeService.updatePlace(call.currentUserId(), call.placeId(), call.receive())
                    )
                }
            }

            delete("/{id}") {
                call.runPlaceAction {
                    placeService.deletePlace(call.currentUserId(), call.placeId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            patch("/{id}/status") {
                call.runPlaceAction {
                    val request = call.receive<UpdatePlaceStatusRequest>()
                    call.respond(
                        HttpStatusCode.OK,
                        placeService.updateStatus(call.currentUserId(), call.placeId(), request.status)
                    )
                }
            }

            post("/{id}/photo") {
                call.runPlaceAction {
                    val userId = call.currentUserId()
                    val placeId = call.placeId()
                    val multipart = call.receiveMultipart()
                    var response: PhotoUploadResponse? = null

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem && response == null) {
                            response = placeService.savePhoto(
                                userId = userId,
                                placeId = placeId,
                                originalFileName = part.originalFileName,
                                input = part.provider()
                            )
                        }
                        part.dispose()
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        response ?: throw PlaceException(HttpStatusCode.BadRequest, "Photo file is required")
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
        ?: throw PlaceException(HttpStatusCode.Unauthorized, "Invalid token")
}

private fun ApplicationCall.placeId(): Long {
    return parameters["id"]?.toLongOrNull()
        ?: throw PlaceException(HttpStatusCode.BadRequest, "Place id is invalid")
}

private suspend fun ApplicationCall.runPlaceAction(block: suspend () -> Unit) {
    try {
        block()
    } catch (error: PlaceException) {
        respond(error.status, PlaceErrorResponse(error.message))
    } catch (error: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("Request is invalid"))
    }
}
