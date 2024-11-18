package com.simulide

import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import com.simulide.domain.UuidSerializerModule
import com.simulide.plugins.customSerializerModule
import com.simulide.plugins.documentRoutes
import com.simulide.plugins.domain.Document
import com.simulide.plugins.domain.DocumentService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

class DocumentTest {
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
    fun `GET documents by id returns document`() = testApplication {
       val documentId = UUID.randomUUID()
       val expectedDocument = Document(
           id = documentId,
           name = "Test doc here",
           content = "hello \n world!",
           version = 54)


        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }
        createDocument(
            dataSource = inMemoryDb,
            document = expectedDocument
        )

        client.get("/documents/$documentId").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = json.decodeFromString<Document>(bodyAsText())

            assertEquals(expectedDocument.id, response.id)
            assertEquals(expectedDocument.name, response.name)
            assertEquals(expectedDocument.content, response.content)
            assertEquals(expectedDocument.version, response.version)
        }
    }
}