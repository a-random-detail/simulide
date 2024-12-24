package com.simulide.plugins

import com.simulide.plugins.domain.DocumentService
import com.simulide.plugins.domain.Operation
import com.simulide.plugins.domain.jsonConfiguration
import com.simulide.plugins.domain.uuidSerializerModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

val activeConnections = ConcurrentHashMap<UUID, MutableSet<DefaultWebSocketServerSession>>()

fun Application.configureCollaboration(documentService: DocumentService) {
    install(ContentNegotiation) {
        json(jsonConfiguration)
    }
    install(plugin = WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/collaborate/{documentId}") {
            val documentId = call.parameters["documentId"] ?.let { UUID.fromString(it) } ?:
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid document ID"))
            println(">>>> WebSocket connection established for documentID: ${documentId}")

            val connections = activeConnections.computeIfAbsent(documentId) { mutableSetOf() }
            connections.add(this)

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val operation = jsonConfiguration.decodeFromString<CreateOperationRequest>( frame.readText())
                        val transformed = documentService.applyOperation(documentId, operation)
                        connections.forEach { connection ->
                            if (connection != this)
                                connection.send(content = jsonConfiguration.encodeToString(Operation.serializer(), transformed))
                        }
                    }
                }
            } finally {
                connections.remove(this)
            }
        }
    }
}
