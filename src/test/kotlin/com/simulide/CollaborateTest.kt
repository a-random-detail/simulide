package com.simulide

import com.simulide.domain.OperationType
import com.simulide.plugins.CreateOperationRequest
import com.simulide.plugins.configureCollaboration
import com.simulide.plugins.domain.Document
import com.simulide.plugins.domain.DocumentService
import com.simulide.plugins.domain.Operation
import com.simulide.plugins.domain.jsonConfiguration
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CollaborateTest {
    private lateinit var inMemoryDb: DataSource
    private lateinit var documentService: DocumentService

    val document = Document(
        name = "Test doc here",
        content = "",
        id = UUID.randomUUID(),
        version = 1
    )


    @Before
    fun setup() {
        inMemoryDb = setupTestDatabase()
        documentService = DocumentService(inMemoryDb)
    }

    @After
    fun teardown() {
        cleanupDatabase(dataSource = inMemoryDb)
    }

    @Test
    fun `test WebSocket collaboration`() = testApplication {
        application {
            configureCollaboration(documentService)
        }

        createDocument(inMemoryDb, document)


        val documentId = document.id
        val endpoint = "/ws/collaborate/$documentId"
        val operation = CreateOperationRequest(
            type = OperationType.insert,
            position = 0,
            content = "Hello",
            documentId = documentId.toString(),
            length = 5
        )
        var client2ReceivedMessage: String? = null

        runBlocking {
            val client1 = createClient { install(WebSockets) }
            val client2 = createClient { install(WebSockets) }
            val client1Job = launch {
               client1.webSocket(endpoint) {
                   send(Frame.Text(jsonConfiguration.encodeToString(operation)))
               }
            }

            val client2Job = launch {
                client2.webSocket(endpoint) {
                    val frame = incoming.receive() as? Frame.Text
                    client2ReceivedMessage = frame?.readText()
                }
            }

            client1Job.join()
            client2Job.join()
        }

        assertNotNull(client2ReceivedMessage)
        val message = jsonConfiguration.decodeFromString<Operation>(client2ReceivedMessage!!)
        assertEquals(OperationType.insert, message.type)
        assertEquals("Hello", message.content)
        assertEquals(documentId, message.documentId)
        assertEquals(operation.position, message.position)
        assertEquals(operation.length, message.length)
    }

}
