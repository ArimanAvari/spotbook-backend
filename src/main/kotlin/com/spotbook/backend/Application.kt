package com.spotbook.backend

import com.spotbook.backend.plugins.configureDatabase
import com.spotbook.backend.plugins.configureRouting
import com.spotbook.backend.plugins.configureSecurity
import com.spotbook.backend.plugins.configureSerialization
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
    configureSerialization()
    configureSecurity()
    configureDatabase()
    configureRouting()
}

