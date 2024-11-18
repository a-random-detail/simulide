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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.sql.DataSource

@Serializable
data class CreateDocumentRequest(val name: String, val content: String)

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
            val documentId = call.parameters["id"] ?: call.respond(HttpStatusCode.BadRequest, "Invalid document ID")
            val docUUID = UUID.fromString(documentId as String)

            val document = documentService.getById(docUUID) ?: call.respond(HttpStatusCode.NotFound, "Unable to find document")

            call.respond(document)
        }

        post("/documents/{id}/operations") {

            val documentId = call.parameters["id"] ?: call.respond(HttpStatusCode.BadRequest, "Invalid document ID")
            val docUUID = UUID.fromString(documentId as String)

            val operation = call.receive<Operation>()

            try {
                val operation = documentService.applyOperation(docUUID, operation)
                call.respond(operation)
            } catch (e: IllegalArgumentException) {
                //TODO: make a validation function
                call.respond(HttpStatusCode.BadRequest, "Invalid Request: ${e.localizedMessage}")
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, "Conflict ${e.localizedMessage}")
            }
        }

        post("/documents") {
            try {
                val document = call.receive<CreateDocumentRequest>()
                val createdDocument = documentService.createDocument(document)
                call.respond(createdDocument)
            } catch (e: IllegalArgumentException) {
                //TODO: make a validation function
                call.respond(HttpStatusCode.BadRequest, "Invalid Request: ${e.localizedMessage}")
            }
        }
    }
}

fun Application.connectToPostgres(): Connection {
    Class.forName("org.postgresql.Driver")
    val host = environment.config.property("postgres.host").getString()
    val port = environment.config.property("postgres.port").getString()
    val dbName = environment.config.property("postgres.db_name").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()
    val url = "jdbc:postgresql://$host:$port/$dbName"
    return DriverManager.getConnection(url, user, password)
}
