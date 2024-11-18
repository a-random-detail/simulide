package com.simulide

import com.simulide.plugins.documentRoutes
import com.simulide.plugins.domain.Document
import com.simulide.plugins.domain.DocumentService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import javax.sql.DataSource
import kotlin.math.exp
import kotlin.test.assertEquals

class DocumentTest {
    private lateinit var inMemoryDb: DataSource

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

        createDocument(
            dataSource = inMemoryDb,
            document = expectedDocument
        )

        application {
            routing {
                documentRoutes(DocumentService(dataSource = inMemoryDb))
            }
        }

        client.get("/documents/$documentId").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.decodeFromString<Document>(bodyAsText())

            assertEquals(expectedDocument.id, response.id)
            assertEquals(expectedDocument.name, response.name)
            assertEquals(expectedDocument.content, response.content)
            assertEquals(expectedDocument.version, response.version)
        }
    }
}