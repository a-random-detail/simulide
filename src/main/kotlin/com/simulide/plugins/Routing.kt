package com.simulide.plugins

import com.simulide.domain.UuidSerializerModule
import com.simulide.plugins.domain.DocumentService
import com.simulide.plugins.domain.Operation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.flywaydb.core.api.output.OperationResult
import java.util.*

@Serializable
data class CreateDocumentRequest(val name: String, val content: String)
@Serializable
data class CreateOperationRequest(val documentId: String, val type: String, val content: String, val position: Int)

val customSerializerModule = SerializersModule {
    contextual(UUID::class, UuidSerializerModule)
}
fun Application.documentRoutes(documentService: DocumentService) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
    install(ContentNegotiation) {
        json(Json {
            serializersModule = customSerializerModule
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    routing {
        get("/documents/{id}") {
            val documentId = call.parameters["id"] ?: call.respond(HttpStatusCode.NotFound)
            try {
                val docUUID = UUID.fromString(documentId as String)
                val document =
                    documentService.getById(docUUID) ?: call.respond(HttpStatusCode.NotFound, "Invalid/missing document ID")
                call.respond(document)
            } catch( e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, "Invalid/missing document ID")
            }
        }

        post("/documents/{id}/operations") {
            val documentId = call.parameters["id"] ?: call.respond(HttpStatusCode.NotFound)
            val docUUID = UUID.fromString(documentId as String)
            try {
                val operationRequest = call.receive<CreateOperationRequest>()
                val operation = documentService.applyOperation(docUUID, operationRequest)
                call.respond(HttpStatusCode.Created, operation)
            } catch (e: IllegalArgumentException) {
                //TODO: make a validation function
                call.respond(HttpStatusCode.BadRequest, "Invalid Request: ${e.localizedMessage}")
            } catch (e: Throwable) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body: ${e.localizedMessage}")
            }
        }

        post("/documents") {
            try {
                val document = call.receive<CreateDocumentRequest>()
                val createdDocument = documentService.createDocument(document)
                call.respond(HttpStatusCode.Created, createdDocument)
            } catch (e: IllegalArgumentException) {
                //TODO: make a validation function
                call.respond(HttpStatusCode.BadRequest, "Invalid Request: ${e.localizedMessage}")
            } catch (e: Throwable) {
                log.info("Threw error: ${e.message}")
                var foo = "bar"
                call.respond(HttpStatusCode.Conflict, "Conflict: ${e.localizedMessage}")
            }

        }
    }
}
