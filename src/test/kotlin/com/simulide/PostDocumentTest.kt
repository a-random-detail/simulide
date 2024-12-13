package com.simulide

import com.simulide.domain.UuidSerializerModule
import com.simulide.plugins.CreateDocumentRequest
import com.simulide.plugins.documentRoutes
import com.simulide.plugins.domain.Document
import com.simulide.plugins.domain.DocumentService
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
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostDocumentTest {
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

    @Before
    fun setup() {
        inMemoryDb = setupTestDatabase()
    }

    @After
    fun teardown() {
        cleanupDatabase(dataSource = inMemoryDb)
    }

    @Test
    fun `POST document returns document with created status`() = testApplication {
       val input = CreateDocumentRequest(
           name = "Test doc here",
           content = "hello \n world!",
       )


        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        client.post("/documents") {
            setBody(json.encodeToString(input))
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            val response = json.decodeFromString<Document>(bodyAsText())

            assertNotNull(response.id)
            assertEquals(input.name, response.name)
            assertEquals(input.content, response.content)
            assertEquals(1, response.version)
        }
    }

    @Test
    fun `POST document returns badrequest when empty object sent`() = testApplication {
        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        client.post("/documents") {
            setBody("{}")
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `POST document returns badrequest when non-json sent`() = testApplication {
        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        client.post("/documents") {
            setBody("this is not json")
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

}