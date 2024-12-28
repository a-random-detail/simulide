package com.simulide

import com.simulide.domain.OperationType
import com.simulide.domain.UuidSerializerModule
import com.simulide.plugins.CreateDocumentRequest
import com.simulide.plugins.CreateOperationRequest
import com.simulide.plugins.documentRoutes
import com.simulide.plugins.domain.Document
import com.simulide.plugins.domain.DocumentService
import com.simulide.plugins.domain.Operation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class PostOperationTest(private val type: OperationType) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<OperationType> {
            return listOf(OperationType.insert, OperationType.delete)
        }
    }
    private lateinit var inMemoryDb: DataSource

    val customSerializerModule = SerializersModule {
        contextual(UUID::class, UuidSerializerModule)
    }

    val json = Json {
        serializersModule = customSerializerModule
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true

    }

    val documentRequest = CreateDocumentRequest(
        name = "Test doc here",
        content = "hello \n world!",
    )

    @Before
    fun setup() {
        inMemoryDb = setupTestDatabase()
    }

    @After
    fun teardown() {
        cleanupDatabase(dataSource = inMemoryDb)
    }

    @Test
    fun `POST operation returns operation with created status`() = testApplication {
        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        val documentResponse = client.post("/documents") {
            setBody(json.encodeToString(documentRequest))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, documentResponse.status)
        val decodedDocumentResponse = json.decodeFromString<Document>(documentResponse.bodyAsText())

        val operationContent = "foo-bar-baz"
        val operationInput = CreateOperationRequest(
            documentId = decodedDocumentResponse.id.toString(),
            type = type,
            content = if (OperationType.insert.equals(type)) operationContent else null,
            position = 0,
            length = if (OperationType.delete.equals(type)) operationContent.length else null
        )

        val operationResponse = client.post("documents/${decodedDocumentResponse.id}/operations") {
            setBody(json.encodeToString(operationInput))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Created, operationResponse.status)
        val decodedOperationResponse = json.decodeFromString<Operation>(operationResponse.bodyAsText())

        assertNotNull(decodedOperationResponse.id)
        assertEquals(decodedDocumentResponse.id, decodedOperationResponse.documentId)
        assertEquals(operationInput.type, decodedOperationResponse.type)
        assertEquals(operationInput.content, decodedOperationResponse.content)
        assertEquals(operationInput.position, decodedOperationResponse.position)
    }

    @Test
    fun `POST operation returns badrequest when empty object sent`() = testApplication {
        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        val documentResponse = client.post("/documents") {
            setBody(json.encodeToString(documentRequest))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, documentResponse.status)
        val decodedDocumentResponse = json.decodeFromString<Document>(documentResponse.bodyAsText())

        val operationResponse = client.post("documents/${decodedDocumentResponse.id}/operations") {
            setBody("{}")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.BadRequest, operationResponse.status)
    }

    @Test
    fun `POST document returns badrequest when non-json sent`() = testApplication {
        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        val documentResponse = client.post("/documents") {
            setBody(json.encodeToString(documentRequest))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, documentResponse.status)
        val decodedDocumentResponse = json.decodeFromString<Document>(documentResponse.bodyAsText())

        val operationResponse = client.post("documents/${decodedDocumentResponse.id}/operations") {
            setBody("this is not json")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.BadRequest, operationResponse.status)
    }
}