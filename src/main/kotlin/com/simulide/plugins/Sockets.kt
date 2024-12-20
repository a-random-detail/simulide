package com.simulide.plugins

import com.simulide.domain.UuidSerializerModule
import com.simulide.plugins.domain.DocumentService
import com.simulide.plugins.domain.Operation
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

val activeConnections = ConcurrentHashMap<UUID, MutableSet<DefaultWebSocketServerSession>>()

fun Application.configureSockets(documentService: DocumentService) {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = customSerializerModule
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws/collaborate/{documentId}") {
            val documentId = call.parameters["documentId"] ?.let { UUID.fromString(it) } ?:
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid document ID"))

            val connections = activeConnections.computeIfAbsent(documentId) { mutableSetOf() }
            connections.add(this)

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val operation = Json.decodeFromString(Operation.serializer(), frame.readText())
//                    val transformed = documentService.trans
                }
            }
        }
    }
}
